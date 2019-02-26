1.什么是SimpleDateFormat
==========================================================================================================================================================

在java doc对SimpleDateFormat的解释如下:

 <code>SimpleDateFormat</code> is a concrete class for formatting and parsing dates in a locale-sensitive manner. It allows for formatting
 (date &rarr; text), parsing (text &rarr; date), and normalization.

SimpleDateFormat是一个用来对位置敏感的格式化和解析日期的实体类。他允许把日期格式化成text，把text解析成日期和规范化。

1.1使用SimpleDateFormat
---------------------

simpleDateFormat的使用方法比较简单:


```
public static void main(String[] args) throws Exception {

 SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd  HH:mm:ss");

 System.out.println(simpleDateFormat.format(new Date()));

 System.out.println(simpleDateFormat.parse("2018-07-09  11:10:21"));
 }
```
1.首先需要定义一个日期的pattern,这里我们定义的是"yyyy-mm-dd HH:mm:ss" ，也就是我们这个simpleDateFormat不管是格式化还是解析都需要按照这个pattern。

2.对于format需要传递Date的对象，会返回一个String类型，这个String会按照我们上面的格式生成。

3.对于parse需要传递一个按照上面pattern的字符串，如果传递错误的pattern会抛出java.text.ParseException异常，如果传递正确的会生成一个Date对象。

 
```
附：格式占位符

 G 年代标志符

 y 年

 M 月

 d 日

 h 时 在上午或下午 (1~12)

 H 时 在一天中 (0~23)

 m 分

 s 秒

 S 毫秒

 E 星期

 D 一年中的第几天

 F 一月中第几个星期几

 w 一年中第几个星期

 W 一月中第几个星期

 a 上午 / 下午 标记符

 k 时 在一天中 (1~24)

 K 时 在上午或下午 (0~11)

 z 时区
```


  

2.SimpleDateFormat的隐患
=====================

很多初学者，或者一些经验比较浅的java开发工程师，用SimpleDateFormat会出现一些奇奇怪怪的BUG。

1.结果值不对：转换的结果值经常会出人意料，和预期不同，往往让很多人摸不着头脑。

2.内存泄漏: 由于转换的结果值不对，后续的一些操作，如一个循环，累加一天处理一个东西，但是生成的日期如果异常导致很大的话，会让这个循环变成一个类似死循环一样导致系统内存泄漏，频繁触发GC，造成系统不可用。

为什么会出现这么多问题呢？因为SimpleDateFormat线程不安全，很多人都会写个Util类，然后把SimpleDateFormat定义成全局的一个常量，所有线程都共享这个常量:


```
protected static final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

public static Date formatDate(String date) throws ParseException {

 return dayFormat.parse(date);

}
```


为什么SimpleDateFormat会线程不安全呢，在SimpleDateFormat源码中,所有的格式化和解析都需要通过一个中间对象进行转换，那就是Calendar，而这个也是我们出现线程不安全的罪魁祸首，试想一下当我们有多个线程操作同一个Calendar的时候后来的线程会覆盖先来线程的数据，那最后其实返回的是后来线程的数据，这样就导致我们上面所述的BUG的产生：

/
```
/ Called from Format after creating a FieldDelegate

 private StringBuffer format(Date date, StringBuffer toAppendTo,

 FieldDelegate delegate) {

 // Convert input date to time field list

 calendar.setTime(date);

​

 boolean useDateFormatSymbols = useDateFormatSymbols();

​

 for (int i = 0; i < compiledPattern.length; ) {

 int tag = compiledPattern\[i\] >>> 8;

 int count = compiledPattern\[i++\] & 0xff;

 if (count == 255) {

 count = compiledPattern\[i++\] << 16;

 count |= compiledPattern\[i++\];

 }

​

 switch (tag) {

 case TAG\_QUOTE\_ASCII_CHAR:

 toAppendTo.append((char)count);

 break;

​

 case TAG\_QUOTE\_CHARS:

 toAppendTo.append(compiledPattern, i, count);

 i += count;

 break;

​

 default:

 subFormat(tag, count, delegate, toAppendTo, useDateFormatSymbols);

 break;

 }

 }

 return toAppendTo;

 }
```

