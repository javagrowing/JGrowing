# Singleton(单例模式)

## 饿汉单例

在类加载时就初始化单例。

- [HungerSingleton](src/main/java/org/xuyuji/pattern/singleton/HungerSingleton.java)
- [HungerStaticSingleton](src/main/java/org/xuyuji/pattern/singleton/HungerStaticSingleton.java)

## 懒汉单例

饿汉模式实现简单方便，但是会造成内存浪费，即使没有使用也会初始化。

于是自然就有了懒加载的懒汉模式，在使用时再实例化。

- [LazySingleton](src/main/java/org/xuyuji/pattern/singleton/LazySingleton.java)

  普通的懒加载，不考虑线程安全问题。

  用于验证。见[ConcurrentTest.testDestroyLazySingleton()](src/test/java/org/xuyuji/pattern/singleton/ConcurrentTest.java)

- [LazyThreadSafeSingleton](src/main/java/org/xuyuji/pattern/singleton/LazyThreadSafeSingleton.java)

  在LazySingleton基础上增加synchronized、volatile，保证线程安全。

  PS：创建类在底层有三步：1、分配地址 2、初始化 3、返回地址

  ​	编译期间可能因为指令重排导致2、3顺序颠倒，会出现对象引用不为null，但是内部没有初始化完成的情况。

  ​	故加上volatile关键字禁止指令重排。

- [DoubleLockingSingleton](src/main/java/org/xuyuji/pattern/singleton/DoubleLockingSingleton.java)

  LazyThreadSafeSingleton每次获取单例都需要加锁，在初始化后这一处理没意义，平白增加资源消耗。

  因为增加双重判断加锁，外层判断单例是否已经初始化，内层判断保证初始化时的线程安全问题。

  性能比LazyThreadSafeSingleton高。

- [InnerClassSingleton](src/main/java/org/xuyuji/pattern/singleton/InnerClassSingleton.java)

  利用内部类初始化单例，这一过程由虚拟机来保证线程安全问题。

PS：饿汉模式因为是类初始化过程

## 反射破坏

虽然将构造方法设置为private可以避免类被new出来，但是通过反射机制还是能够访问到(`setAccessible(true)`)。

 为了防止被错误调用，需要在构造方法中加入校验。

 ```java
 private HungerStaticSingleton() {
 	if (INSTANCE != null) {
 		throw new RuntimeException("非法操作");
 	}
 }
 ```

 验证见[ReflectionTest](src/test/java/org/xuyuji/pattern/singleton/ReflectionTest.java)

## 序列化破坏

除了反射破坏，序列化也能破坏单例，将类序列化写出，然后再重新载入，这又会重新创建出一个类实例来。

解决办法就是接下来的`可序列化单例`和`枚举单例`

验证见[SerializableTest.testDestroyHungerStaticSingleton()](src/test/java/org/xuyuji/pattern/singleton/SerializableTest.java)

## 可序列化单例

实现一个方法即可

```java
private Object readResolve() {
	return instance;
}
```

实现见[SerializableSingleton](src/main/java/org/xuyuji/pattern/singleton/SerializableSingleton.java)

为什么？

ObjectInputStream.readObject()

​    ObjectInputStream.readObject0(false)

​        ObjectInputStream.readOrdinaryObject(false)

```java
if (obj != null && handles.lookupException(passHandle) == null && desc.hasReadResolveMethod())
{
    Object rep = desc.invokeReadResolve(obj);
    if (unshared && rep.getClass().isArray()) {
        rep = cloneArray(rep);
    }
    if (rep != obj) {
        // Filter the replacement object
        if (rep != null) {
            if (rep.getClass().isArray()) {
                filterCheck(rep.getClass(), Array.getLength(rep));
            } else {
                filterCheck(rep.getClass(), -1);
            }
        }
        handles.setObject(passHandle, obj = rep);
    }
}

return obj;
```

ObjectStreamClass.java

