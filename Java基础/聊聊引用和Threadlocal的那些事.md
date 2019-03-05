# 1 背景
某一天在某一个群里面的某个群友突然提出了一个问题:"threadlocal的key是弱引用，那么在threadlocal.get()的时候,发生GC之后，key是否是null？"屏幕前的你可以好好的想想这个问题，在这里我先卖个关子，先讲讲Java中引用和ThreadLocal的那些事。
# 2 Java中的引用
对于很多Java初学者来说，会把引用和对象给搞混淆。下面有一段代码，

```
User zhangsan = new User("zhangsan", 24);
```
这里先提个问题zhangsan到底是引用还是对象呢？很多人会认为zhangsan是个对象，如果你也是这样认为的话那么再看一下下面一段代码

```
User zhangsan;
zhangsan = new User("zhangsan", 24);
```
这段代码和开始的代码其实执行效果是一致的，这段代码的第一行User zhangsan，定义了zhangsan，那你认为zhangsan还是对象吗？如果你还认为的话，那么这个对象应该是什么呢？的确,zhangsan其实只是一个引用，对JVM内存划分熟悉的同学应该熟悉下面的图片:

![](https://user-gold-cdn.xitu.io/2019/2/28/1693469ee918d76f?w=892&h=506&f=png&s=28644)
其实zhangsan是栈中分配的一个引用，而new User("zhangsan", 24)是在堆中分配的一个对象。而'='的作用是用来将引用指向堆中的对象的。就像你叫张三但张三是个名字而已并不是一个实际的人，他只是指向的你。

我们一般所说的引用其实都是代指的强引用，在JDK1.2之后引用不止这一种，一般来说分为四种:强引用，软引用，弱引用，虚引用。而接下来我会一一介绍这四种引用。
## 2.1 强引用
上面我们说过了
User zhangsan = new User("zhangsan", 24);这种就是强引用，有点类似C的指针。对强引用他的特点有下面几个:
- 强引用可以直接访问目标对象。
- 只要这个对象被强引用所关联，那么垃圾回收器都不会回收，那怕是抛出OOM异常。
- 容易导致内存泄漏。

## 2.2 软引用
在Java中使用SoftReference帮助我们定义软引用。其构造方法有两个:
```
public SoftReference(T referent);
public SoftReference(T referent, ReferenceQueue<? super T> q);
```
两个构造方法相似，第二个比第一个多了一个引用队列，在构造方法中的第一个参数就是我们的实际被指向的对象，这里用新建一个SoftReference来替代我们上面强引用的等号。
下面是构造软引用的例子:
```
 softZhangsan = new SoftReference(new User("zhangsan", 24));
```
#### 2.2.1软引用有什么用？
如果某个对象他只被软引用所指向，那么他将会在内存要溢出的时候被回收，也就是当我们要出现OOM的时候，如果回收了一波内存还不够，这才抛出OOM,弱引用回收的时候如果设置了引用队列，那么这个软引用还会进一次引用队列，但是引用所指向的对象已经被回收。这里要和下面的弱引用区分开来，弱引用是只要有垃圾回收，那么他所指向的对象就会被回收。下面是一个代码例子:

```
public static void main(String[] args) {
        ReferenceQueue<User> referenceQueue = new ReferenceQueue();
        SoftReference softReference = new SoftReference(new User("zhangsan",24), referenceQueue);
        //手动触发GC
        System.gc();
        Thread.sleep(1000);
        System.out.println("手动触发GC:" + softReference.get());
        System.out.println("手动触发的队列:" + referenceQueue.poll());
        //通过堆内存不足触发GC
        makeHeapNotEnough();
        System.out.println("通过堆内存不足触发GC:" + softReference.get());
        System.out.println("通过堆内存不足触发GC:" + referenceQueue.poll());
    }

    private static void makeHeapNotEnough() {
        SoftReference softReference = new SoftReference(new byte[1024*1024*5]);
        byte[] bytes = new byte[1024*1024*5];
    }
    输出:
    手动触发GC:User{name='zhangsan', age=24}
    手动触发的队列:null
    通过堆内存不足触发GC:null
    通过堆内存不足触发GC:java.lang.ref.SoftReference@4b85612c
```
通过-Xmx10m设置我们堆内存大小为10，方便构造堆内存不足的情况。可以看见我们输出的情况我们手动调用System.gc并没有回收我们的软引用所指向的对象，只有在内存不足的情况下才能触发。
#### 2.2.2软引用的应用
在SoftReference的doc中有这么一句话:
> Soft references are most often used to implement memory-sensitive caches

也就是说软引用经常用来实现内存敏感的高速缓存。怎么理解这句话呢？我们知道软引用他只会在内存不足的时候才触发，不会像强引用那用容易内存溢出，我们可以用其实现高速缓存，一方面内存不足的时候可以回收，一方面也不会频繁回收。在高速本地缓存Caffeine中实现了软引用的缓存，当需要缓存淘汰的时候，如果是只有软引用指向那么久会被回收。不熟悉Caffeine的同学可以阅读[深入理解Caffeine](https://mp.weixin.qq.com/s/BH6vcUgI8na7iLaF0RGrSg)

## 2.3 弱引用
弱引用在Java中使用WeakReference来定义一个弱引用，上面我们说过他比软引用更加弱，只要发生垃圾回收，若这个对象只被弱引用指向，那么就会被回收。这里我们就不多废话了，直接上例子:
```
public static void main(String[] args)  {
        WeakReference weakReference = new WeakReference(new User("zhangsan",24));
        System.gc();
        System.out.println("手动触发GC:" + weakReference.get());
    }
输出结果:
手动触发GC:null
```
可以看见上面的例子只要垃圾回收一触发，该对象就被回收了。

#### 2.3.1 弱引用的作用
在WeakReference的注释中写到:
>Weak references are most often used to implement canonicalizing mappings.

从中可以知道弱引用更多的是用来实现canonicalizing mappings(规范化映射)。在JDK中WeakHashMap很好的体现了这个例子:

```
public static void main(String[] args) throws Exception {
        WeakHashMap<User, String> weakHashMap = new WeakHashMap();
        //强引用
        User zhangsan = new User("zhangsan", 24);
        weakHashMap.put(zhangsan, "zhangsan");
        System.out.println("有强引用的时候:map大小" + weakHashMap.size());
        //去掉强引用
        zhangsan = null;
        System.gc();
        Thread.sleep(1000);
        System.out.println("无强引用的时候:map大小"+weakHashMap.size());
    }
输出结果为:
有强引用的时候:map大小1
无强引用的时候:map大小0
```
可以看出在GC之后我们在map中的键值对就被回收了，在weakHashMap中其实只有Key是弱引用做关联的，然后通过引用队列再去对我们的map进行回收处理。

## 2.4 虚引用
虚引用是最弱的引用，在Java中使用PhantomReference进行定义。弱到什么地步呢？也就是你定义了虚引用根本无法通过虚引用获取到这个对象，更别谈影响这个对象的生命周期了。在虚引用中唯一的作用就是用队列接收对象即将死亡的通知。

```
    public static void main(String[] args) throws Exception {
        ReferenceQueue referenceQueue = new ReferenceQueue();
        PhantomReference phantomReference = new PhantomReference(new User("zhangsan", 24), referenceQueue);
        System.out.println("什么也不做，获取:" + phantomReference.get());
    }
输出结果:
什么也不做，获取:null
```
在PhantomReference的注释中写到:
>Phantom references are most often used for scheduling pre-mortem cleanup actions in a more flexible way than is possible with the Java finalization mechanism.

虚引用得最多的就是在对象死前所做的清理操作，这是一个比Java的finalization梗灵活的机制。
在DirectByteBuffer中使用Cleaner用来回收对外内存，Cleaner是PhantomReference的子类，当DirectByteBuffer被回收的时候未防止内存泄漏所以通过这种方式进行回收，有点类似于下面的代码:

```
public static void main(String[] args) throws Exception {
        Cleaner.create(new User("zhangsan", 24), () -> {System.out.println("我被回收了，当前线程:{}"+ Thread.currentThread().getName());});
        System.gc();
        Thread.sleep(1000);
    }
输出:
我被回收了，当前线程:Reference Handler
```

# 3 ThreadLocal
ThreadLocal是一个本地线程副本变量工具类,基本在我们的代码中随处可见。这里就不过多的介绍他了。
## 3.1 ThreadLocal和弱引用的那些事
上面说了这么多关于引用的事，这里终于回到了主题了我们的ThreadLocal和弱引用有什么关系呢？

在我们的Thread类中有下面这个变量:

```
ThreadLocal.ThreadLocalMap threadLocals
```
ThreadLocalMap本质上也是个Map,其中Key是我们的ThreadLocal这个对象，Value就是我们在ThreadLocal中保存的值。也就是说我们的ThreadLocal保存和取对象都是通过Thread中的ThreadLocalMap来操作的，而key就是本身。在ThreadLocalMap中Entry有如下定义:

```
 static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }
```
可以看见Entry是WeakReference的子类，而这个弱引用所关联的对象正是我们的ThreadLocal这个对象。我们又回到上面的问题:
>"threadlocal的key是弱引用，那么在threadlocal.get()的时候,发生GC之后，key是否是null？"

这个问题晃眼一看，弱引用嘛，还有垃圾回收那肯定是为null，这其实是不对的，因为题目说的是在做threadlocal.get()操作，证明其实还是有强引用存在的。所以key并不为null。如果我们的强引用不存在的话，那么Key就会被回收，也就是会出现我们value没被回收，key被回收，导致value永远存在，出现内存泄漏。这也是ThreadLocal经常会被很多书籍提醒到需要remove()的原因。

你也许会问看到很多源码的ThreadLocal并没有写remove依然再用得很好呢？那其实是因为很多源码经常是作为静态变量存在的生命周期和Class是一样的，而remove需要再那些方法或者对象里面使用ThreadLocal，因为方法栈或者对象的销毁从而强引用丢失，导致内存泄漏。

## 3.2 FastThreadLocal
FastThreadLocal是Netty中提供的高性能本地线程副本变量工具。在Netty的io.netty.util中提供了很多牛逼的工具，后续会一一给大家介绍，这里就先说下FastThreadLocal。

FastThreadLocal有下面几个特点:
- 使用数组代替ThreadLocalMap存储数据，从而获取更快的性能。(缓存行和一次定位，不会有hash冲突)
- 由于使用数组，不会出现Key回收，value没被回收的尴尬局面，所以避免了内存泄漏。

# 总结
文章开头的问题，为什么会被问出来，其实是对弱引用和ThreadLocal理解不深导致，很多时候只记着一个如果是弱引用，在垃圾回收时就会被回收，就会导致把这个观念先入为主，没有做更多的分析思考。所以大家再分析一个问题的时候还是需要更多的站在不同的场景上做更多的思考。

最后这篇文章被我收录于JGrowing-Java基础篇，一个全面，优秀，由社区一起共建的Java学习路线，如果您想参与开源项目的维护，可以一起共建，github地址为:https://github.com/javagrowing/JGrowing 
麻烦给个小星星哟。

> 如果大家觉得这篇文章对你有帮助，你的关注和转发是对我最大的支持，O(∩_∩)O:

![](https://user-gold-cdn.xitu.io/2018/7/22/164c2ad786c7cfe4?w=500&h=375&f=jpeg&s=215163)
