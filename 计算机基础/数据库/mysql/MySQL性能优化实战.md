# MySQL性能优化

## 性能、性能

在1990s，人们还使用拨号接入互联网的时候，浏览一个网页或加入一个线上聊天室需要几分钟的时间去加载是一件很正常的事情。而2009年Akamai公司的报告显示，如果一个网页的加载时间超过3秒钟，那么40%的用户将会放弃访问。同时网页的加载速度也和很多情况下的网站收入正相关，2006年亚马逊报告显示每超过100ms的网页加载延迟将会降低1%的网站收入，同时2008年google也统计到如果搜索时间从400ms降低到900ms，那么谷歌将会失去20%的广告收入。由此可见，提升网站性能对于业务收入和用户留存将会产生重大的影响，这也就是为什么越来越多的人在关注网站性能。

## MySQL性能

一个网站或APP的性能取决于多种因素，请求从前台发送到后台，经过数据库查询，数据处理，多系统集成交互，返回到前台再通过渲染呈现给客户，这其中任意一环出现问题都将会对客户体验造成极大的影响。常见的影响诸如大量的5XX错误访问，页面加载超时，事务阻塞。而在部分情况下，我今天所要说的MySQL服务器性能，成为了瓶颈。比如说我当前的项目 :P

## 如何优化MySQL性能

优化MySQL，就不得不说起MySQL性能优化模型，根据优化成本和优化效果，一个金字塔结构可以被绘制出来。

![sql_optimize](/Users/changle.zhang/Pictures/sql_optimize.png)

可以看到，从下到上的优化成本越来越高，而优化的效果反而越来越低。

比如针对一个没有索引的数据表进行数据查询，由于没有索引，所以查询任何一条数据时都需要进行全表扫描，我们可以通过使用更快的处理器，高速SSD等方式进行优化，这将会付出大量的金钱并且收效甚微。当然我们也可以更简单的，为查询筛选字段构建一条索引，以最低的成本实现常数级的查询速度（最优状况）。

在接下来的文章中，我将会逐层为大家带来优化的方法，同时也会讨论一些优化以外的原理。

### SQL语句优化

对于SQL语句优化，是最常见也是效果最好、成本最低的一种优化方式。

通常情况下，优秀的SQL将会以最快的速度，最小的消耗返回最完整（且没有冗余）的信息。

#### 原理

从原理上来看，我们如果要监控SQL语句的执行情况，可以通过MySQL自带的慢查询分析工具。

通过执行如下SQL：

```mysql
-- 查看是否开启慢查询日志
SHOW variables like 'slow_query_log';
-- 查看是否在慢查询日志中记录未使用到索引的查询计划
SHOW variables like 'log_queries_not_using_indexes';
-- 开启非索引查询计划记录到慢查询
SET GLOBAL log_queries_not_using_indexes = ON;
-- 开启慢查询日志
SET GLOBAL slow_query_log=ON;
-- 慢查询记录本地保存位置
SHOW VARIABLES LIKE 'slow_query_log_file';
-- 慢查询限定时间(s)，超过该时间认为是慢查询
SHOW VARIABLES LIKE 'long_query_time';
-- 设置慢查询限定时间为3(s)
SET GLOBAL long_query_time=3;
-- 日志存储方式，支持FILE和TABLE，可以同时共存
SHOW VARIABLES LIKE 'log_output';
-- 设置日志存储到本地文件，因为保存到mysql.slow_log表会消耗更多的系统资源
SET GLOBAL log_output='FILE';
```

具体的慢查询日志总结可以参考这篇文章：https://www.cnblogs.com/kerrycode/p/5593204.HTML

同时需要注意的是，以上的所有修改都是只针对当前数据库生效，如果数据库发生了重启，以上配置都可能会失效。如果要使有关慢查询的相关配置永久生效，就必须修改数据库配置文件my.cnf（或其他配置文件，这里不展开赘述MySQL的多种配置方式）。

#### 配置

用最简单的方式生成一个慢查询结果：

```mysql
select sleep(4);
```

这时候直接查看慢查询日志输出结果：