```java
boolean hasReadResolveMethod() {
	requireInitialized();
	return (readResolveMethod != null);
}

Object invokeReadResolve(Object obj){
    ...
    return readResolveMethod.invoke(obj, (Object[]) null); 
    ...
}

private ObjectStreamClass(final Class<?> cl) {
    ...
    readResolveMethod = getInheritableMethod(cl, "readResolve", null, Object.class);
    ...
}
```

从上面这些代码片段可以看出，当序列化对象有readResolve方法时，会调用这个方法然后替换掉obj。

验证见[SerializableTest.testDestroySerializableSingleton()](src/test/java/org/xuyuji/pattern/singleton/SerializableTest.java)

## 枚举单例

利用枚举来实现单例，见[EnumSingleton](src/main/java/org/xuyuji/pattern/singleton/EnumSingleton.java)。

枚举依靠JDK保证了无法通过反射调用构造方法、无法通过反序列化创建新类。

- **枚举不能通过反射调用构造方法**

  `jad EnumSingleton.class`得到EnumSingleton.jad

  ```java
  public final class EnumSingleton extends Enum
  {
  
      private EnumSingleton(String s, int i)
      {
          super(s, i);
      }
  
      public static EnumSingleton[] values()
      {
          EnumSingleton aenumsingleton[];
          int i;
          EnumSingleton aenumsingleton1[];
          System.arraycopy(aenumsingleton = ENUM$VALUES, 0, aenumsingleton1 = new EnumSingleton[i = aenumsingleton.length], 0, i);
          return aenumsingleton1;
      }
  
      public static EnumSingleton valueOf(String s)
      {
          return (EnumSingleton)Enum.valueOf(org/xuyuji/pattern/singleton/EnumSingleton, s);
      }
  
      public static final EnumSingleton INSTANCE;
      private static final EnumSingleton ENUM$VALUES[];
  
      static 
      {
          INSTANCE = new EnumSingleton("INSTANCE", 0);
          ENUM$VALUES = (new EnumSingleton[] {
              INSTANCE
          });
      }
  }
  ```

  可以看到，没有无参构造器，反射调用无参构造器只会抛出异常`java.lang.NoSuchMethodException: org.xuyuji.pattern.singleton.EnumSingleton.<init>()`。

  验证见[ReflectionTest.testDestroyEnumSingleton()](src/test/java/org/xuyuji/pattern/singleton/ReflectionTest.java)。

  那么再试试反射调用EnumSingleton(String s, int i)，依然抛出异常:`java.lang.IllegalArgumentException: Cannot reflectively create enum objects`。

  验证见[ReflectionTest.testDestroyEnumSingleton2()](src/test/java/org/xuyuji/pattern/singleton/ReflectionTest.java)。

  这是为什么？

  进newInstance方法看下

  ```java
  if ((clazz.getModifiers() & Modifier.ENUM) != 0)
              throw new IllegalArgumentException("Cannot reflectively create enum objects");
  ```

  这里的clazz.getModifiers()返回值为16401=16384(enum)+16(final)+1(public)，Modifier.ENUM=16384。

  clazz.getModifiers()和Modifier.ENUM做与运算，当modifier中包含有enum时，运算结果就是非零，那么这段逻辑判断就是判断是否是枚举类，也即是只要是枚举类就不允许调用newInstance方法。