![](https://user-gold-cdn.xitu.io/2018/7/18/164ada6def13ed96?w=1150&h=1218&f=png&s=614095)

3.如何避坑
======

对于SimpleDateFormat的解决方法有下面几种:

3.1新建SimpleDateFormat
---------------------

上面出现Bug的原因是因为所有线程都共用一个SimpleDateFormat，这里有个比较好解决的办法，每次使用的时候都创建一个新的SimpleDateFormat,我们可以在DateUtils中将创建SimpleDateFormat放在方法内部:

```
public static Date formatDate(String date) throws ParseException {

 SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");

 return dayFormat.parse(date);

}
```


上面这个方法虽然能解决我们的问题但是引入了另外一个问题就是，如果这个方法使用量比较大，有可能会频繁造成Young gc，整个系统还是会受一定的影响。

3.2使用ThreadLocal
----------------

使用ThreadLocal能避免上面频繁的造成Young gc，我们对每个线程都使用ThreadLocal进行保存，由于ThreadLocal是线程之间隔离开的，所以不会出现线程安全问题:


```
private static ThreadLocal<SimpleDateFormat> simpleDateFormatThreadLocal = new ThreadLocal<>();

 public static Date formatDate(String date) throws ParseException {

 SimpleDateFormat dayFormat = getSimpleDateFormat();

 return dayFormat.parse(date);

 }

​

 private static SimpleDateFormat getSimpleDateFormat() {

 SimpleDateFormat simpleDateFormat = simpleDateFormatThreadLocal.get();

 if (simpleDateFormat == null){

 simpleDateFormat = new SimpleDateFormat("yyyy-mm-dd  HH:mm:ss")

 simpleDateFormatThreadLocal.set(simpleDateFormat);

 }

 return simpleDateFormat;

 }
```


3.3使用第三方工具包
-----------

虽然上面的ThreadLocal能解决我们出现的问题，但是第三方工具包提供的功能更加强大,在java中有两个类库比较出名一个是Joda-Time，一个是Apache common包

### 3.3.1 Joda-Time(推荐)

Joda-Time 令时间和日期值变得易于管理、操作和理解。对于我们复杂的操作都可以使用Joda-Time操作，下面我列举两个例子,对于把日期加上90天，如果使用原生的Jdk我们需要这样写:


```
Calendar calendar = Calendar.getInstance();

calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);

SimpleDateFormat sdf =

 new SimpleDateFormat("E MM/dd/yyyy HH:mm:ss.SSS");

calendar.add(Calendar.DAY\_OF\_MONTH, 90);

System.out.println(sdf.format(calendar.getTime()));
```


但是在我们的joda-time中只需要两句话，并且api也比较通俗易懂，所以你为什么不用Joda-Time呢？


```
DateTime dateTime = new DateTime(2000, 1, 1, 0, 0, 0, 0);

System.out.println(dateTime.plusDays(90).toString("E MM/dd/yyyy HH:mm:ss.SSS");
```


### 3.3.2 common-lang包

在common-lang包中有个类叫FastDateFormat，由于common-lang这个包基本被很多Java项目都会引用，所以你可以不用专门去引用处理时间包，即可处理时间，在FastDateFormat中每次处理时间的时候会创建一个calendar,使用方法比较简单代码如下所示:

FastDateFormat.getInstance().format(new Date());

3.4升级jdk8(推荐)
-------------

在java8中Date这个类中的很多方法包括构造方法都被打上了@Deprecated废弃的注解，取而代之的是LocalDateTime,LocalDate LocalTime这三个类：

*   _LocalDate无法包含时间；_
    

*   _LocalTime无法包含日期；_
    

*   _LocalDateTime才能同时包含日期和时间。_
    

如果你是Java8，那你一定要使用他，在日期的格式化和解析方面不用考虑线程安全性，代码如下:


```
public static String formatTime(LocalDateTime time,String pattern) {

 return time.format(DateTimeFormatter.ofPattern(pattern));

 }
```


​

当然localDateTime是java8的一大亮点，当然不仅仅只是解决了线程安全的问题，同样也提供了一些其他的运算比如加减天数:


```
//日期加上一个数,根据field不同加不同值,field为ChronoUnit.*

 public static LocalDateTime plus(LocalDateTime time, long number, TemporalUnit field) {

 return time.plus(number, field);

 }

​

 //日期减去一个数,根据field不同减不同值,field参数为ChronoUnit.*

 public static LocalDateTime minu(LocalDateTime time, long number, TemporalUnit field){

 return time.minus(number,field);

 }
```


最后，如果你担心使用LocalDateTime 会对你现有的代码产生很大的改变的话，那你可以将他们两进行互转:


```
//Date转换为LocalDateTime

 public static LocalDateTime convertDateToLDT(Date date) {

 return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

 }

​

 //LocalDateTime转换为Date

 public static Date convertLDTToDate(LocalDateTime time) {

 return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());

 }
```