```txt
/usr/sbin/mysqld, Version: 5.6.20-enterprise-commercial-advanced-log (MySQL Enterprise Server - Advanced Edition (Commercial)). started with:
Tcp port: 0  Unix socket: (null)
Time                 Id Command    Argument
# Time: 160616 17:24:35
# User@Host: root[root] @ localhost []  Id:     5
# Query_time: 3.002615  Lock_time: 0.000000 Rows_sent: 1  Rows_examined: 0
SET timestamp=1466069075;
select sleep(3);
```

`User@Host`表示执行SQL的主机信息

`Query_time`等表示SQL的执行时间，锁时间，发送行数和检查行数

`Set timestamp`表示SQL执行时刻

最后一行则代表执行SQL的具体内容

可以看到针对一次慢查询我们需要的具体信息都已经记录在了慢查询日志中。至此我们就将所有的慢查询技术输出到了文件中，在生产环境中可能会有成千上万条慢查询记录，我们总不能一条一条去翻看吧？这时候就需要引出慢查询日志分析工具，包括MySQL自带的mysqldumpslow以及Percona公司制作的pt-query-digest。

#### 使用

**mysqldumpslow**

mysqldumpslow是MySQL官方自带的慢查询日志分析工具，但是所提供的功能较为基础，在一定程度上难以满足我们的使用需求，具体的使用方式不再赘述，参见官网文档：https://dev.mysql.com/doc/refman/5.7/en/mysqldumpslow.html

**pt-query-digest**

pt-query-digest是Percona公司制作的mysql慢查询分析工具，不仅可以分析slow log，还可以用来分析general log，binlog。

一句话的使用方式如下：

```shell
Shell> pt-query-digest slow.log
```

典型的输出结果如下所示：

```txt
Column        Meaning
============  ==========================================================
Rank          The query's rank within the entire set of queries analyzed
Query ID      The query's fingerprint
Response time The total response time, and percentage of overall total
Calls         The number of times this query was executed
R/Call        The mean response time per execution
V/M           The Variance-to-mean ratio of response time
Item          The distilled query
```

详细的操作指导手册如下：https://www.percona.com/doc/percona-toolkit/LATEST/pt-query-digest.html

#### 有问题的SQL

综合来看，通过pt-query-digest分析的结果我们可以发现有问题的sql主要包含以下几种特点：

* 查询次数多且占用时间长
* IO消耗大（分析结果中Rows examine比较大的项）
* 未命中索引的SQL（分析结果中Rows examine较多但是Rows send实际上很少）

#### 分析执行计划

通过mysql自带的explain语法可以查看待执行语句的执行计划，这个执行计划源于MySQL优化器的优化结果，执行计划在基本上是可以保证准确的（当然有些情况下可能存在问题，比如explain select语句中有limit的情况，可能rows展示的条数会远远多于limit的限制数量）。

典型的执行结果如下：

![image-20191104162229085](/Users/changle.zhang/Library/Application Support/typora-user-images/image-20191104162229085.png)

其中各列的简要介绍如下：

**id:** 在查询执行过程中的各个子查询的顺序，id越大的执行越早

**select_type:** 查询类型，常见的情况如下

|    类型    | 说明 |
| ---------- | --- |
| SIMPLE | 简单表查询，不使用任何的子查询或者联表查询 |
| PRIMARY | 主查询，包含子查询的外层查询或者联表中的主表 |
| UNION | 联合查询中非主查询表 |
| SUBQUERY | 子查询 |
| DRIVEN | 派生查询表（虚拟表） |

**table:** 涉及到的表名，如果是派生查询表，会用driven(id)的方式给出

**type:** 表示当前查询语句在表中查找出所需要的结果行的方式（访问类型）

| 访问类型      | 扫描方式         | 举例                                                         |
| :------------ | ---------------- | ------------------------------------------------------------ |
| ALL           | 全表扫描         | MySQL将会遍历整个数据表来找到匹配的行，不使用where或者筛选条件没有使用索引 |
| index         | 索引全扫描       | MySQL遍历整个索引来查找数据，比如说从数据表中select一个有索引的字段，其实和全表扫描时间一样，只不过扫描结果已经按照索引进行了排序 |
| range         | 索引范围扫描     | 针对索引的>，>=，<，<=以及between会达到这样的扫描效果        |
| ref           | 非唯一索引扫描   | 针对非唯一索引的等值扫描或者唯一索引的前缀扫描，将会可能返回多条结果，所以需要针对索引进行扫描 |
| eq_ref        | 唯一索引等值扫描 | 针对唯一索引的等值扫描，最多返回一条记录（在联表过程中）     |
| const, system | 常数扫描         | 针对唯一索引或者主键的等值扫描，查询结果只包含唯一的一条记录 |
| NULL          | 不需要索引扫描   | 比如select now();                                            |

