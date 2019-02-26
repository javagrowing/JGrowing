
![](https://user-gold-cdn.xitu.io/2018/7/24/164ca72ddcf3c7bd?w=702&h=369&f=png&s=13675)

1.概述
====

Apache Kafka最早是由LinkedIn开源出来的分布式消息系统，现在是Apache旗下的一个子项目，并且已经成为开源领域应用最广泛的消息系统之一。Kafka社区非常活跃,从0.9版本开始，Kafka的标语已经从“一个高吞吐量，分布式的消息系统”改为"一个分布式流平台"。

Kafka和传统的消息系统不同在于:

*   kafka是一个分布式系统，易于向外扩展。
    
*   它同时为发布和订阅提供高吞吐量
    
*   它支持多订阅者，当失败时能自动平衡消费者
    
*   消息的持久化
    

kafka和其他消息队列的对比:

<table style="width: 1336px;"><tr class="cloneHeader" style="right: 32px; left: -125px;"><th width="50px" data-colwidth="50" style="min-width: 50px; max-width: 50px;"><p>&nbsp;</p></th><th width="460px" data-colwidth="460" style="min-width: 460px; max-width: 460px;"><p>kafka</p></th><th width="273px" data-colwidth="273" style="min-width: 273px; max-width: 273px;"><p>activemq</p></th><th width="318px" data-colwidth="318" style="min-width: 318px; max-width: 318px;"><p>rabbitmq</p></th><th width="235px" data-colwidth="235" style="min-width: 235px; max-width: 235px;"><p>rocketmq</p></th></tr><colgroup><col style="width: 50px;"><col style="width: 460px;"><col style="width: 273px;"><col style="width: 318px;"><col style="width: 235px;"></colgroup><tbody><tr class="originHeader"></tr><tr><td width="50px" data-colwidth="50"><p>背景</p></td><td width="460px" data-colwidth="460"><p><span style="color: rgb(64, 64, 64);">Kafka 是LinkedIn 开发的一个高性能、分布式的消息系统，广泛用于日志收集、流式数据处理、在线和离线消息分发等场景</span></p></td><td width="273px" data-colwidth="273"><p>&nbsp;ActiveMQActiveMQ是一种开源的，实现了JMS1.1规范的，面向消息(MOM)的中间件，为应用程序提供高效的、可扩展的、稳定的和安全的企业级消息通信。</p></td><td width="318px" data-colwidth="318"><p><span style="color: rgb(47, 47, 47);">RabbitMQ是一个由erlang开发的AMQP协议（Advanced Message Queue ）的开源实现。</span></p></td><td width="235px" data-colwidth="235"><p><span style="color: rgb(69, 69, 69);">&nbsp;&nbsp; RocketMQ是阿里巴巴在2012年开源的分布式消息中间件，目前已经捐赠给Apache基金会，已经于2016年11月成为 Apache 孵化项目</span></p></td></tr><tr><td width="50px" data-colwidth="50"><p>开发语言</p></td><td width="460px" data-colwidth="460"><p>java,scala</p></td><td width="273px" data-colwidth="273"><p><span style="color: rgb(69, 69, 69);">Java</span></p></td><td width="318px" data-colwidth="318"><p><span style="color: rgb(69, 69, 69);">Erlang</span></p></td><td width="235px" data-colwidth="235"><p>Java</p></td></tr><tr><td width="50px" data-colwidth="50"><p><strong>协议支持</strong></p></td><td width="460px" data-colwidth="460"><p>自己制定的一套协议</p></td><td width="273px" data-colwidth="273"><p>JMS协议</p></td><td width="318px" data-colwidth="318"><p>AMQP</p></td><td width="235px" data-colwidth="235"><p><span style="color: rgb(47, 47, 47);">&nbsp;JMS、MQTT</span></p></td></tr><tr><td width="50px" data-colwidth="50"><p><strong>持久化支持</strong></p></td><td width="460px" data-colwidth="460"><p>支持</p></td><td width="273px" data-colwidth="273"><p>支持</p></td><td width="318px" data-colwidth="318"><p>支持</p></td><td width="235px" data-colwidth="235"><p>支持</p></td></tr><tr><td width="50px" data-colwidth="50"><p>事务支持</p></td><td width="460px" data-colwidth="460"><p>0.11.0之后支持</p></td><td width="273px" data-colwidth="273"><p>支持</p></td><td width="318px" data-colwidth="318"><p>支持</p></td><td width="235px" data-colwidth="235"><p>支持</p></td></tr><tr><td width="50px" data-colwidth="50"><p>producer容错</p></td><td width="460px" data-colwidth="460"><p>在kafka中提供了ack配置选项,</p><p>request.required.acks=-1,级别最低，生产者不需要关心是否发送成功</p><p>request.required.acks=0,只需要leader分区有了即可</p><p>request.required.acks=1,isr集合中的所有同步了才返回</p><p>可能会有重复数据</p></td><td width="273px" data-colwidth="273"><p>发送失败后即可重试</p></td><td width="318px" data-colwidth="318"><p>有ack模型</p><p>ack模型可能重复消息</p><p>事务模型保证完全一致</p></td><td width="235px" data-colwidth="235"><p>和kafka类似</p></td></tr><tr><td width="50px" data-colwidth="50"><p>吞吐量</p></td><td width="460px" data-colwidth="460"><p><span style="color: rgb(0, 0, 0);">kafka具有高的吞吐量，内部采用消息的批量处理，zero-copy机制，数据的存储和获取是本地磁盘顺序批量操作，具有O(1)的复杂度，消息处理的效率很高</span></p></td><td width="273px" data-colwidth="273"><p><span style="color: rgb(0, 0, 0);"><br></span></p></td><td width="318px" data-colwidth="318"><p><span style="color: rgb(0, 0, 0);">rabbitMQ在吞吐量方面稍逊于kafka，他们的出发点不一样，rabbitMQ支持对消息的可靠的传递，支持事务，不支持批量的操作；基于存储的可靠性的要求存储可以采用内存或者硬盘。</span></p></td><td width="235px" data-colwidth="235"><p>kafka在topic数量不多的情况下吞吐量比rocketMq高，在topic数量多的情况下rocketMq比kafka高</p></td></tr><tr><td width="50px" data-colwidth="50"><p>负载均衡</p></td><td width="460px" data-colwidth="460"><p><span style="color: rgb(0, 0, 0);">kafka采用zookeeper对集群中的broker、consumer进行管理，可以注册topic到zookeeper上；通过zookeeper的协调机制，producer保存对应topic的broker信息，可以随机或者轮询发送到broker上；并且producer可以基于语义指定分片，消息发送到broker的某分片上</span></p></td><td width="273px" data-colwidth="273"><p>&nbsp;</p></td><td width="318px" data-colwidth="318"><p><span style="color: rgb(0, 0, 0);">rabbitMQ的负载均衡需要单独的loadbalancer进行支持</span></p></td><td width="235px" data-colwidth="235"><p>NamerServer进行负载均衡</p></td></tr></tbody></table>

2.入门实例
======

2.1生产者
------

**producer**


```
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class UserKafkaProducer extends Thread
{
    private final KafkaProducer<Integer, String> producer;
    private final String topic;
    private final Properties props = new Properties();
    public UserKafkaProducer(String topic)
    {
        props.put("metadata.broker.list", "localhost:9092");
        props.put("bootstrap.servers", "master2:6667");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<Integer, String>(props);
        this.topic = topic;
    }
    @Override
    public void run() {
        int messageNo = 1;
        while (true)
        {
            String messageStr = new String("Message_" + messageNo);
            System.out.println("Send:" + messageStr);
			//返回的是Future<RecordMetadata>,异步发送
            producer.send(new ProducerRecord<Integer, String>(topic, messageStr));
            messageNo++;
            try {
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```


## 2.2 消费者


```
Properties props = new Properties();
/* 定义kakfa 服务的地址，不需要将所有broker指定上 */
props.put("bootstrap.servers", "localhost:9092");
/* 制定consumer group */
props.put("group.id", "test");
/* 是否自动确认offset */
props.put("enable.auto.commit", "true");
/* 自动确认offset的时间间隔 */
props.put("auto.commit.interval.ms", "1000");
props.put("session.timeout.ms", "30000");
/* key的序列化类 */
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
/* value的序列化类 */
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
 /* 定义consumer */
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
/* 消费者订阅的topic, 可同时订阅多个 */
consumer.subscribe(Arrays.asList("foo", "bar"));

 /* 读取数据，读取超时时间为100ms */
while (true) {
    ConsumerRecords<String, String> records = consumer.poll(100);
    for (ConsumerRecord<String, String> record : records)
        System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
}
```


3.Kafka架构原理
===========

对于kafka的架构原理我们先提出几个问题?

1.Kafka的topic和分区内部是如何存储的，有什么特点？

2.与传统的消息系统相比,Kafka的消费模型有什么优点?

3.Kafka如何实现分布式的数据存储与数据读取?

3.1Kafka架构图
-----------


![](https://user-gold-cdn.xitu.io/2018/7/24/164ca4624c6bdcf2?w=1690&h=1017&f=png&s=129957)

3.2kafka名词解释
------------

在一套kafka架构中有多个Producer，多个Broker,多个Consumer，每个Producer可以对应多个Topic，每个Consumer只能对应一个ConsumerGroup。

整个Kafka架构对应一个ZK集群，通过ZK管理集群配置，选举Leader，以及在consumer group发生变化时进行rebalance。

<table style="width: 1196px;"><tr class="cloneHeader" style="right: 32px; left: 8px;"><th width="126px" data-colwidth="126" style="min-width: 126px; max-width: 126px;"><p style="text-align: center;">名称</p></th><th width="1070px" data-colwidth="1070" style="min-width: 1070px; max-width: 1070px;"><p style="text-align: center;">解释</p></th></tr><colgroup><col style="width: 126px;"><col style="width: 1070px;"></colgroup><tbody><tr><td width="126px" data-colwidth="126"><p>Broker</p></td><td width="1070px" data-colwidth="1070"><p>消息中间件处理节点，一个Kafka节点就是一个broker，一个或者多个Broker可以组成一个Kafka集群</p></td></tr><tr><td width="126px" data-colwidth="126"><p>Topic</p></td><td width="1070px" data-colwidth="1070"><p>主题，Kafka根据topic对消息进行归类，发布到Kafka集群的每条消息都需要指定一个topic</p></td></tr><tr><td width="126px" data-colwidth="126"><p>Producer</p></td><td width="1070px" data-colwidth="1070"><p>消息生产者，向Broker发送消息的客户端</p></td></tr><tr><td width="126px" data-colwidth="126"><p>Consumer</p></td><td width="1070px" data-colwidth="1070"><p>消息消费者，从Broker读取消息的客户端</p></td></tr><tr><td width="126px" data-colwidth="126"><p>ConsumerGroup</p></td><td width="1070px" data-colwidth="1070"><p>每个Consumer属于一个特定的Consumer Group，一条消息可以发送到多个不同的Consumer Group，但是一个Consumer Group中只能有一个Consumer能够消费该消息</p></td></tr><tr><td width="126px" data-colwidth="126"><p>Partition</p></td><td width="1070px" data-colwidth="1070"><p>物理上的概念，一个topic可以分为多个partition，每个partition内部是有序的</p></td></tr></tbody></table>

3.3Topic和Partition
------------------

在Kafka中的每一条消息都有一个topic。一般来说在我们应用中产生不同类型的数据，都可以设置不同的主题。一个主题一般会有多个消息的订阅者，当生产者发布消息到某个主题时，订阅了这个主题的消费者都可以接收到生产者写入的新消息。

kafka为每个主题维护了分布式的分区(partition)日志文件，每个partition在kafka存储层面是append log。任何发布到此partition的消息都会被追加到log文件的尾部，在分区中的每条消息都会按照时间顺序分配到一个单调递增的顺序编号，也就是我们的offset,offset是一个long型的数字，我们通过这个offset可以确定一条在该partition下的唯一消息。在partition下面是保证了有序性，但是在topic下面没有保证有序性。

![](https://user-gold-cdn.xitu.io/2018/7/24/164ca4ed208c90b5?w=1405&h=495&f=png&s=90138)


在上图中在我们的生产者会决定发送到哪个Partition。

1.如果没有Key值则进行轮询发送。

2.如果有Key值，对Key值进行Hash，然后对分区数量取余，保证了同一个Key值的会被路由到同一个分区，如果想队列的强顺序一致性，可以让所有的消息都设置为同一个Key。

3.4消费模型
-------

消息由生产者发送到kafka集群后，会被消费者消费。一般来说我们的消费模型有两种:推送模型(psuh)和拉取模型(pull)

基于推送模型的消息系统，由消息代理记录消费状态。消息代理将消息推送到消费者后，标记这条消息为已经被消费，但是这种方式无法很好地保证消费的处理语义。比如当我们把已经把消息发送给消费者之后，由于消费进程挂掉或者由于网络原因没有收到这条消息，如果我们在消费代理将其标记为已消费，这个消息就永久丢失了。如果我们利用生产者收到消息后回复这种方法，消息代理需要记录消费状态，这种不可取。如果采用push，消息消费的速率就完全由消费代理控制，一旦消费者发生阻塞，就会出现问题。

Kafka采取拉取模型(poll)，由自己控制消费速度，以及消费的进度，消费者可以按照任意的偏移量进行消费。比如消费者可以消费已经消费过的消息进行重新处理，或者消费最近的消息等等。

3.5网络模型
-------

### 3.5.1 KafkaClient --单线程Selector

![](https://user-gold-cdn.xitu.io/2018/7/24/164ca4f14f4304d2?w=990&h=685&f=png&s=117940)

单线程模式适用于并发链接数小，逻辑简单，数据量小。

在kafka中，consumer和producer都是使用的上面的单线程模式。这种模式不适合kafka的服务端，在服务端中请求处理过程比较复杂，会造成线程阻塞，一旦出现后续请求就会无法处理，会造成大量请求超时，引起雪崩。而在服务器中应该充分利用多线程来处理执行逻辑。

### 3.5.2 Kafka--server -- 多线程Selector

![](https://user-gold-cdn.xitu.io/2018/7/24/164ca4f676316f8d?w=1158&h=674&f=png&s=137678)

在kafka服务端采用的是多线程的Selector模型，Acceptor运行在一个单独的线程中，对于读取操作的线程池中的线程都会在selector注册read事件，负责服务端读取请求的逻辑。成功读取后，将请求放入message queue共享队列中。然后在写线程池中，取出这个请求，对其进行逻辑处理，即使某个请求线程阻塞了，还有后续的县城从消息队列中获取请求并进行处理，在写线程中处理完逻辑处理，由于注册了OP_WIRTE事件，所以还需要对其发送响应。

3.6高可靠分布式存储模型
-------------

在Kafka中保证高可靠模型的依靠的是副本机制，有了副本机制之后，就算机器宕机也不会发生数据丢失。

### 3.6.1高性能的日志存储

kafka一个topic下面的所有消息都是以partition的方式分布式的存储在多个节点上。同时在kafka的机器上，每个Partition其实都会对应一个日志目录，在目录下面会对应多个日志分段(LogSegment)。LogSegment文件由两部分组成，分别为“.index”文件和“.log”文件，分别表示为segment索引文件和数据文件。这两个文件的命令规则为：partition全局的第一个segment从0开始，后续每个segment文件名为上一个segment文件最后一条消息的offset值，数值大小为64位，20位数字字符长度，没有数字用0填充，如下，假设有1000条消息，每个LogSegment大小为100，下面展现了900-1000的索引和Log：

![](https://user-gold-cdn.xitu.io/2018/7/24/164ca4f7e778be7c?w=651&h=478&f=png&s=55737)

由于kafka消息数据太大，如果全部建立索引，即占了空间又增加了耗时，所以kafka选择了稀疏索引的方式，这样的话索引可以直接进入内存，加快偏查询速度。

简单介绍一下如何读取数据，如果我们要读取第911条数据首先第一步，找到他是属于哪一段的，根据二分法查找到他属于的文件，找到0000900.index和00000900.log之后，然后去index中去查找 (911-900) =11这个索引或者小于11最近的索引,在这里通过二分法我们找到了索引是[10,1367]然后我们通过这条索引的物理位置1367，开始往后找，直到找到911条数据。

上面讲的是如果要找某个offset的流程，但是我们大多数时候并不需要查找某个offset,只需要按照顺序读即可，而在顺序读中，操作系统会对内存和磁盘之间添加page cahe，也就是我们平常见到的预读操作，所以我们的顺序读操作时速度很快。但是kafka有个问题，如果分区过多，那么日志分段也会很多，写的时候由于是批量写，其实就会变成随机写了，随机I/O这个时候对性能影响很大。所以一般来说Kafka不能有太多的partition。针对这一点，RocketMQ把所有的日志都写在一个文件里面，就能变成顺序写，通过一定优化，读也能接近于顺序读。

>可以思考一下:1.为什么需要分区，也就是说主题只有一个分区，难道不行吗？2.日志为什么需要分段


>1.分区是为了水平扩展
2.日志如果在同一个文件太大会影响性能。如果日志无限增长，查询速度会减慢

### 3.6.2副本机制

Kafka的副本机制是多个服务端节点对其他节点的主题分区的日志进行复制。当集群中的某个节点出现故障，访问故障节点的请求会被转移到其他正常节点(这一过程通常叫Reblance),kafka每个主题的每个分区都有一个主副本以及0个或者多个副本，副本保持和主副本的数据同步，当主副本出故障时就会被替代。

![](https://user-gold-cdn.xitu.io/2018/7/24/164ca4f9594fe4fe?w=1452&h=589&f=png&s=73653)

在Kafka中并不是所有的副本都能被拿来替代主副本，所以在kafka的leader节点中维护着一个ISR(In sync Replicas)集合，翻译过来也叫正在同步中集合，在这个集合中的需要满足两个条件:

*   节点必须和ZK保持连接
    
*   在同步的过程中这个副本不能落后主副本太多
    

另外还有个AR(Assigned Replicas)用来标识副本的全集,OSR用来表示由于落后被剔除的副本集合，所以公式如下:ISR = leader + 没有落后太多的副本; AR = OSR+ ISR;

这里先要说下两个名词:HW(高水位)是consumer能够看到的此partition的位置，LEO是每个partition的log最后一条Message的位置。HW能保证leader所在的broker失效，该消息仍然可以从新选举的leader中获取，不会造成消息丢失。

当producer向leader发送数据时，可以通过request.required.acks参数来设置数据可靠性的级别：

*   1（默认）：这意味着producer在ISR中的leader已成功收到的数据并得到确认后发送下一条message。如果leader宕机了，则会丢失数据。
    
*   0：这意味着producer无需等待来自broker的确认而继续发送下一批消息。这种情况下数据传输效率最高，但是数据可靠性确是最低的。
    
*   -1：producer需要等待ISR中的所有follower都确认接收到数据后才算一次发送完成，可靠性最高。但是这样也不能保证数据不丢失，比如当ISR中只有leader时(其他节点都和zk断开连接，或者都没追上)，这样就变成了acks=1的情况。
    

4.高可用模型及幂等
==========

 在分布式系统中一般有三种处理语义:

*   **at-least-once：**
    
    至少一次，有可能会有多次。如果producer收到来自ack的确认，则表示该消息已经写入到Kafka了，此时刚好是一次，也就是我们后面的exactly-once。但是如果producer超时或收到错误，并且request.required.acks配置的不是-1，则会重试发送消息，客户端会认为该消息未写入Kafka。如果broker在发送Ack之前失败，但在消息成功写入Kafka之后，这一次重试将会导致我们的消息会被写入两次，所以消息就不止一次地传递给最终consumer，如果consumer处理逻辑没有保证幂等的话就会得到不正确的结果。  
    在这种语义中会出现乱序，也就是当第一次ack失败准备重试的时候，但是第二消息已经发送过去了，这个时候会出现单分区中乱序的现象,我们需要设置Prouducer的参数[max.in](http://max.in/ "max.in").flight.requests.per.connection，flight.requests是Producer端用来保存发送请求且没有响应的队列，保证Producer端未响应的请求个数为1。
    
*   **at-most-once:**
    
    如果在ack超时或返回错误时producer不重试，也就是我们讲request.required.acks=-1，则该消息可能最终没有写入kafka，所以consumer不会接收消息。
    
*   **exactly-once：**
    
    刚好一次，即使producer重试发送消息，消息也会保证最多一次地传递给consumer。该语义是最理想的，也是最难实现的。在0.10之前并不能保证exactly-once，需要使用consumer自带的幂等性保证。0.11.0使用事务保证了
    

4.1 如何实现exactly-once
--------------------

要实现exactly-once在Kafka 0.11.0中有两个官方策略:

### 4.1.1单Producer单Topic

每个producer在初始化的时候都会被分配一个唯一的PID，对于每个唯一的PID，Producer向指定的Topic中某个特定的Partition发送的消息都会携带一个从0单调递增的sequence number。

在我们的Broker端也会维护一个维度为<PID,Topic,Partition>，每次提交一次消息的时候都会对齐进行校验:

*   如果消息序号比Broker维护的序号大一以上，说明中间有数据尚未写入，也即乱序，此时Broker拒绝该消息，Producer抛出InvalidSequenceNumber
    
*   如果消息序号小于等于Broker维护的序号，说明该消息已被保存，即为重复消息，Broker直接丢弃该消息，Producer抛出DuplicateSequenceNumber
    
*   如果消息序号刚好大一，就证明是合法的
    

上面所说的解决了两个问题:

1.当Prouducer发送了一条消息之后失败，broker并没有保存，但是第二条消息却发送成功，造成了数据的乱序。

2.当Producer发送了一条消息之后，broker保存成功，ack回传失败，producer再次投递重复的消息。

上面所说的都是在同一个PID下面，意味着必须保证在单个Producer中的同一个seesion内，如果Producer挂了，被分配了新的PID，这样就无法保证了，所以Kafka中又有事务机制去保证。

### 4.1.2事务

在kafka中事务的作用是

*   实现exactly-once语义
    
*   保证操作的原子性，要么全部成功，要么全部失败。
    
*   有状态的操作的恢复
    

事务可以保证就算跨多个<Topic, Partition>，在本次事务中的对消费队列的操作都当成原子性，要么全部成功，要么全部失败。并且，有状态的应用也可以保证重启后从断点处继续处理，也即事务恢复。在kafka的事务中，应用程序必须提供一个唯一的事务ID，即Transaction ID，并且宕机重启之后，也不会发生改变，Transactin ID与PID可能一一对应。区别在于Transaction ID由用户提供，而PID是内部的实现对用户透明。为了Producer重启之后，旧的Producer具有相同的Transaction ID失效，每次Producer通过Transaction ID拿到PID的同时，还会获取一个单调递增的epoch。由于旧的Producer的epoch比新Producer的epoch小，Kafka可以很容易识别出该Producer是老的Producer并拒绝其请求。为了实现这一点，Kafka 0.11.0.0引入了一个服务器端的模块，名为Transaction Coordinator，用于管理Producer发送的消息的事务性。该Transaction Coordinator维护Transaction Log，该log存于一个内部的Topic内。由于Topic数据具有持久性，因此事务的状态也具有持久性。Producer并不直接读写Transaction Log，它与Transaction Coordinator通信，然后由Transaction Coordinator将该事务的状态插入相应的Transaction Log。Transaction Log的设计与Offset Log用于保存Consumer的Offset类似。

# 最后
关于消息队列或者Kafka的一些常见的面试题，通过上面的文章可以提炼出以下几个比较经典的问题:
1. 为什么使用消息队列？消息队列的作用是什么？
2. Kafka的topic和分区内部是如何存储的，有什么特点？
3. 与传统的消息系统相比,Kafka的消费模型有什么优点?
4. Kafka如何实现分布式的数据存储与数据读取?
5. kafka为什么比rocketmq支持的单机partion要少?
6. 为什么需要分区，也就是说主题只有一个分区，难道不行吗？
7. 日志为什么需要分段？
8. kafka是依靠什么机制保持高可靠，高可用？
9. 消息队列如何保证消息幂等？
10. 让你自己设计个消息队列，你会怎么设计，会考虑哪些方面？
