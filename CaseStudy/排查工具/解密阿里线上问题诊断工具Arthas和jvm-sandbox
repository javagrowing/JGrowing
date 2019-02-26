# 大纲目录
这篇文章是之前学习Arthas和jvm-sandbox的一些心得和总结，希望能帮助到大家。本文字较多，可以根据目录进行对应的阅读。
- 背景:现在的问题所在？
- Arthas: Arthas能帮助你干什么？各种命令原理是什么？
- jvm-sandbox: jvm-sandbox能帮助你干什么？
- 实现原理？自己如何实现一个？
- 常见的一些问题？
# 1.背景

2018年已过，但是在过去的一年里面开源了很多优秀的项目,这里我要介绍两个比较相似的阿里开源项目一个是Arthas,另一个是jvm-sandbox。这两个项目都是在今年开源的，为什么要介绍这两个项目呢？这里先卖个关子，先问下大家不知道是否遇到过下面的场景呢？

- 当你线上项目出了问题，但是一打开日志发现，有些地方忘记打了日志，于是你马上补上日志，然后重新上线。这个在一些上线流程不规范的公司还比较轻松，在一些流程比较严格，比如美团上线的时候就有封禁期，一般就只能9点之后才能上线。有可能这样一拖就耽误了解决问题的黄金时刻。
- 当你的项目某个接口执行速度较慢，为了排查问题，于是你四处加上每个方法运行时间。
- 当你发现某个类有冲突，好像在线上运行的结果和你预期的不符合，手动把线上编译出的class文件下载下来然后反编译，看看究竟class内容是什么。
- 当代码已经写好准备联调，但是下游业务环境并没有准备好，于是你把以前的代码依次进行注释，采用mock的形式又写了一遍方便联调。

以上这些场景，再真正的业务开发中大家或多或少都遇见过，而一般大家的处理方式和我在场景的描述得大体一致。而这里要给大家介绍一下Arthas和jvm-sandbox，如果你学会了这两个项目，上面所有的问题再你手上再也不是难事。
# 2. Arthas
当然再介绍Arthas之前还是要给大家说一下Greys,无论是Arthas还是jvm-sandbox都是从Greys演变而来，这个是2014年阿里开源的一款Java在线问题诊断工具。而Arthas可以看做是他的升级版本，是一款更加优秀的，功能更加丰富的Java诊断工具。
在他的github的READEME中的介绍这款工具可以帮助你做下面这些事:
- 这个类从哪个 jar 包加载的？为什么会报各种类相关的 Exception？
- 我改的代码为什么没有执行到？难道是我没 commit？分支搞错了？
- 遇到问题无法在线上 debug，难道只能通过加日志再重新发布吗？
- 线上遇到某个用户的数据处理有问题，但线上同样无法 debug，线下无法重现！
- 是否有一个全局视角来查看系统的运行状况？
- 有什么办法可以监控到JVM的实时运行状态？

下面我将会介绍一下Arthas的一些常用的命令和用法，看看是如何解决我们实际中的问题的，至于安装教程可以参考Arthas的github。

## 2.1 奇怪的类加载错误
相信大家都遇到过NoSuchMethodError这个错误，一般老司机看见这个错误第一反应就是jar包版本号冲突，这种问题一般来说使用maven的一些插件就能轻松解决。

之前遇到个奇怪的问题，我们有两个服务的client-jar包，有个类的包名和类名均是一致，在编写代码的时候没有注意到这个问题，在编译阶段由于包名和类名都是一致，所有编译阶段并没有报错，在线下的运行阶段没有问题，但是测试环境的机器中的运行阶段缺报出了问题。这个和之前的jar包版本号冲突有点不同，因为在排查的时候我们想使用A服务的client-jar包的这个类，但是这个jar包的版本号在Maven中的确是唯一的。

这个时候Arthas就可以大显神通了。
#### 2.1.1 sc命令
找到对应的类，然后输出下面的命令(用例使用的是官方提供的用例):

