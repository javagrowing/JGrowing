
# 1.背景
我所在的公司最近要求需要在所有地方都要脱敏敏感数据，应该是受faceBook数据泄密影响吧。

说到脱敏一般来说在数据输出的地方需要脱敏而我们数据落地输出的地方一般是有三个地方:

- 接口返回值脱敏
- 日志脱敏
- 数据库脱敏

这里主要说一下如何进行日志脱敏，对于代码中来说日志打印敏感数据有两种:

1. 敏感数据在方法参数中

```
LOGGER.info("person mobile:{}", mobile);
```
对于这种建议写个Util直接进行脱敏，因为mobile这个参数名在代码中是无法获取的，当时有想过对传的参数使用正则匹配，这样的话效率太低，会让每个日志方法都进行正则匹配，效率极低，并且如果刚好有个符合手机号的字符串但是不是敏感信息，这样也被脱敏了。

```
 LOGGER.info("person mobile:{}", DesensitizationUtil.mobileDesensitiza(mobile));
```
2.敏感数据在参数对象中
```
 Person person = new Person();
 person.setMobile(mobile);
 LOGGER.info("person :{}", person);
```
对于我们业务中最多的其实就是上面的日志了，为了把整个参数打全，第一种方法需要把参数取出来，第二种只需要传一个参数即可，然后通过toString打印出这个日志，对于这种脱敏有两个方案
- 修改toString这个方法，对于修改toString方法又有三个办法:

1. 直接在toString中修改代码，这种方法很麻烦，效率低，需要修改每一个要脱敏的类，或者写个idea插件自动修改toString()，这样不好的地方在于所有编译器都需要开个插件，不够通用。
2. 在编译时期修改抽象语法树修改toString()方法，就像类似Lombok一样，这个之前调研过，开发难度较大，可能后会更新如何去写。
3. 在加载的时候通过实现Instrumentation接口 + asm库，修改class文件的字节码，但是有个比较麻烦的地方在于需要给jvm加上启动参数 -javaagent:agent_jar_path，这个已经实现了，但是实现后发现的确不够通用。
- 可以看到上面修改上面toString()方法三个都比较麻烦，我们可以换个思路，不利用toString()生成日志信息，下面的部分具体解释如何去做。



# 2.方案
首先我们要知道当我们使用LOGGER.info的时候到底是发生了什么？如下图所示，我这里列举的是异步的情况(我们项目中都是使用异步，同步效率太低了)。

