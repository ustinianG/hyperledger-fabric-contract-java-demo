
# 如何在 fabric 中使用 CouchDB

# 1. 为什么使用 CouchDB

Fabric 支持两种类型的节点数据库。LevelDB 是默认嵌入在 peer 节点的状态数据库。
LevelDB 用于将链码数据存储为简单的键-值对，仅支持键、键范围和复合键查询。
CouchDB 是一 个可选的状态数据库，支持以 JSON 格式在账本上建模数据并支持富查询，以便您查询实际数据 内容而不是键。
CouchDB 同样支持在链码中部署索引，以便高效查询和对大型数据集的支持。

为了发挥 CouchDB 的优势，也就是说基于内容的 JSON 查询，你的数据必须以 JSON 格式 建模。
你必须在设置你的网络之前确定使用 LevelDB 还是 CouchDB 。
由于数据兼容性的问 题，不支持节点从 LevelDB 切换为 CouchDB 。
网络中的所有节点必须使用相同的数据库类型。
如果你想 JSON 和二进制数据混合使用，你同样可以使用 CouchDB 
，但是二进制数据只 能根据键、键范围和复合键查询。

# 2. CouchDB 是什么

Couch ( Cluster Of Unreliable Commodity Hardware ) ，
它反映了 CouchDB 的目标具有高度可伸缩性，提供了高可用性和高可靠性，即使运行在容易出现故障的硬件上也是如此。

Apache CouchDB是一个开源数据库，专注于易用性和成为"完全拥抱web的数据库"。
它是一个使用JSON作为存储格式，JavaScript作为查询语言，MapReduce和HTTP作为API的面向文档的NoSQL数据库。
其中一个显著的功能就是多主复制。CouchDB的第一个版本发布在2005年，在2008年成为了Apache的项目。


不同于关系型数据库，CouchDB没有将数据和关系存储在表格里。替代的，每个数据库是一个独立的文档集合。
每一个文档维护其自己独立的数据和自包涵的schema。
一个应用程序可能会访问多个数据库，比如其中一个位于用户的手机上，另一个位于在远程的服务器上。
文档的元数据包含版本信息，让其能够合并可能因为数据库链接丢失导致的任何差异。

CouchDB实现了一个多版本并发控制（MVCC）形式，用来避免在数据库写操作的时候对文件进行加锁。
冲突留给应用程序去解决。解决一个冲突的通用操作的是首先合并数据到其中一个文档，然后删除旧的数据。

其他功能包括文档级别的ACID语义和最终一致性，MapReduce，复制（Replication）。
它还支持通过一个做Futon的内置web应用程序来进行数据库管理。

---

# 3. CouchDB 特点

## 3.1 文档存储

CouchDB将数据存储为“文档”，其为用JSON表示的有一个或者多个字段/值的对。
字段的值可以是简单的东西比如字符串，数字，或者时间；但是数组和字典同样也可以使用。
CouchDB中的每一个文档有一个唯一的id但是没有必须的文档schema。

## 3.2 ACID 语义

CouchDB提供了ACID语义，其通过多版本并发控制的形式来实现，意味着CouchDB能够处理大量的并发读写而不会产生冲突。

## 3.3 Map/Reduce 视图 和 索引

存储的数据通过视图进行组装。在CouchDB中，每一个视图都是由作为map/reduce操作中的Map部分的Javascript函数构成。
该函数接受一个文档并且将其转换为一个单独的值来返回。
CouchDB能够对视图进行索引，同时在文档新增，修改，删除的时候对这些索引进行更新。

## 3.4 支持复制的分布式架构

CouchDB的设计基于支持双向的复制（同步）和离线操作。这意味着多个复制能够对同一数据有其自己的拷贝，可以进行修改，之后将这些变更进行同步。

## 3.5 REST API

所有的数据都有一个唯一的通过HTTP暴露出来的URI。
REST使用HTTP方法 POST，GET，PUT和DELETE来操作对应的四个基本
CRUD(Create，Read，Update，Delete）操作来操作所有的资源。

## 3.5 最终一致性

CouchDB保证最终一致性，使其能够同时提供可用性和分割容忍。

## 3.6 离线支持

CoucbDB能够同步复制到可能会离线的终端设备（比如智能手机），同时当设置再次在线时处理数据同步。
CouchDB内置了一个的叫做Futon的通过web访问的管理接口。

# 4 创建索引

为什么索引很重要？

索引可以让数据库不用在每次查询的时候都检查每一行，可以让数据库运行的更快和更高效。
 一般来说，对频繁查询的数据进行索引可以使数据查询更高效。
 为了充分发挥 CouchDB 的优势 – 对 JSON 数据进行富查询的能力 – 并不需要索引，
 但是为了性能考虑强烈建议建立 索引。另外，如果在一个查询中需要排序，CouchDB 需要在排序的字段有一个索引。

# 4.1 索引定义

JSON 索引文件必须放在链码目录的 META-INF/statedb/couchdb/indexes 路径下。索引文件名称可以任意。

```json5
{
  "index":{
      "fields":["docType","owner"] // Names of the fields to be queried
  },
  "ddoc":"indexOwnerDoc", // (optional) Name of the design document in which the index will be created.
  "name":"indexOwner",
  "type":"json"
}
```
```json5
{
  "index": {
    "fields": [
      "name",
      "color"
    ]
  },
  "ddoc": "indexNameColorDoc",
  "name": "indexNameColor",
  "type": "json"
}
```

CouchDB 可以根 据查询的字段决定使用哪个索引。
如果这个查询准则存在索引，它就会被使用。但是建议在查询的时候指定 use_index 关键字。

```json5
{
    "selector":{
        "name":"tom"
    },
    "use_index":[
        "_design/indexNameColorDoc",
        "indexNameColor"
    ]
}
```

启动测试网络使用 couchdb :
```
./network.sh up createChannel -s couchdb
```

> Fauxton 是用于创建、升级和部署 CouchDB 索引的一个网页，
如果你想尝试这个接口， 有一个 Marbles 示例中索引的 Fauxton 版本格式的例子。
如果你使用 CouchDB 部署了测试网络，可以通过在浏览器的导航栏中打开 
http://localhost:5984/_utils 来 访问 Fauxton 。


```
peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C mychannel -n hyperledger-fabric-contract-java-demo --peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses localhost:9051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"function":"createCat","Args":["cat-0" , "tom" ,  "3" , "蓝色" , "大懒猫"]}'

peer chaincode query -C mychannel -n hyperledger-fabric-contract-java-demo -c '{"Args":["queryCatByName" , "tom"]}'

peer chaincode query -C mychannel -n hyperledger-fabric-contract-java-demo -c '{"Args":["queryCatPageByName" , "tom" , "1" , ""]}'
```