```
$ sc -d demo.MathGame
class-info        demo.MathGame
code-source       /private/tmp/arthas-demo.jar
name              demo.MathGame
isInterface       false
isAnnotation      false
isEnum            false
isAnonymousClass  false
isArray           false
isLocalClass      false
isMemberClass     false
isPrimitive       false
isSynthetic       false
simple-name       MathGame
modifier          public
annotation
interfaces
super-class       +-java.lang.Object
class-loader      +-sun.misc.Launcher$AppClassLoader@3d4eac69
                    +-sun.misc.Launcher$ExtClassLoader@66350f69
classLoaderHash   3d4eac69
 
Affect(row-cnt:1) cost in 875 ms.
```
可以看见打印出了code-source,当时发现了code-source并不是从对应的Jar包取出来的，于是发现了两个服务对于同一个类使用了同样的包名和类名，导致了这个奇怪的问题，后续通过修改包名和类名进行解决。

##### sc原理
sc的信息主要从对应的Class中获取。
比如isInterface,isAnnotation等等都是通过下面的方式获取:


![](https://user-gold-cdn.xitu.io/2019/1/20/1686a17f1ae75d46?w=818&h=398&f=png&s=119353)
对于我们上面的某个类从哪个jar包加载的是通过CodeSource来进行获取的:


![](https://user-gold-cdn.xitu.io/2019/1/20/1686a16e10f0fa19?w=904&h=143&f=png&s=28473)
#### 2.1.2 jad
Arthas还提供了一个命令jad用来反编译,对于解决类冲突错误很有用，比如我们想知道这个类里面的代码到底是什么，直接一个jad命令就能搞定:

```
$ jad java.lang.String
 
ClassLoader:
 
Location:
 
/*
* Decompiled with CFR 0_132.
*/
package java.lang;
 
import java.io.ObjectStreamField;
...
public final class String
implements Serializable,
Comparable<String>,
CharSequence {
    private final char[] value;
    private int hash;
    private static final long serialVersionUID = -6849794470754667710L;
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[0];
    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();
 
    public String(byte[] arrby, int n, int n2) {
        String.checkBounds(arrby, n, n2);
        this.value = StringCoding.decode(arrby, n, n2);
    }
...

```
一般通过这个命令我们就能发现和你所期待的类是否缺少了某些方法，或者某些方法有些改变，从而确定jar包冲突。

##### jad原理
jad使用的是cfr提供的jar包来进行反编译。这里过程比较复杂这里就不进行叙述。

## 2.2 动态修改日志级别
有很多同学可能会觉得动态修改日志有什么用呢？好像自己也没怎么用过呢？
一般来说下面这几个场景可以需要:
- 一般大家日志级别默认是info，有时候需要查看debug的日志可能需要重新上线。
- 当线上某个应用流量比较大的时候，如何业务出现问题，可能会短时间之内产生大量日志，由于日志会写盘，会消耗大量的内存和磁盘IO进一步加重我们的问题严重性，进而引起雪崩。
我们可以使用动态修改日志解决我们上面两个问题，在美团内部开发了一个工具通过LogContext，记录下所有的logConfig然后动态修改更新。但是如果没有这个工具我们如何动态修改日志呢？

#### 2.2.1 ognl
ognl是一门表达式语言，在Arthas中你可以利用这个表达式语言做很多事，比如执行某个方法，获取某个信息。再这里我们可以通过下面的命令来动态的修改日志级别:

```
 $ ognl '@com.lz.test@LOGGER.logger.privateConfig'
@PrivateConfig[
    loggerConfig=@LoggerConfig[root],
    loggerConfigLevel=@Level[INFO],
    intLevel=@Integer[400],
]
$ ognl '@com.lz.test@LOGGER.logger.setLevel(@org.apache.logging.log4j.Level@ERROR)'
null
$ ognl '@com.lz.test@LOGGER.logger.privateConfig'
@PrivateConfig[
    loggerConfig=@LoggerConfig[root],
    loggerConfigLevel=@Level[ERROR],
    intLevel=@Integer[200],
  
]

```
上面的命令可以修改对应类中的info日志为error日志打印级别，如果想全局修改root的级别的话对于ognl表达式来说执行比较困难，总的来说需要将ognl翻译为下面这段代码:
```
org.apache.logging.log4j.core.LoggerContext loggerContext = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Map<String, LoggerConfig> map = loggerContext.getConfiguration().getLoggers();
        for (org.apache.logging.log4j.core.config.LoggerConfig loggerConfig : map.values()) {
            String key = loggerConfig.getName();
            if (StringUtils.isBlank(key)) {
                loggerConfig.setLevel(Level.ERROR);
            }
        }
loggerContext.updateLoggers();
```
总的来说比较复杂，这里不给予实现，如果有兴趣的可以用代码的形式去实现以下，美团的动态调整日志组件也是通过这种方法实现的。

##### 原理
具体原理是首先获取AppClassLoader(默认)或者指定的ClassLoader，然后再调用Ognl的包，自动执行解析这个表达式，而这个执行的类都会从前面的ClassLoader中获取中去获取。

## 2.3 如何知道某个方法是否调用
很多时候我们方法执行的情况和我们预期不符合，但是我们又不知道到底哪里不符合，Arthas的watch命令就能帮助我们解决这个问题。
#### 2.3.1 watch
watch命令顾名思义观察，他可以观察指定方法调用情况，定义了4个观察事件点， -b 方法调用前，-e 方法异常后，-s 方法返回后，-f 方法结束后。默认是-f

比如我们想知道某个方法执行的时候，参数和返回值到底是什么。注意这里的参数是方法执行完成的时候的参数，和入参不同有可能会发生变化。

```
$ watch demo.MathGame primeFactors "{params,returnObj}" -x 2
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 44 ms.
ts=2018-12-03 19:16:51; [cost=1.280502ms] result=@ArrayList[
    @Object[][
        @Integer[535629513],
    ],
    @ArrayList[
        @Integer[3],
        @Integer[19],
        @Integer[191],
        @Integer[49199],
    ],
]
```
你能得到参数和返回值的情况，以及方法时间消耗的等信息。

##### 原理
利用jdk1.6的instrument + ASM 记录方法的入参出参，以及方法消耗时间。


## 2.4 如何知道某个方法耗时较多
当某个方法耗时较长，这个时候你需要排查到底是某一处发生了长时间的耗时，一般这种问题比较难排查，都是通过全链路追踪trace图去进行排查，但是在本地的应用中没有trace图，这个时候需要Arthas的trace命令来进行排查问题。
#### 2.4.1 trace
trace 命令能主动搜索 class-pattern／method-pattern 对应的方法调用路径，渲染和统计整个调用链路上的所有性能开销和追踪调用链路。

但是trace只能追踪一层的调用链路，如果一层的链路信息不够用，可以把该链路上有问题的方法再次进行trace。
trace使用例子如下。

```
$ trace demo.MathGame run
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 42 ms.
`---ts=2018-12-04 00:44:17;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@3d4eac69
    `---[10.611029ms] demo.MathGame:run()
        +---[0.05638ms] java.util.Random:nextInt()
        +---[10.036885ms] demo.MathGame:primeFactors()
        `---[0.170316ms] demo.MathGame:print()
```
可以看见上述耗时最多的方法是primeFactors，所以我们可以对其进行trace进行再一步的排查。

##### 原理
利用jdk1.6的instrument + ASM。在访问方法之前和之后会进行记录。

## 2.5 如何使用命令重发请求？
有时候排查一个问题需要上游再次调用这个方法，比如使用postMan等工具，当然Arthas提供了一个命令让替代我们来回手动请求。

#### 2.5.1 tt
tt官方介绍: 方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测。可以看见tt可以用于录制请求，当然也支持我们重放。
如果要录制某个方法，可以用下面命令:

```
$ tt -t demo.MathGame primeFactors
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 66 ms.
 INDEX   TIMESTAMP            COST(ms)  IS-RET  IS-EXP   OBJECT         CLASS                          METHOD
-------------------------------------------------------------------------------------------------------------------------------------
 1000    2018-12-04 11:15:38  1.096236  false   true     0x4b67cf4d     MathGame                       primeFactors
 1001    2018-12-04 11:15:39  0.191848  false   true     0x4b67cf4d     MathGame                       primeFactors
 1002    2018-12-04 11:15:40  0.069523  false   true     0x4b67cf4d     MathGame                       primeFactors
 1003    2018-12-04 11:15:41  0.186073  false   true     0x4b67cf4d     MathGame                       primeFactors
 1004    2018-12-04 11:15:42  17.76437  true    false    0x4b67cf4d     MathGame                       primeFactors

```
上面录制了5个调用环境现场，也可以看做是录制了5个请求返回信息。比如我们想选择index为1004个的请求来重放，可以输入下面的命令。

```
$ tt -i 1004 -p
 RE-INDEX       1004
 GMT-REPLAY     2018-12-04 11:26:00
 OBJECT         0x4b67cf4d
 CLASS          demo.MathGame
 METHOD         primeFactors
 PARAMETERS[0]  @Integer[946738738]
 IS-RETURN      true
 IS-EXCEPTION   false
 RETURN-OBJ     @ArrayList[
                    @Integer[2],
                    @Integer[11],
                    @Integer[17],
                    @Integer[2531387],
                ]
Time fragment[1004] successfully replayed.
Affect(row-cnt:1) cost in 14 ms.
```
注意重放请求需要关注两点:
- ThreadLocal 信息丢失:由于使用的是Arthas线程调用，会让threadLocal信息丢失，比如一些TraceId信息可能会丢失

- 引用的对象:保存的入参是保存的引用，而不是拷贝，所以如果参数中的内容被修改，那么入参其实也是被修改的。


## 2.6 一些耗时的方法，经常被触发，如何知道谁调用的？
有时候有些方法非常耗时或者非常重要，需要知道到底是谁发起的调用，比如System.gc(),有时候如果你发现fullgc频繁是因为System.gc()引起的，你需要查看到底是什么应用调用的，那么你就可以使用下面的命令。
#### 2.6.1 
我们可以输入下面的命令:
```
$ options unsafe true
 NAME    BEFORE-VALUE  AFTER-VALUE                                                                                                                                                                        
-----------------------------------                                                                                                                                                                       
 unsafe  false         true                                                                                                                                                                               
$ stack java.lang.System gc
Press Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 50 ms.
ts=2019-01-20 21:14:05;thread_name=main;id=1;is_daemon=false;priority=5;TCCL=sun.misc.Launcher$AppClassLoader@14dad5dc
    @java.lang.System.gc()
        at com.lz.test.Test.main(Test.java:322)

```
首先输入options unsafe true允许我们对jdk增强，然后对System.gc进行进行监视，然后记录当前的堆栈来获取是什么位置进行的调用。


## 2.7 如何重定义某个类?
有些时候我们找了所有的命令，发现和我们的需求并不符合的时候，那么这个时候我们可以重新定义这个类，我们可以用使用下面的命令。

#### 2.7.1 redefine
redefine命令提供了我们可以重新定义jvm中的class，但是使用这个命令之后class不可恢复。我们首先需要把重写的class编译出来，然后上传到我们指定的目录，进行下面的操作:

```
 redefine -p /tmp/Test.class
```
可以重定义我们的Test.class。从而修改逻辑，完成我们自定义的需求。

## 2.8 Arthas小结
上面介绍了7种Arthas比较常见的场景和命令。当然这个命令还远远不止这么点，每个命令的用法也没有局限于我介绍的。尤其是开源以后更多的开发者参与了进来，现在也将其优化成可以有界面的，在线排查问题的方式，来解决去线上安装的各种不便。

更多的命令可以参考Arthas的用户文档:https://alibaba.github.io/arthas/index.html。

# 3.jvm-sandbox
上面已经给大家介绍了强大的Arthas，有很多人也想做一个可以动态替换Class的工具，但是这种东西过于底层，比较小众，入门的门槛相对来说比较高。但是jvm-sandbox，给我们提供了用通俗易懂的编码方式来动态替换Class。

## 3.1 AOP
对于AOP来说大家肯定对其不陌生,在Spring中我们可以很方便的实现一个AOP，但是这样有两个缺点:一个是只能针对Spring中的Bean进行增强，还有个是增强之后如果要修改增强内容那么就只能重写然后发布项目，不能动态的增强。

## 3.2 sanbox能带来什么
JVM Sandbox 利用 HotSwap 技术在不重启 JVM的情况下实现：

- 在运行期完成对 JVM 中任意类里的任意方法的 AOP 增强
- 可以动态热插拔扩展模块
- 通过扩展模块改变任意方法执行的流程

也就是我们可以通过这种技术来完成我们在arthas的命令。
一般来说sandbox的适用场景如下:
- 线上故障定位
- 线上系统流控
- 线上故障模拟
- 方法请求录制和结果回放
- 动态日志打印
- ...

当然还有更多的场景，他能做什么完全取决于你的想象，只要你想得出来他就能做到。

## 3.3 sandbox例子
sandbox提供了Module的概念，每个Module都是一个AOP的实例。
比如我们想完成一个打印所有jdbc statement sql日志的Module，需要建一个下面的Module：

```
public class JdbcLoggerModule implements Module, LoadCompleted {

    private final Logger smLogger = LoggerFactory.getLogger("DEBUG-JDBC-LOGGER");

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void loadCompleted() {
        monitorJavaSqlStatement();
    }

    // 监控java.sql.Statement的所有实现类
    private void monitorJavaSqlStatement() {
        new EventWatchBuilder(moduleEventWatcher)
                .onClass(Statement.class).includeSubClasses()
                .onBehavior("execute*")
                /**/.withParameterTypes(String.class)
                /**/.withParameterTypes(String.class, int.class)
                /**/.withParameterTypes(String.class, int[].class)
                /**/.withParameterTypes(String.class, String[].class)
                .onWatch(new AdviceListener() {

                    private final String MARK_STATEMENT_EXECUTE = "MARK_STATEMENT_EXECUTE";
                    private final String PREFIX = "STMT";

                    @Override
                    public void before(Advice advice) {
                        advice.attach(System.currentTimeMillis(), MARK_STATEMENT_EXECUTE);
                    }

                    @Override
                    public void afterReturning(Advice advice) {
                        if (advice.hasMark(MARK_STATEMENT_EXECUTE)) {
                            final long costMs = System.currentTimeMillis() - (Long) advice.attachment();
                            final String sql = advice.getParameterArray()[0].toString();
                            logSql(PREFIX, sql, costMs, true, null);
                        }
                    }
                    ....
                });
    }

}
```
monitorJavaSqlStatement是我们的核心方法。流程如下:

1. 首先通过new EventWatchBuilder(moduleEventWatcher)构造一个事件观察者的构造器，通过Builder我们可以方便的构造出我们的观察者。
2. onclass是对我们需要观察的类进行筛选，includeSubClasses包含所有的子类。
3. withParameterTypes进一步筛选参数。
4. onWatch进行观察，采取模板模式，和我们Spring的AOP很类似，首先在before里面记录下当前的时间，然后在afterReturning中将before的时间取出来得到当前消耗的时间，然后获取当前的sql语句，最后进行打印。

## 3.4 sandbox小结
Arthas是一款很优秀的Java线上问题诊断工具，Sandbox的作者没有选择和Arthas去做一个功能很全的工具平台，而选择了去做一款底层中台，让更多的人可以很轻松的去实现字节码增强相关的工具。如果说Arthas是一把锋利的剑能斩杀万千敌人，那么jvm-sandbox就是打造一把好剑的模子，等待着大家去打造一把属于自己的绝世好剑。

sadbox介绍得比较少，有兴趣的同学可以去github上自行了解:https://github.com/alibaba/jvm-sandbox

# 4.自己实现字节码动态替换

不论上我们的Arthas还是我们的jvm-sandbox无外乎使用的就是下面几种技术:
- ASM
- Instrumentation(核心)
- VirtualMachine

## 4.1 ASM
对于ASM字节码修改技术可以参考我之前写的几篇文章:
- [字节码也能做有趣的事
](https://mp.weixin.qq.com/s/Nk4lP7723XIbu5AWXaswmw)
- [字节码也能做有趣的事之ASM](https://mp.weixin.qq.com/s/8yvMSwdjPklcJOyZAlDESw)
- [教你用Java字节码做日志脱敏工具](https://mp.weixin.qq.com/s/I-MIkZ2s57ft14Cul9i2IA)
 

对于ASM修改字节码的技术这里就不做多余阐述。
## 4.2 Instrumentation
Instrumentation是JDK1.6用来构建Java代码的类。Instrumentation是在方法中添加字节码来达到收集数据或者改变流程的目的。当然他也提供了一些额外功能，比如获取当前JVM中所有加载的Class等。

#### 4.2.1获取Instrumentation
Java提供了两种方法获取Instrumentation，下面介绍一下这两种:

##### 4.2.1.1 premain 

在启动的时候，会调用preMain方法:

```
public static void premain(String agentArgs, Instrumentation inst) {
    }

```
需要在启动时添加额外命令

```
java -javaagent:jar 文件的位置 [= 传入 premain 的参数 ] 

```
也需要在maven中配置PreMainClass。

在[教你用Java字节码做日志脱敏工具](https://mp.weixin.qq.com/s/I-MIkZ2s57ft14Cul9i2IA)中很详细的介绍了premain
##### 4.2.1.2 agentmain 
premain是Java SE5开始就提供的代理方式，给了开发者诸多惊喜，不过也有些须不变，由于其必须在命令行指定代理jar，并且代理类必须在main方法前启动。因此，要求开发者在应用前就必须确认代理的处理逻辑和参数内容等等，在有些场合下，这是比较困难的。比如正常的生产环境下，一般不会开启代理功能，所有java SE6之后提供了agentmain，用于我们动态的进行修改，而不需要在设置代理。在 JavaSE6文档当中，开发者也许无法在 java.lang.instrument包相关的文档部分看到明确的介绍，更加无法看到具体的应用 agnetmain 的例子。不过，在 Java SE 6 的新特性里面，有一个不太起眼的地方，揭示了 agentmain 的用法。这就是 Java SE 6 当中提供的 Attach API。

Attach API 不是Java的标准API，而是Sun公司提供的一套扩展 API，用来向目标JVM”附着”（Attach）代理工具程序的。有了它，开发者可以方便的监控一个JVM，运行一个外加的代理程序。

在VirtualMachine中提供了attach的接口

## 4.3 实现HotSwap
本文实现的HotSwap的代码均在https://github.com/lzggsimida123/hotswapsample中，下面简单介绍一下:
#### 4.3.1 redefineClasses
redefineClasses允许我们重新替换JVM中的类,我们现在利用它实现一个简单的需求，我们有下面一个类:

```
public class Test1 implements T1 {

    public void sayHello(){
        System.out.println("Test1");
    }
}
```
在sayHello中打印Test1,然后我们在main方法中循环调用sayHello:

```
 public static void main(String[] args) throws Exception {
        Test1 tt = new Test1();
        int max = 20;
        int index = 0;
        while (++index<max){
            Thread.sleep(100L);
        }
    }
```
如果我们不做任何处理，那么肯定打印出20次Test1。如果我们想完成一个需求，这20次打印是交替打印出Test1,Test2,Test3。那么我们可以借助redefineClass。

```
        //获取Test1,Test2,Test3的字节码
        List<byte[]> bytess = getBytesList();
        int index = 0;
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals("Test1")) {
                while (true) {                
                    //根据index获取本次对应的字节码
                    ClassDefinition classDefinition = new ClassDefinition(clazz, getIndexBytes(index, bytess));
                    // redefindeClass Test1
                    inst.redefineClasses(classDefinition);
                    Thread.sleep(100L);
                    index++;
                }
            }
        }
