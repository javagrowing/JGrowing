# 背景
这篇文章最开始再我的群里面有讨论过，当时想写的这篇文章的，但是因为一些时间的关系所以便没有写。最近阅读微信文章的时候发现了一篇零度写的一篇文章《分享一道阿里Java并发面试题》，对于有关Java并发性技术的文章我一般还是挺感兴趣的，于是阅读了一下，整体来说还是挺不错的，但是其中犯了一个验证可见性的问题。由于微信文章回复不方便讨论，于是我便把之前一些和群友的讨论在这里写出来。
# 如何测试可见性问题
因为在群里面我们习惯的有每周一问，也就由我或者群友发现一些由意思的问题然后提问给大家，让大家参与讨论，当时我提出了一个如何测试vlolatile可见性的问题，首先在Effective Java给出了一个测试volatile可见性的例子:

```
import java.util.concurrent.*;  
  
public class Test {  
    private static /*volatile*/ boolean stop = false;  
    public static void main(String[] args) throws Exception {  
        Thread t = new Thread(new Runnable() {  
            public void run() {  
                int i = 0;  
                while (!stop) {  
                    i++;  
//                    System.out.println("hello");  
                }  
            }  
        });  
        t.start();  
  
        Thread.sleep(1000);  
        TimeUnit.SECONDS.sleep(1);  
        System.out.println("Stop Thread");  
        stop = true;  
    }  
}  
```
这里大家可以复制上面的代码，你会发现这里程序永远不会结束，在零度的那篇文章中也给出了一个测试可见性的例子:

```
public class ThreadSafeCache {
    int result;

    public int getResult() {
        return result;
    }

    public synchronized void setResult(int result) {
        this.result = result;
    }

    public static void main(String[] args) {
        ThreadSafeCache threadSafeCache = new ThreadSafeCache();

        for (int i = 0; i < 8; i++) {
            new Thread(() -> {
                int x = 0;
                while (threadSafeCache.getResult() < 100) {
                    x++;
                }
                System.out.println(x);
            }).start();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        threadSafeCache.setResult(200);
    }
}
```
这里大家也可以运行一下这里是不会结束的。

然而这两个例子真的是测试可见性的？我们先不着急下定论，首先我们来看看何为可见性，这里为了防止自己的一些片面之词，查阅了一些资料可以发现可见性的定义总体来说可以定义为:
> 当一个线程修改了共享变量后，其他线程能够立即得知这个修改。