![log4j流程图](https://user-gold-cdn.xitu.io/2018/7/7/164726371b50bab2?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)


log4j提供给我们扩展的地方实在是太多了，只要你有需求都可以在里面自定义，比如美团点评自己的xmdt统一日志和线下报警日志都是自己实现的Appender，统一日志也对LogEvent进行了封装。

我们同样也可以利用Log4j2提供给我们的扩展性，在里面定制化自己的需求。

## 2.1自定义PatterLayout的Convert

也就是修改上面图中第8步。
通过重写Convert，并且加入过滤逻辑。

优点：

这种方法是最理想的，他基本不会影响我们的日志的性能，因为过滤的逻辑都在PatterLayout里面。

缺点：

但是我在这个地方很尴尬我只能拿到已经生成的String,我只能用笨办法一个词一个词的匹配去搞，然后在修改这个词后面所接的数据进行脱敏，这样太复杂。有想过利用什么算法去优化(比如那些评论系统是如果过滤几万字文章的敏感词的)，但是这样成本太高，故而**放弃**。

## 2.2自定义全局filter
在想到第一个方法的时候，这个时候 其实是遇到瓶颈了，当时没有完全分析Log4j2的链路，后面我觉得可能从Log4j2全景链路上看，能找到更多的思路，所有便有了上面的图。

上面2.1的方案，为什么不大可行呢？主要是我只能拿到已经生成的String了。这个时候我就想我要是能修改String的生成方法就好了，日志其实就是一个字符串而已，具体这个字符串怎么来的不重要。

这个时候我就想到了json，json也是字符串，是我们数据交换的一种格式。利用生成Json的时候，进行过滤，对我们需要转换的值进行脱敏从而达到我们的目的。

当然转换Json和toString()方法，可能两个会有很大效率的差别，这个时候就只能祭出fastjson了，fastjson利用asm字节码技术，摆脱了反射的降低效率，下面的性能基准测试中也已经说明，效率影响基本可以忽略不计。

所以其实我们就需要两种filter:一个是log4j2的用于脱敏日志的filter,一个是fastjson的filter用于转换Json的时候进行对某些字段做处理。

优点：

改动最小，只需要在Log4j.xml配置文件中添加这个过滤器全局生效，即可使用。

缺点:  

1.既然是全局生效，必然会让每个日志都会从以前的toString转变为json，在追求极端性能的某些服务(比如哪怕多1ms都不可接受)上可能不适用。

2.可以看见我们这个是在第一步，而第一步的后面是自带的等级过滤器，因为我们有时候会动态调整日志级别，会导致我们这个哪怕不是当前可输出等级，他也会进行转换，有点得不偿失。

这个第二点经过优化我把等级过滤器的工作也提前做了，等级不够的直接拒绝。

示例代码如下:

/
```

@Plugin(name = "CrmSensitiveFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true) 
public class CrmSensitiveFilter extends AbstractFilter { 
    private static final long                       serialVersionUID = 1L; 
 
    private final boolean                           enabled; 
 
 
 
    private CrmSensitiveFilter(final boolean enabled, final Result onMatch, final Result onMismatch) { 
        super(onMatch, onMismatch); 
        //线上线下开关 
        this.enabled = enabled; 
    } 
 
    @Override 
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, 
                         final Throwable t) { 
        return filter(logger, level, marker, null, msg); 
    } 
 
    @Override 
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) { 
        if (this.enabled == false) { 
            return onMatch; 
        } 
        if (level == null || logger.getLevel().intLevel() < level.intLevel()) { 
            return onMismatch; 
        } 
        if (params == null || params.length <= 0) { 
            return super.filter(logger, level, marker, msg, params); 
        } 
        for (int i = 0; i < params.length; i++) { 
            params[i] = deepToString(params[i]); 
        } 
        return onMatch; 
    } 
 
 
 
    @PluginFactory 
    public static CrmSensitiveFilter createFilter(@PluginAttribute("enabled") final Boolean enabled, 
                                                  @PluginAttribute("onMatch") final Result match, 
                                                  @PluginAttribute("onMismatch") final Result mismatch) throws IllegalArgumentException, 
                                                                                                     IllegalAccessException { 
        return new CrmSensitiveFilter(enabled, match, mismatch); 
    } 
}
```

​
## 2.3重写MessageFactory
上面全局过滤器的缺点是无法定制化，这个时候我把目光锁定在第三步，生成日志内容输出Message。

通过重写MessageFactory我们可以生成我们自己的Message，并且我们能在代码层面指定我们的LoggerMannger到底是使用我们自己的MesssageFactory，还是使用默认的，能由我们自己控制。

当然我们这里生成的Message基本思路不变依然是fastjson的value过滤器。

优点:

能定制化LOGGER，非全局。

缺点：

局限于Log4j2,其他LogBack等日志框架不适用

下面给出部分代码:


```

public class DesensitizedMessageFactory extends AbstractMessageFactory { 
    private static final long                      serialVersionUID = 1L; 
 
    /** 
     * Instance of DesensitizedMessageFactory. 
     */ 
    public static final DesensitizedMessageFactory INSTANCE         = new DesensitizedMessageFactory(); 
 
    /** 
     * @param message The message pattern. 
     * @param params The message parameters. 
     * @return The Message. 
     * 
     * @see MessageFactory#newMessage(String, Object...) 
     */ 
    @Override 
    public Message newMessage(String message, Object... params) { 
        return new DesensitizedMessage(message, params); 
    } 
 
    /** 
     * 
     * @param message 
     * @return 
     */ 
    @Override 
    public Message newMessage(Object message) { 
        return new ObjectMessage(DesensitizedMessage.deepToString(message)); 
    } 
}
```



# 3.使用

我们团队业务项目之前log4j是使用的2.6版本的，之前是一直是使用的filter，突然有次升级直接升到2.7，突然一下脱敏不管用了，当时研究源码发现，filter发生了一些改变当传日志参数小于等于2的时候是有问题的。

需要根据自己业务场景选择一个最适合业务场景的:

log4j版本小于2.6使用filter，大于2.6(当然不大于2.6也能使用)使用MessageFactory

## 3.1 filter配置(二选一)
找到Log4j.xml（每个环境都有自己对应的哈）

在最外层节点下面，也就是<configuration></configuration>里面写如下配置，enabled用于线上线下切换，true为生效，false为不生效。

<filters> 
        <CrmSensitiveFilter enabled="true" onMatch="NEUTRAL" onMismatch="DENY"/> 
    </filters>
    
## 3.2 MessageFactory配置(二选一)


创建文件:log4j2.component.properties 

输入:log4j2.messageFactory=log.message.DesensitizedMessageFactory


#  4.性能基准测试:
基准测试聚焦打印日志效率如何。



硬件：

    4核,8G

操作系统：

    linux

JRE：

    v1.8.0_101，初始堆大小4G

预热策略：

    测试开始前，全局预热，执行全部测试若干次，判断运行时间稳定后停止，确保所需class全部加载完成

    每个测试开始前，独立预热，重复执行该测试64次，确保JIT编译器充分优化完代码。

执行策略：

    循环执行，初始次数200，以200的步长递增，递增至1000为止。

    每次执行10次，去掉一个最高，去掉一个最低，取平均值。

测试结果：



由上面结果可见增长速率基本稳定

上述结果脱敏的时间大概是未脱敏的时间1.5倍，

平均下来未脱敏的是0.1255ms 产生一条，而脱敏的是0.18825ms产生一条日志，两者相差0.06ms左右。

我们整个请求预估最多有10-20条日志打印，整条请求平均会影响时间0.6ms-1.2ms左右，我觉得这个时间可以忽略不计在整个请求当中。

所以这种模式的性能还是比较好，可以应用于生产环境。
![性能基准测试](https://img-blog.csdn.net/20180704200129376?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L2xpNTYzODY4Mjcz/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