```
可以看见我们获取了三个calss的字节码，在我们根目录下面有，然后调用redefineClasses替换我们对应的字节码,可以看见我们的结果，将Test1,Test2,Test3打印出来。

![](https://user-gold-cdn.xitu.io/2019/1/21/168701c42bb7236d?w=562&h=630&f=png&s=39548)

####  4.3.2 retransformClasses
redefineClasses直接将字节码做了交换，导致原始字节码丢失，局限较大。使用retransformClasses配合我们的Transformer进行转换字节码。同样的我们有下面这个类:

```
public class TestTransformer {

    public void testTrans() {
        System.out.println("testTrans1");
    }
}
```
在testTrans中打印testTrans1,我们有下面一个main方法:

```
 public static void main(String[] args) throws Exception {
        TestTransformer testTransformer = new TestTransformer();
        int max = 20;
        int index = 0;
        while (++index<max){
            testTransformer.testTrans();
            Thread.sleep(100L);
        }
```
如果我们不做任何操作，那么肯定打印的是testTrans1，接下来我们使用retransformClasses：

```
        while (true) {
            try {
                for(Class<?> clazz : inst.getAllLoadedClasses()){
                    if (clazz.getName().equals("TestTransformer")) {
                        inst.retransformClasses(clazz);
                    }
                }
                Thread.sleep(100L);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
```
这里只是将我们对应的类尝试去retransform，但是需要Transformer:

```
//必须设置true，才能进行多次retrans
        inst.addTransformer(new SampleTransformer(), true);
```
上面添加了一个Transformer,如果设置为false，这下次retransform一个类的时候他不会执行，而是直接返回他已经执行完之后的代码。如果设置为true，那么只要有retransform的调用就会执行。

```
public class SampleTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!"TestTransformer".equals(className)){
            //返回Null代表不进行处理
            return null;
        }
        //进行随机输出testTrans + random.nextInt(3)
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new SampleClassVistor(Opcodes.ASM5,classWriter);
        reader.accept(classVisitor,ClassReader.SKIP_DEBUG);
        return classWriter.toByteArray();
        }
    }
}
```
这里的SampleTransFormer使用ASM去对代码进行替换，进行随机输出testTrans + random.nextInt(3)。可以看有下面的结果:

![](https://user-gold-cdn.xitu.io/2019/1/21/168702c77ed4c0b7?w=689&h=567&f=png&s=46543)

上面的代码已经上传至github：https://github.com/lzggsimida123/hotswapsample

# 5.常见的一些问题
1. Q: instrumentation中trans的顺序?我有多个Transformer执行顺序是怎么样的?

A:执行顺序如下:
- 执行不可retransformClasses的Transformer
- 执行不可retransformClasses的native-Transformer
- 执行可以retransformClasses的Transformer
- 执行可以retransformClasses的native-Transformer

在同一级当中，按照添加顺序进行处理。

2. Q: redefineClass和retransClass区别？

A:redefineClass的class不可找回到以前的,不会触发我们的Transformer，retransClass会根据当前的calss然后依次执行Transformer做class替换。

3. Q:什么时候替换？会影响我运行的代码吗？

A:在jdk文档中的解释是，不会影响当前调用，会在本次调用结束以后才会加载我们替换的class。

4. 重新转换类能改哪些地方？

A: 重新转换可以会更改方法体、常量池和属性。重新转换不能添加、删除或重命名字段或方法、更改方法的签名或更改继承。未来版本会取消(java8没有取消)
5. 哪些类字节码不能转换？

A:私有类，比如Integer.TYPE,和数组class。

6.JIT的代码怎么办？

A:清除原来JIT代码，然后重新走解释执行的过程。

7.arthas和jvm-sandbox性能影响？

A:由于添加了部分逻辑，肯定会有影响，并且替换代码的时候需要到SafePoint的时候才能替换，进行STW，如果替换代码过于频繁，那么会频繁执行STW，这个时候会影响性能。
# 总结
今年阿里开源的arthas和jvm-sandbox推动了Java线上诊断工具的发展。大家以后遇到一些难以解决的线上问题，那么arthas肯定是你的首选目标工具之一。当然如果你想要做自己的一些日志收集,Mock平台，故障模拟等公共的组件，jvm-sandbox能够很好的帮助你。同时了解他们的底层原理也能对你在调优或者排查问题的时候起很大的帮助作用。字数有点多，希望大家能学习到有用的知识。


参考文档:
- jdk6官方instrumentation-Api文档:https://docs.oracle.com/javase/6/docs/technotes/guides/instrumentation/index.html
- arthas:https://github.com/alibaba/arthas
- jvm-sandbox:https://github.com/alibaba/jvm-sandbox

如果大家觉得这篇文章对你有帮助，你的关注和转发是对我最大的支持，O(∩_∩)O:

![](https://user-gold-cdn.xitu.io/2018/7/22/164c2ad786c7cfe4?w=500&h=375&f=jpeg&s=215163)