**possible_keys:** 扫描过程中可能用到的索引

**key:** 扫描过程中实际用到的索引

**key_len:** 使用索引的长度

**ref:** 使用哪一列或者常数与key一起从表中选择需要的行

**rows:** 执行查询所扫描的行数

**filtered:** 存储引擎返回的数据到达server层过滤后，剩下的满足记录的百分比悲观估计

**extra:** 执行情况的详细额外描述

| 描述信息        | 讲解                                                 | 优化建议                                                     |
| --------------- | ---------------------------------------------------- | ------------------------------------------------------------ |
| Using Index     | 使用索引覆盖，不会进行回表查询                       | 优秀的sql，无需优化                                          |
| Using where     | 在存储引擎检索后再去server层进行过滤，进行了回表查询 | 这可能是一种暗示：该查询可以受益于不同的索引，存在优化的可能 |
| Using temporary | 使用了临时表                                         | 查询需要优化，mysql创建临时表存储结果，通常发生在对不同列的order by上面 |
| Using filesort  | 对查询结果使用了外部索引排序，而不是按照索引         | 查询可能需要进行优化，可以选择针对排序列构建索引             |

### 索引优化

#### 选择合适的列构建索引

在选择构造索引的列时，遵循以下原则可以构建高效索引：

1. 在where从句，group by从句，order by从句及联表on从句中出现的列
2. 索引字段越小越好
3. 离散度较大的列，或者经常用到的列适合放在联合索引的前面

#### 减少索引冗余

索引不是越多越好，在进行DML语句时，需要对表中的索引进行维护，过多的索引将会使得DML语句时间执行过长，影响系统性能。

什么事冗余索引？比如将某列作为主键的同时又针对该列构造了唯一索引，这个唯一索引就是冗余。联合索引的前缀列和针对该列的索引也是冗余，包含主键的联合索引也是冗余。

可以通过如下的sql语句查询当前数据库是否有冗余的索引

```mysql
SELECT a.TABLE_SCHEMA, a.TABLE_NAME, a.COLUMN_NAME, 
a.INDEX_NAME AS 'index1', b.INDEX_NAME AS 'index2'
FROM information_schema.STATISTICS a 
JOIN information_schema.STATISTICS b 
ON a.TABLE_SCHEMA = b.TABLE_SCHEMA    
AND a.TABLE_NAME = b.TABLE_NAME 
AND a.SEQ_IN_INDEX = b.SEQ_IN_INDEX   
AND a.COLUMN_NAME = b.COLUMN_NAME 
WHERE a.SEQ_IN_INDEX = 1 AND a.INDEX_NAME <> b.INDEX_NAME
```

或者使用Percona公司制作的pt-duplicate-key-checker，具体的使用文档参见：https://www.percona.com/doc/percona-toolkit/LATEST/pt-duplicate-key-checker.html

#### 维护索引

随着业务的改变和程序的演进，数据库的更改，过去针对某些业务场景创建的索引也许将会再也不被用到，这时候及时删除不用到的索引将会减少数据库开销，同时也将会节约数据库中的存储资源。

可以通过分析慢查询日志+pt-index-usage工具来完成索引的使用情况分析，同时，非常建议在删除某个索引时彻底检查业务逻辑，确保索引删除不会对当前业务产生影响。可以根据前缀名看到，pt-index-usage仍然是Percona公司生产的工具，具体的文档可以参见：https://www.percona.com/doc/percona-toolkit/LATEST/pt-index-usage.html

### 数据库表结构优化

#### 选择合适的数据类型

所谓合适的数据类型，应当遵循最小原则，比如在可以遇见的整数下，尽量使用int而不是bigint。innoDb引擎下尽量少使用text和blob，尽量用not null来进行字段定义。因为innoDB引擎在维护可能为null字段的索引时，将会消耗更多的空间以及更大的计算复杂度。

#### 范式化与反范式化Schema设计

这里先来简要介绍一下常说的数据库三范式：