- **枚举无法通过反序列化创建新类**

  枚举类反序列化回来依然是单例，验证见[SerializableTest.testDestroyEnumSingleton()](src/test/java/org/xuyuji/pattern/singleton/SerializableTest.java)。

  为什么？

  ObjectInputStream.readObject()

  ​    ObjectInputStream.readObject0(false)

  ​        ObjectInputStream.readEnum(false)

  ```java
  ......
  Enum<?> result = null;
  Class<?> cl = desc.forClass();
  if (cl != null) {
      try {
          @SuppressWarnings("unchecked")
          Enum<?> en = Enum.valueOf((Class)cl, name);
          result = en;
      } catch (IllegalArgumentException ex) {
          throw (IOException) new InvalidObjectException(
              "enum constant " + name + " does not exist in " +
              cl).initCause(ex);
      }
      if (!unshared) {
          handles.setObject(enumHandle, result);
      }
  }
  
  handles.finish(enumHandle);
  passHandle = enumHandle;
  return result;
  ```

  可以看到，直接调用了Enum.valueOf(Class<T> enumType, String name)方法

  Enum.valueOf

  ```java
  public static <T extends Enum<T>> T valueOf(Class<T> enumType,String name) {
      T result = enumType.enumConstantDirectory().get(name);
      if (result != null)
          return result;
      if (name == null)
          throw new NullPointerException("Name is null");
      throw new IllegalArgumentException("No enum constant " + enumType.getCanonicalName() + "." + name);
  }
  ```

  Class.enumConstantDirectory

  ```java
  Map<String, T> enumConstantDirectory() {
      if (enumConstantDirectory == null) {
          T[] universe = getEnumConstantsShared();
          if (universe == null)
              throw new IllegalArgumentException(getName() + " is not an enum type");
          Map<String, T> m = new HashMap<>(2 * universe.length);
          for (T constant : universe)
              m.put(((Enum<?>)constant).name(), constant);
          enumConstantDirectory = m;
      }
      return enumConstantDirectory;
  }
  
  T[] getEnumConstantsShared() {
      if (enumConstants == null) {
          if (!isEnum()) return null;
          try {
              final Method values = getMethod("values");
              java.security.AccessController.doPrivileged(
                  new java.security.PrivilegedAction<Void>() {
                      public Void run() {
                          values.setAccessible(true);
                          return null;
                      }
                  });
              @SuppressWarnings("unchecked")
              T[] temporaryConstants = (T[])values.invoke(null);
              enumConstants = temporaryConstants;
          }
          // These can happen when users concoct enum-like classes
          // that don't comply with the enum spec.
          catch (InvocationTargetException | NoSuchMethodException |
                 IllegalAccessException ex) { return null; }
      }
      return enumConstants;
  }
  ```

  又回到枚举类的values方法了，看看之前反编译的代码

  ```java
  public static EnumSingleton[] values()
  {
  	EnumSingleton aenumsingleton[];
  	int i;
  	EnumSingleton aenumsingleton1[];
  	System.arraycopy(aenumsingleton = ENUM$VALUES, 0, aenumsingleton1 = new EnumSingleton[i = aenumsingleton.length], 0, i);
  	return aenumsingleton1;
  }
  
  public static final EnumSingleton INSTANCE;
  private static final EnumSingleton ENUM$VALUES[];
  
  static 
  {
      INSTANCE = new EnumSingleton("INSTANCE", 0);
      ENUM$VALUES = (new EnumSingleton[] {
          INSTANCE
      });
  }
  ```

  values()方法返回的是一份ENUM$VALUES的拷贝，而ENUM$VALUES就是我们的枚举值数组。

  再回到Class.enumConstantDirectory将枚举值数组转化成哈希表，然后Enum.valueOf里的get(name)从哈希表中获取枚举实例。

  如此反序列化得到的类实例就是虚拟机中旧枚举类实例。

## 容器单例

通过哈希表记录一批实例，统一管理。

spring的模式，只是相对于例子更复杂，管理的是BeanDefinition。

[ContainerSingleton](src/main/java/org/xuyuji/pattern/singleton/ContainerSingleton.java)

## ThreadLocal单例

利用ThreadLocal初始化方法实例化单例，保证各线程内单例唯一。

[ThreadLocalSingleton](src/main/java/org/xuyuji/pattern/singleton/ThreadLocalSingleton.java)

验证见[ConcurrentTest.testThreadLocalSingleton()](src/test/java/org/xuyuji/pattern/singleton/ConcurrentTest.java)