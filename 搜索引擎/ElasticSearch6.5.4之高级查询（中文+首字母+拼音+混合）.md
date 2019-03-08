# 准备
    es：elasticsearch6.5.4<br> 
    ik分词器：elasticsearch-analysis-ik-6.5.4<br> 
    pinping分词器：elasticsearch-analysis-pinyin-6.5.4<br> 
# 部署
## 1.解压es，把ik分词器、pinping分词器放到es的plugins文件夹下面<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es1.png?token=AXMzD0yBXgT98fnVVDVU15SX5NstYGgjks5cgi9GwA%3D%3D)<br> 
## 2.修改elasticsearch.yml（如果部署在本机的话其实没有必要修改，但是需要远程访问的话，需要配置network.host，这个默认是允许本机访问）<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es2.png?token=AXMzD1xVbqyzzdt_psy5jfz8g-GUMiunks5cgi_VwA%3D%3D)<br> 
## 3.启动es（不能用root启动，需要创建一个用户），启动的时候es会自动去加载两个插件./bin/elasticsearch<br> 
访问本地：curl -H “Content-Type: application/json” -X GET ‘localhost:9200/_all/_search’<br> 
下图说明启动成功能本地正常访问<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es3.png?token=AXMzD2Jz4oe1ak449p7g_pu1TPS2g_BUks5cgi_9wA%3D%3D)<br> 
## 4.访问插件<br> 
通过上述的方式访问很不方便，特别是之后的一些操作，这里介绍一个访问es的插件：Sense<br> 
安装方法自行百度下<br> 
192.168.10.128：是es安装的服务器ip<br> 
9200：是es默认的端口<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es4.png?token=AXMzD__bZgF7Ff3AFFvoGlyf_9X12Dg7ks5cgjAzwA%3D%3D)<br> 
## 5.设置index<br> 
    put /class
    {
    “settings”: {
    “analysis”: {
    “filter”: {

    },
    “tokenizer”: {
    “pinyin_simple”: {
    “type”: “pinyin”,
    “keep_first_letter”: true,
    “keep_separate_first_letter”: false,
    “keep_full_pinyin”: false,
    “keep_original”: false,
    “limit_first_letter_length”: 50,
    “lowercase”: true},
    “ik_max_word”: {
    “type”: “ik_max_word”,
    “use_smart”: true
    }
    },
    “analyzer”: {
    “ikMaxWord”: {
    “type”: “custom”,
    “tokenizer”: “ik_max_word”
    },
    “pinyinSimple”: {
    “type”: “custom”,
    “tokenizer”: “pinyin_simple”,
    “filter”: [
    “lowercase”
    ]
    }
    }
    }
    },
    “mappings”: {
    “person”:{
    “properties”:{
    “name”:{
    “type”:”text”,
    “analyzer”: “ikMaxWord”,
    “fields”: {
    “PS”:{
    “type”:”text”,
    “analyzer”:”pinyinSimple”
    }
    }
    }
    }
    }
    }
    }
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es5.png?token=AXMzD9rwNc9_XAluf9viuV_3UQ__6Wzfks5cgjBHwA%3D%3D)<br> 
## 6.查询配置是否可用<br> 
拼音插件<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es6.png?token=AXMzD1dP1-WyQWOLFe6IEbbhuZun6_heks5cgjBWwA%3D%3D)<br> 
ik插件<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es7.png?token=AXMzD20KzyXZE4Tzc1hOHefqpw1Jn9Zpks5cgjBswA%3D%3D)<br> 
## 7.插入一条数据<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es8.png?token=AXMzD-c1l0Evvmx-Rg4oRyX03ELk5l_rks5cgjCHwA%3D%3D)<br> 
## 8.查询<br> 
拼音查询<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es9.png?token=AXMzD0Tw-cAX-QOTFNykpsZKi1RrNOrxks5cgjCVwA%3D%3D)<br> 
中文查询<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es10.png?token=AXMzD8DmsgZ2H08a3O6heST8MbNPlTsoks5cgjDAwA%3D%3D)<br> 
混合搜索<br> 
![Image text](https://raw.githubusercontent.com/rancho00/image/master/es/es11.png?token=AXMzD9pNj7bTsmmj8qP35E03_79efCAlks5cgjDmwA%3D%3D)<br> 