> 第一范式：
>
> 强调原子性，数据库中不应该存在完全一样的两条记录，每一个字段应当都是最小不可再分的；
>
> 第二范式：
>
> 在第一范式基础上，每一个数据表必须有主键，同时没有包含在主键中的列必须完全依赖于主键；
>
> 第三范式：
>
> 在第一、二范式的基础上，任何非主键属性不应当依赖于其他非主键属性（即，不应该产生依赖传递）。

这里举一个违反第三范式的例子：

| 商品名称            | 价格 | 类型 | 类型描述  | 类型详细描述    |
| ------------------- | ---- | ---- | --------- | --------------- |
| 高性能mysql         | 20   | 图书 | mysql图书 | 大学生mysql图书 |
| 小学生都能懂的mysql | 1    | 图书 | mysql图书 | 小学生mysql图书 |

这里我们可以看到，类型详细描述依赖于类型描述，而类型描述又依赖于类型，通常来看类型和类型描述在数据库设计过程中就属于冗余信息。

违反数据库设计范式将会带来很多问题，除了我们刚才讲过的数据冗余将会占用过多的空间，也将会带来数据插入异常，比如当类型和类型描述作为必填字段时，如果业务场景中只给出类型详细描述，将会在插入新数据时报错。同时在数据更新过程中也会存在问题，将`小学生都能懂的mysql`类型详细描述修改为小学生必看图书，那么类型描述不会同步触发更新，这时我们看到这本书还是mysql图书，那么就会造成业务上的错误逻辑。

遵循范式化设计规则时，也需要根据业务场景适当增加冗余。比如报表系统，在报表系统中增加很多中间表将会加快报表生成速度。这就要求我们能够在空间和时间中选择一种权衡，满足响应时间要求的前提下尽可能占用小的存储空间。

#### 数据库的垂直拆分

所谓数据库的垂直拆分，就是针对单张数据表，如果包含过多的列，我们可以将其拆分为多个表的形式。之所以需要垂直拆分，是因为MySQL对数据表有4096个列的限制，而在innoDB存储引擎下，这个限制缩小到了1017列。为了解决这个问题，就需要引入数据库的垂直拆分，通常数据库的垂直拆分遵循以下原则：

1. 把不常用的字段单独放在一个或多个额外的表中
2. 把大的字段独立放在一个表中
3. 经常使用的字段和筛选条件需要放在一起来减少联表操作

举例实际开发中用到的垂直拆分样例。

#### 数据库的水平拆分

相比于垂直拆分，水平拆分更多的是应用于数据表过大的情况。对于不同的存储引擎，最大行数的限制不尽相同，此外过大的单表查询将会使索引维护困难，响应时间过慢，引发多种线上问题。这时就需要对数据库进行水平拆分，常见的水平拆分方式就是分库分表。

常见的分表方式就是按照某种规则将数据放置到不同的表中，比如针对主键取range，取HASH，取模等。

水平分库也是按照同样的规则，将数据分布到不同的数据库中，解决单库压力，减少出现Io瓶颈的可能。

但是分库分表也会带来诸多技术上的挑战，比如跨分区表进行数据查询，后台报表系统的多库统计查询等。

### 系统配置优化

系统配置优化可以分为服务器系统配置优化及MySQL配置优化。

#### 服务器系统配置优化

常见的与MySQL息息相关的服务器系统配置包括最大网络连接数，最大打开文件数。最大网络连接数将会影响MySQL服务器在并发请求处理时的最大并发量，而我们知道MySQL所有的数据表都是以文件的形式存储在系统中，最大打开文件数将会影响MySQL处理效率，尤其是在分库分表的场景下，如果最大打开文件数过小将可能会影响到大批量数据查询的效率。

#### MySQL配置优化

通过启动过程中指定的参数或者直接使用全局配置文件（如my.cnf或者my.ini文件），可以对MySQL服务器参数进行详细的配置。

常用的MySQL配置及解释如下：

Innodb_buffer_pool_size: innodb缓冲池大小，



### 服务器硬件优化

所谓服务器硬件优化，涉及到具体的MySQL版本特性。

比如早起的MySQL服务器对于多核心CPU的支持不是很完善，这就需要我们在选择服务器的时候把cpu的主频作为主要考虑而不是核心数。

针对磁盘优化，支持随机读取的固态硬盘设备将会是优选，同时为了保证数据盘的稳定性和进行容灾备份，使用分布式存储服务或者RAID也是不错的选择。