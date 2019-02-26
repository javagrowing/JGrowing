
## 1.Future接口
### 1.1 什么是Future?
在jdk的官方的注解中写道

```
A {@code Future} represents the result of an asynchronous
 * computation.  Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation.
```
在上面的注释中我们能知道Future用来代表异步的结果，并且提供了检查计算完成，等待完成，检索结果完成等方法。简而言之就是提供一个异步运算结果的一个建模。它可以让我们把耗时的操作从我们本身的调用线程中释放出来，只需要完成后再进行回调。就好像我们去饭店里面吃饭，不需要你去煮饭，而你这个时候可以做任何事，然后饭煮好后就会回调你去吃。
### 1.2 JDK8以前的Future
在JDK8以前的Future使用比较简单，我们只需要把我们需要用来异步计算的过程封装在Callable或者Runnable中，比如一些很耗时的操作(不能占用我们的调用线程时间的)，然后再将它提交给我们的线程池ExecutorService。代码例子如下：

```
public static void main(String[] args) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }
        });

        doSomethingElse();//在我们异步操作的同时一样可以做其他操作
        try {
            String res = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
```
上面展示了我们的线程可以并发方式调用另一个线程去做我们耗时的操作。当我们必须依赖我们的异步结果的时候我们就可以调用get方法去获得。当我们调用get方法的时候如果我们的任务完成就可以立马返回，但是如果任务没有完成就会阻塞，直到超时为止。

Future底层是怎么实现的呢？
我们首先来到我们ExecutorService的代码中submit方法这里会返回一个Future

```
public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();
        RunnableFuture<T> ftask = newTaskFor(task);
        execute(ftask);
        return ftask;
    }
```
在sumbmit中会对我们的Callable进行包装封装成我们的FutureTask，我们最后的Future其实也是Future的实现类FutureTask，FutureTask实现了Runnable接口所以这里直接调用execute。在FutureTask代码中的run方法代码如下:

```
public void run() {
        if (state != NEW ||
            !UNSAFE.compareAndSwapObject(this, runnerOffset,
                                         null, Thread.currentThread()))
            return;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran)
                    set(result);
            }
        } 
        .......
    }
```
可以看见当我们执行完成之后会set(result)来通知我们的结果完成了。set(result)代码如下:

```
protected void set(V v) {
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            outcome = v;
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state
            finishCompletion();
        }
    }
```
首先用CAS置换状态为完成，以及替换结果，当替换结果完成之后，才会替换为我们的最终状态，这里主要是怕我们设置完COMPLETING状态之后最终值还没有真正的赋值出去，而我们的get就去使用了，所以还会有个最终状态。我们的get()方法的代码如下：

```
public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s <= COMPLETING)
            s = awaitDone(false, 0L);
        return report(s);
    }
```
首先获得当前状态，然后判断状态是否完成，如果没有完成则进入awaitDone循环等待，这也是我们阻塞的代码，然后返回我们的最终结果。
#### 1.2.1缺陷
我们的Future使用很简单，这也导致了如果我们想完成一些复杂的任务可能就比较难。比如下面一些例子:
- 将两个异步计算合成一个异步计算，这两个异步计算互相独立，同时第二个又依赖第一个的结果。
- 当Future集合中某个任务最快结束时，返回结果。
- 等待Future结合中的所有任务都完成。
- 通过编程方式完成一个Future任务的执行。
- 应对Future的完成时间。也就是我们的回调通知。
### 1.3CompletableFuture
CompletableFuture是JDK8提出的一个支持非阻塞的多功能的Future，同样也是实现了Future接口。
#### 1.3.1CompletableFuture基本实现
下面会写一个比较简单的例子：

```
public static void main(String[] args) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        new Thread(()->{
            completableFuture.complete(Thread.currentThread().getName());
        }).start();
        doSomethingelse();//做你想做的其他操作
        
        try {
            System.out.println(completableFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
```
用法上来说和Future有一点不同，我们这里fork了一个新的线程来完成我们的异步操作，在异步操作中我们会设置值，然后在外部做我们其他操作。在complete中会用CAS替换result，然后当我们get如果可以获取到值得时候就可以返回了。
#### 1.3.2错误处理
上面介绍了正常情况下但是当我们在我们异步线程中产生了错误的话就会非常的不幸，错误的异常不会告知给你，会被扼杀在我们的异步线程中，而我们的get方法会被阻塞。

对于我们的CompletableFuture提供了completeException方法可以让我们返回我们异步线程中的异常,代码如下:

```
public static void main(String[] args) {
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        new Thread(()->{
            completableFuture.completeExceptionally(new RuntimeException("error"));
            completableFuture.complete(Thread.currentThread().getName());
        }).start();
//        doSomethingelse();//做你想做的耗时操作

        try {
            System.out.println(completableFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
--------------
输出:
java.util.concurrent.ExecutionException: java.lang.RuntimeException: error
	at java.util.concurrent.CompletableFuture.reportGet(CompletableFuture.java:357)
	at java.util.concurrent.CompletableFuture.get(CompletableFuture.java:1887)
	at futurepackge.jdk8Future.main(jdk8Future.java:19)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:497)
	at com.intellij.rt.execution.application.AppMain.main(AppMain.java:147)
Caused by: java.lang.RuntimeException: error
	at futurepackge.jdk8Future.lambda$main$0(jdk8Future.java:13)
	at futurepackge.jdk8Future$$Lambda$1/1768305536.run(Unknown Source)
	at java.lang.Thread.run(Thread.java:745)
```
在我们新建的异步线程中直接New一个异常抛出，在我们客户端中依然可以获得异常。
#### 1.3.2工厂方法创建CompletableFuture
我们的上面的代码虽然不复杂，但是我们的java8依然对其提供了大量的工厂方法，用这些方法更容易完成整个流程。如下面的例子:

```
public static void main(String[] args) {
        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() ->{
                return Thread.currentThread().getName();
        });
//        doSomethingelse();//做你想做的耗时操作

        try {
            System.out.println(completableFuture.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
---------
输出：
ForkJoinPool.commonPool-worker-1
```
上面的例子通过工厂方法supplyAsync提供了一个Completable，在异步线程中的输出是ForkJoinPool可以看出当我们不指定线程池的时候会使用ForkJoinPool,而我们上面的compelte的操作在我们的run方法中做了，源代码如下:

```
public void run() {
            CompletableFuture<T> d; Supplier<T> f;
            if ((d = dep) != null && (f = fn) != null) {
                dep = null; fn = null;
                if (d.result == null) {
                    try {
                        d.completeValue(f.get());
                    } catch (Throwable ex) {
                        d.completeThrowable(ex);
                    }
                }
                d.postComplete();
            }
        }
```
上面代码中通过d.completeValue(f.get());设置了我们的值。同样的构造方法还有runasync等等。
#### 1.3.3计算结果完成时的处理
当CompletableFuture计算结果完成时,我们需要对结果进行处理，或者当CompletableFuture产生异常的时候需要对异常进行处理。有如下几种方法:

```
public CompletableFuture<T> 	whenComplete(BiConsumer<? super T,? super Throwable> action)
public CompletableFuture<T> 	whenCompleteAsync(BiConsumer<? super T,? super Throwable> action)
public CompletableFuture<T> 	whenCompleteAsync(BiConsumer<? super T,? super Throwable> action, Executor executor)
public CompletableFuture<T>     exceptionally(Function<Throwable,? extends T> fn)
```
上面的四种方法都返回了CompletableFuture，当我们Action执行完毕的时候，future返回的值和我们原始的CompletableFuture的值是一样的。上面以Async结尾的会在新的线程池中执行，上面没有一Async结尾的会在之前的CompletableFuture执行的线程中执行。例子代码如下:

```
public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(jdk8Future::getMoreData);
        Future<Integer> f = future.whenComplete((v, e) -> {
            System.out.println(Thread.currentThread().getName());
            System.out.println(v);
        });
        System.out.println("Main" + Thread.currentThread().getName());
        System.out.println(f.get());
    }
```

exceptionally方法返回一个新的CompletableFuture，当原始的CompletableFuture抛出异常的时候，就会触发这个CompletableFuture的计算，调用function计算值，否则如果原始的CompletableFuture正常计算完后，这个新的CompletableFuture也计算完成，它的值和原始的CompletableFuture的计算的值相同。也就是这个exceptionally方法用来处理异常的情况。
#### 1.3.4计算结果完成时的转换
上面我们讨论了如何计算结果完成时进行的处理，接下来我们讨论如何对计算结果完成时，对结果进行转换。

```
public <U> CompletableFuture<U> 	thenApply(Function<? super T,? extends U> fn)
public <U> CompletableFuture<U> 	thenApplyAsync(Function<? super T,? extends U> fn)
public <U> CompletableFuture<U> 	thenApplyAsync(Function<? super T,? extends U> fn, Executor executor)
```
这里同样也是返回CompletableFuture，但是这个结果会由我们自定义返回去转换他，同样的不以Async结尾的方法由原来的线程计算，以Async结尾的方法由默认的线程池ForkJoinPool.commonPool()或者指定的线程池executor运行。Java的CompletableFuture类总是遵循这样的原则，下面就不一一赘述了。
例子代码如下:

```
public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            return 10;
        });
        CompletableFuture<String> f = future.thenApply(i ->i+1 ).thenApply(i-> String.valueOf(i));
        System.out.println(f.get());
    }
```
上面的最终结果会输出11，我们成功将其用两个thenApply转换为String。
#### 1.3.5计算结果完成时的消费
上面已经讲了结果完成时的处理和转换，他们最后的CompletableFuture都会返回对应的值，这里还会有一个只会对计算结果消费不会返回任何结果的方法。

```
public CompletableFuture<Void> 	thenAccept(Consumer<? super T> action)
public CompletableFuture<Void> 	thenAcceptAsync(Consumer<? super T> action)
public CompletableFuture<Void> 	thenAcceptAsync(Consumer<? super T> action, Executor executor)
```
函数接口为Consumer，就知道了只会对函数进行消费，例子代码如下：

```
public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            return 10;
        });
        future.thenAccept(System.out::println);
    }
```
这个方法用法很简单我就不多说了.Accept家族还有个方法是用来合并结果当两个CompletionStage都正常执行的时候就会执行提供的action，它用来组合另外一个异步的结果。

```
public <U> CompletableFuture<Void> 	thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action)
public <U> CompletableFuture<Void> 	thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action)
public <U> CompletableFuture<Void> 	thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T,? super U> action, Executor executor)
public     CompletableFuture<Void> 	runAfterBoth(CompletionStage<?> other,  Runnable action)
```

runAfterBoth是当两个CompletionStage都正常完成计算的时候,执行一个Runnable，这个Runnable并不使用计算的结果。
示例代码如下：

```
public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            return 10;
        });
        System.out.println(future.thenAcceptBoth(CompletableFuture.supplyAsync(() -> {
            return 20;
        }),(x,y) -> System.out.println(x+y)).get());
    }
```
CompletableFuture也提供了执行Runnable的办法，这里我们就不能使用我们future中的值了。

```
public CompletableFuture<Void> 	thenRun(Runnable action)
public CompletableFuture<Void> 	thenRunAsync(Runnable action)
public CompletableFuture<Void> 	thenRunAsync(Runnable action, Executor executor)
```
#### 1.3.6对计算结果的组合
首先是介绍一下连接两个future的方法:

```
public <U> CompletableFuture<U> 	thenCompose(Function<? super T,? extends CompletionStage<U>> fn)
public <U> CompletableFuture<U> 	thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn)
public <U> CompletableFuture<U> 	thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn, Executor executor)
```
对于Compose可以连接两个CompletableFuture，其内部处理逻辑是当第一个CompletableFuture处理没有完成时会合并成一个CompletableFuture,如果处理完成，第二个future会紧接上一个CompletableFuture进行处理。

```
public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            return 10;
        });
        System.out.println(future.thenCompose(i -> CompletableFuture.supplyAsync(() -> { return i+1;})).get());
    }
```
我们上面的thenAcceptBoth讲了合并两个future,但是没有返回值这里将介绍一个有返回值的方法，如下：

```
public <U,V> CompletableFuture<V> 	thenCombine(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn)
public <U,V> CompletableFuture<V> 	thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn)
public <U,V> CompletableFuture<V> 	thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T,? super U,? extends V> fn, Executor executor)
```
例子比较简单如下：

```
public static void main(String[] args) throws Exception {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            return 10;
        });
        CompletableFuture<String> f = future.thenCombine(CompletableFuture.supplyAsync(() -> {
            return 20;
        }),(x,y) -> {return "计算结果："+x+y;});
        System.out.println(f.get());
    }
```
上面介绍了两个future完成的时候应该完成的工作，接下来介绍任意一个future完成时需要执行的工作，方法如下：

```
public CompletableFuture<Void> 	acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action)
public CompletableFuture<Void> 	acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action)
public CompletableFuture<Void> 	acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor)
public <U> CompletableFuture<U> 	applyToEither(CompletionStage<? extends T> other, Function<? super T,U> fn)
public <U> CompletableFuture<U> 	applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T,U> fn)
public <U> CompletableFuture<U> 	applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T,U> fn, Executor executor)
```
上面两个是一个是纯消费不返回结果，一个是计算后返回结果。
#### 1.3.6其他方法

```
public static CompletableFuture<Void> 	    allOf(CompletableFuture<?>... cfs)
public static CompletableFuture<Object> 	anyOf(CompletableFuture<?>... cfs)
```
allOf方法是当所有的CompletableFuture都执行完后执行计算。

anyOf方法是当任意一个CompletableFuture执行完后就会执行计算，计算的结果相同。
#### 1.3.7建议
CompletableFuture和Java8的Stream搭配使用对于一些并行访问的耗时操作有很大的性能提高，可以自行了解。