可见性的定义比较简单，那怎么去实现呢？一般来说可见性会通过缓存一致性协议来完成，这里有篇文章讲CPU缓存一致性协议讲得不错:https://www.cnblogs.com/yanlong300/p/8986041.html，我这里直接借用他的图片,
![](https://user-gold-cdn.xitu.io/2019/5/14/16ab5aba4508a7ba?w=876&h=872&f=png&s=115244)

- CPU A 计算完成后发指令需要修改x.
- CPU A 将x设置为M状态（修改状态）并通知缓存了x的CPU B, CPU B将本地cache b中的x设置为I状态(无效状态)
- CPU A 对x进行赋值
- CPU B 发现x是失效的这个时候会进行回刷操作

可以看见我们的一致性协议会有一定的时间延迟，但是我们的可见性的目的是立即读到最新的，所以我们这里会将无效状态通知到其他拥有该缓存数据的CPU缓存中，并且等待确认，我们vlolatile也是采用这种方式达到可见性的，当然更多的细节你可以直接阅读上面推荐的文章。

我们又回到我们的测试用例，可以发现我们的while循环是一个死循环，但是我们的缓存一致性协议是一定时间延迟，虽然这个一定时间并不保证，但是在现代的电脑系统上尤其是你自己的机器上，刷新一个缓存这点小时间还是有的吧。

并且我们验证可见性的时候似乎违背了我们初衷，可见性的定义是立即读到最新的，但是我们却在强调我们的测试程序会出现死循环，那我们不就是验证的是永远都读不到最新的吗？

通过上面的种种论述我们发现我们可见性的验证似乎出了一点问题。

# 推翻验证程序
我们这里只需要一行代码就可以推翻我们上面的验证程序，我们用第二个验证程序:

![](https://user-gold-cdn.xitu.io/2019/5/14/16ab5bac6a90e150?w=813&h=223&f=png&s=34846)

只添加了一句打印我们的结果值,我们的程序却停止了:

![](https://user-gold-cdn.xitu.io/2019/5/14/16ab5bb96198ea69?w=367&h=390&f=png&s=18887)

这个结果证明我们的其他线程是能获取到我们的更新后的结果值的，所以这里一定是有其他原因。

# 真相大白

我们上面添加了一句话，并没有影响我们的逻辑，但是却产生了截然不同的结果，这个到底是怎么回事呢？首先我们能想到的是编译器优化，看看添加代码前和添加代码后，编译器编译之后的代码是什么，由于我们用的是idea直接打开idea的class文件会帮助我们做反编译。

添加代码前:

![](https://user-gold-cdn.xitu.io/2019/5/14/16ab5c0d0eb53d27?w=761&h=295&f=png&s=39845)

添加代码后:

![](https://user-gold-cdn.xitu.io/2019/5/14/16ab5c051d16575b?w=675&h=318&f=png&s=47067)

这里可以看见编译器已经将我们的while循环优化成for循环，在循环内部添加了一个输出语句，这里可以看见逻辑并没有太大的变化，可以看见不是我们的编译器作怪的问题，这种优化代码的问题还有一个元凶那就是JIT，由于我们的循环有很多次肯定会触发JIT编译优化。

由于JIT编译优化有多个层级，这里我们只看最终的C2优化后的汇编代码,看JIT的汇编代码可以利用hsdis+JITWatch查看，这里我只用了hsdis打印在控制台上查看即可。这里需要添加一下JVM启动参数-XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly，
启动之后一大堆汇编代码，为了看这个查询了好多汇编指令终于是把它理顺了。

```
  0x0000000112f81ce8: cmp    $0x64,%r10d
  0x0000000112f81cec: jge    0x0000000112f81cfc  ;*goto
                                                ; - ThreadSafeCache::lambda$main$0@14 (line 29)

  0x0000000112f81cee: inc    %ebx               ; OopMap{rbp=Oop off=80}
                                                ;*goto
                                                ; - ThreadSafeCache::lambda$main$0@14 (line 29)

  0x0000000112f81cf0: test   %eax,-0xb001cf6(%rip)        # 0x0000000107f80000
                                                ;*goto
                                                ; - ThreadSafeCache::lambda$main$0@14 (line 29)
                                                ;   {poll}
  0x0000000112f81cf6: jmp    0x0000000112f81cee
```
上面的这么多行代码都是我们下面:这段代码的翻译:

```
                while (threadSafeCache.getResult() < 100) {
                    x++;
                }
```

解释一下汇编的代码:
- Step 1：比较threadSafeCache.getResult() 和100的大小
- Step 2: threadSafeCache.getResult()如果大于等于100，跳转至0x0000000112f81cfc,也就是循环外的代码。
- Step 3: 如果小于，那么执行x++操作。
- Step 4: 检查安全点checkpoint,这里不是逻辑代码不需要太关注。
- Step 5: 跳转至我们的Step3处。

可以看见我们上面的代码Step3-5之间形成了死循环，其实我们的代码翻译过来可以看作下面的代码：

```
if(threadSafeCache.getResult() < 100){
    while(true){
        x++;
    }
}
```
可以看见我们的整段代码只执行了这一次get逻辑，有可能get的时候我们主线程还没有执行set。
为什么里面加了一段打印之后就不会有这样的效果呢？我的猜测是如果在我们循环内部有对某个变量进行使用，jit会取消这种激进的优化，当然我们的变量如果是`volatile`也会有这样的效果，我们添加`volatile`的jit的汇编代码如下：

![](https://user-gold-cdn.xitu.io/2019/5/14/16ab5d77fc08f894?w=1066&h=264&f=png&s=56044)

可以发现这里没有做激进的优化而是每次都会获取新的值，来进行比较。

# 总结
到最后，我也没有提及，如何去测试可见性，因为这个东西理论上来说无法去测试，因为有一个很重要的一点我们没法确定线程的执行顺序，当然也有确定的方式，那就是加一个同步器，可以是锁，可以是信号量，让我们的读取操作,在我们写操作之后，还有读操作一定是一次，不能使用循环，我尝试着按照这个思路去写：

```
public class Test {

    private static /*volatile*/ boolean stop = false;

    public static void main(String[] args) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(stop);
            }
        });
        t.start();

        Thread.sleep(1000);
        TimeUnit.SECONDS.sleep(1);
        System.out.println("Stop Thread");
        stop = true;
        countDownLatch.countDown();
    }
}
```
上面这个程序没有加`volatile`,那么输出结果是有一定可能是false的但是发现，所有结果是true，其实这种方式没法去测试，因为我们外加了同步器而我们的同步器会带来读写屏障的加入，如果是读屏障那么会告诉处理器在执行任何的加载前，先应用所有已经在失效队列中的失效操作的指令，也就是会执行失效，回刷缓存。

所以验证可见性的确没有一个很好的例子，我们只需要知道如果没有其他保障(读写屏障等)，有可能不能获取到最新的数据，但是其最终会获取到更新的数据，这个也很像我们分布式一致性中的最终一致性。

最后大家也可以看看零度的这篇文章，其中的对内存屏障和happens-before也有一定的讲解。：https://mp.weixin.qq.com/s/i9ES7u5MPWCv1n8jYU_q_w

> 如果大家觉得这篇文章对你有帮助，你的关注和转发是对我最大的支持，O(∩_∩)O:

![](https://user-gold-cdn.xitu.io/2018/7/22/164c2ad786c7cfe4?w=500&h=375&f=jpeg&s=215163)


