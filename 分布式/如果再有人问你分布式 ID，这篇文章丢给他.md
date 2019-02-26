# 1.背景
在我们的业务需求中通常有需要一些唯一的ID，来记录我们某个数据的标识:
- 某个用户的ID
- 某个订单的单号
- 某个信息的ID

通常我们会调研各种各样的生成策略，根据不同的业务，采取最合适的策略，下面我会讨论一下各种策略/算法，以及他们的一些优劣点。

# 2.UUID
UUID是通用唯一识别码（Universally Unique Identifier)的缩写，开放软件基金会(OSF)规范定义了包括网卡MAC地址、时间戳、名字空间（Namespace）、随机或伪随机数、时序等元素。利用这些元素来生成UUID。

UUID是由128位二进制组成，一般转换成十六进制，然后用String表示。在java中有个UUID类,在他的注释中我们看见这里有4种不同的UUID的生成策略:

![](https://user-gold-cdn.xitu.io/2018/9/28/16620a567adf101c?w=1246&h=154&f=png&s=55373)
- randomly: 基于随机数生成UUID，由于Java中的随机数是伪随机数，其重复的概率是可以被计算出来的。这个一般我们用下面的代码获取基于随机数的UUID:
![](https://user-gold-cdn.xitu.io/2018/9/29/1662309ffe5f4e64?w=872&h=206&f=png&s=35294)
- time-based:基于时间的UUID,这个一般是通过当前时间，随机数，和本地Mac地址来计算出来，自带的JDK包并没有这个算法的我们在一些UUIDUtil中，比如我们的log4j.core.util，会重新定义UUID的高位和低位。
![](https://user-gold-cdn.xitu.io/2018/9/29/166230ded459dfd1?w=1267&h=397&f=png&s=108363)
- DCE security:DCE安全的UUID。
- name-based：基于名字的UUID，通过计算名字和名字空间的MD5来计算UUID。

UUID的优点:
- 通过本地生成，没有经过网络I/O，性能较快
- 无序，无法预测他的生成顺序。(当然这个也是他的缺点之一)

UUID的缺点:
- 128位二进制一般转换成36位的16进制，太长了只能用String存储，空间占用较多。
- 不能生成递增有序的数字

适用场景:UUID的适用场景可以为不需要担心过多的空间占用，以及不需要生成有递增趋势的数字。在Log4j里面他在UuidPatternConverter中加入了UUID来标识每一条日志。

# 3.数据库主键自增
大家对于唯一标识最容易想到的就是主键自增，这个也是我们最常用的方法。例如我们有个订单服务，那么把订单id设置为主键自增即可。

优点:
- 简单方便，有序递增，方便排序和分页

缺点:
- 分库分表会带来问题，需要进行改造。
- 并发性能不高，受限于数据库的性能。
- 简单递增容易被其他人猜测利用，比如你有一个用户服务用的递增，那么其他人可以根据分析注册的用户ID来得到当天你的服务有多少人注册，从而就能猜测出你这个服务当前的一个大概状况。
- 数据库宕机服务不可用。

适用场景:
根据上面可以总结出来，当数据量不多，并发性能不高的时候这个很适合，比如一些to B的业务，商家注册这些，商家注册和用户注册不是一个数量级的，所以可以数据库主键递增。如果对顺序递增强依赖，那么也可以使用数据库主键自增。

# 4.Redis
熟悉Redis的同学，应该知道在Redis中有两个命令Incr，IncrBy,因为Redis是单线程的所以能保证原子性。

优点：
- 性能比数据库好，能满足有序递增。

缺点：
- 由于redis是内存的KV数据库，即使有AOF和RDB，但是依然会存在数据丢失，有可能会造成ID重复。
- 依赖于redis，redis要是不稳定，会影响ID生成。

适用：由于其性能比数据库好，但是有可能会出现ID重复和不稳定，这一块如果可以接受那么就可以使用。也适用于到了某个时间，比如每天都刷新ID，那么这个ID就需要重置，通过(Incr Today)，每天都会从0开始加。

# 5.Zookeeper
利用ZK的Znode数据版本如下面的代码，每次都不获取期望版本号也就是每次都会成功，那么每次都会返回最新的版本号:

![](https://user-gold-cdn.xitu.io/2018/9/29/166243d8d5897f41?w=1034&h=198&f=png&s=47686)

Zookeeper这个方案用得较少，严重依赖Zookeeper集群，并且性能不是很高，所以不予推荐。

# 6.数据库分段+服务缓存ID
这个方法在美团的Leaf中有介绍，详情可以参考美团技术团队的发布的技术文章:[Leaf——美团点评分布式ID生成系统](https://tech.meituan.com/MT_Leaf.html),这个方案是将数据库主键自增进行优化。

![](https://user-gold-cdn.xitu.io/2018/9/29/1662445bec45eb5d?w=1389&h=1033&f=png&s=228810)

biz_tag代表每个不同的业务，max_id代表每个业务设置的大小，step代表每个proxyServer缓存的步长。
之前我们的每个服务都访问的是数据库，现在不需要，每个服务直接和我们的ProxyServer做交互，减少了对数据库的依赖。我们的每个ProxyServer回去数据库中拿出步长的长度，比如server1拿到了1-1000,server2拿到来 1001-2000。如果用完会再次去数据库中拿。

优点:
- 比主键递增性能高，能保证趋势递增。
- 如果DB宕机，proxServer由于有缓存依然可以坚持一段时间。

缺点:
- 和主键递增一样，容易被人猜测。
- DB宕机，虽然能支撑一段时间但是仍然会造成系统不可用。

适用场景:需要趋势递增，并且ID大小可控制的，可以使用这套方案。

当然这个方案也可以通过一些手段避免被人猜测，把ID变成是无序的，比如把我们生成的数据是一个递增的long型，把这个Long分成几个部分，比如可以分成几组三位数，几组四位数，然后在建立一个映射表，将我们的数据变成无序。

# 7.雪花算法-Snowflake
Snowflake是Twitter提出来的一个算法，其目的是生成一个64bit的整数:

![](https://user-gold-cdn.xitu.io/2018/9/29/16624602fd5d9c4c?w=1740&h=435&f=png&s=124725)
- 1bit:一般是符号位，不做处理
- 41bit:用来记录时间戳，这里可以记录69年，如果设置好起始时间比如今年是2018年，那么可以用到2089年，到时候怎么办？要是这个系统能用69年，我相信这个系统早都重构了好多次了。
- 10bit:10bit用来记录机器ID，总共可以记录1024台机器，一般用前5位代表数据中心，后面5位是某个数据中心的机器ID
- 12bit:循环位，用来对同一个毫秒之内产生不同的ID，12位可以最多记录4095个，也就是在同一个机器同一毫秒最多记录4095个，多余的需要进行等待下毫秒。

上面只是一个将64bit划分的标准，当然也不一定这么做，可以根据不同业务的具体场景来划分，比如下面给出一个业务场景：
- 服务目前QPS10万，预计几年之内会发展到百万。
- 当前机器三地部署，上海，北京，深圳都有。
- 当前机器10台左右，预计未来会增加至百台。

这个时候我们根据上面的场景可以再次合理的划分62bit,QPS几年之内会发展到百万，那么每毫秒就是千级的请求，目前10台机器那么每台机器承担百级的请求，为了保证扩展，后面的循环位可以限制到1024，也就是2^10，那么循环位10位就足够了。

机器三地部署我们可以用3bit总共8来表示机房位置，当前的机器10台，为了保证扩展到百台那么可以用7bit 128来表示，时间位依然是41bit,那么还剩下64-10-3-7-41-1 = 2bit,还剩下2bit可以用来进行扩展。


![](https://user-gold-cdn.xitu.io/2018/9/29/16624909d2007c22?w=1497&h=103&f=png&s=22675)

适用场景:当我们需要无序不能被猜测的ID，并且需要一定高性能，且需要long型，那么就可以使用我们雪花算法。比如常见的订单ID，用雪花算法别人就无法猜测你每天的订单量是多少。

## 7.1一个简单的Snowflake

```
public class IdWorker{

    private long workerId;
    private long datacenterId;
    private long sequence = 0;
    /**
     * 2018/9/29日，从此时开始计算，可以用到2089年
     */
    private long twepoch = 1538211907857L;

    private long workerIdBits = 5L;
    private long datacenterIdBits = 5L;
    private long sequenceBits = 12L;

    private long workerIdShift = sequenceBits;
    private long datacenterIdShift = sequenceBits + workerIdBits;
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    // 得到0000000000000000000000000000000000000000000000000000111111111111
    private long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimestamp = -1L;


    public IdWorker(long workerId, long datacenterId){
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }
    public synchronized long nextId() {
        long timestamp = timeGen();
        //时间回拨，抛出异常
        if (timestamp < lastTimestamp) {
            System.err.printf("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp);
            throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds",
                    lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    /**
     * 当前ms已经满了
     * @param lastTimestamp
     * @return
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen(){
        return System.currentTimeMillis();
    }

    public static void main(String[] args) {
        IdWorker worker = new IdWorker(1,1);
        for (int i = 0; i < 30; i++) {
            System.out.println(worker.nextId());
        }
    }

}
```
上面定义了雪花算法的实现，在nextId中是我们生成雪花算法的关键。

## 7.2防止时钟回拨
因为机器的原因会发生时间回拨，我们的雪花算法是强依赖我们的时间的，如果时间发生回拨，有可能会生成重复的ID，在我们上面的nextId中我们用当前时间和上一次的时间进行判断，如果当前时间小于上一次的时间那么肯定是发生了回拨，普通的算法会直接抛出异常,这里我们可以对其进行优化,一般分为两个情况:
- 如果时间回拨时间较短，比如配置5ms以内，那么可以直接等待一定的时间，让机器的时间追上来。
- 如果时间的回拨时间较长，我们不能接受这么长的阻塞等待，那么又有两个策略:
1.  直接拒绝，抛出异常，打日志，通知RD时钟回滚。
2.  利用扩展位，上面我们讨论过不同业务场景位数可能用不到那么多，那么我们可以把扩展位数利用起来了，比如当这个时间回拨比较长的时候，我们可以不需要等待，直接在扩展位加1。2位的扩展位允许我们有3次大的时钟回拨，一般来说就够了，如果其超过三次我们还是选择抛出异常，打日志。

通过上面的几种策略可以比较的防护我们的时钟回拨，防止出现回拨之后大量的异常出现。下面是修改之后的代码，这里修改了时钟回拨的逻辑:

![](https://user-gold-cdn.xitu.io/2018/9/29/166252f2a1edac10?w=1263&h=935&f=png&s=182505)

# 最后
本文分析了各种生产分布式ID的算法的原理，以及他们的适用场景，相信你已经能为自己的项目选择好一个合适的分布式ID生成策略了。没有一个策略是完美的，只有适合自己的才是最好的。


![](https://user-gold-cdn.xitu.io/2018/7/22/164c2ad786c7cfe4?w=500&h=375&f=jpeg&s=215163)


