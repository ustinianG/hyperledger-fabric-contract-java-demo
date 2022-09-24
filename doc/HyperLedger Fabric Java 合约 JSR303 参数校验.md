# HyperLedger Fabric Java 合约 JSR303 参数校验

开始阅读本篇内容前建议先看 [HyperLedger Fabric Java 合约参数传 JSON 字符串](https://juejin.cn/post/7077853585291083790)

## 引入 hibernate-validator 依赖

```
<!-- https://mvnrepository.com/artifact/org.hibernate.validator/hibernate-validator -->
<dependency>
	<groupId>org.hibernate.validator</groupId>
	<artifactId>hibernate-validator</artifactId>
	<version>7.0.4.Final</version>
</dependency>

<!-- https://mvnrepository.com/artifact/org.glassfish/jakarta.el -->
<dependency>
	<groupId>org.glassfish</groupId>
	<artifactId>jakarta.el</artifactId>
	<version>4.0.2</version>
</dependency>
```

## 编写 SerializerInterface 实现

实现 `org.hyperledger.fabric.contract.execution.SerializerInterface` 在 `fromBuffer` 函数中增加参数校验逻辑；

> 必须在类上注释 org.hyperledger.fabric.contract.annotation.Serializer 注解，并且 target = Serializer.TARGET.TRANSACTION

```
@Serializer(target = Serializer.TARGET.TRANSACTION)
@Log
public class ValidationJSONTransactionSerializer extends JSONTransactionSerializer {

    static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();


    @Override
    public Object fromBuffer(byte[] buffer, TypeSchema ts) {

        Object obj = super.fromBuffer(buffer, ts);

        log.info(String.format("对请求参数执行参数校验 %s" , ReflectionToStringBuilder.toString(obj , ToStringStyle.JSON_STYLE)));

        Set<ConstraintViolation<Object>> constraintViolations = VALIDATOR.validate(obj);

        if (CollectionUtils.isNotEmpty(constraintViolations)) {
            Map<String , String> err = Maps.newHashMapWithExpectedSize(constraintViolations.size());
            for (ConstraintViolation<Object> cv : constraintViolations) {
                err.put(cv.getPropertyPath().toString() , cv.getMessage());
            }
            String errMsg = String.format("参数校验不通过,错误信息 %s" , JSON.toJSONString(err));

            log.info(errMsg);
            throw new ChaincodeException(errMsg);

        }
        return obj;
    }
}
```

## 在合约类注解上指定使用自定义的 Serializer

```
@Contract(
        name = "UserContract",

		// 这里指定 Serializer 类对全限定名
        transactionSerializer = "org.hepeng.ValidationJSONTransactionSerializer" ,
        info = @Info(
                title = "User contract",
                description = "user contract",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "f.carr@example.com",
                        name = "User contract",
                        url = "https://hyperledger.example.com")))
@Log
public class UserContract implements ContractInterface {

    @Transaction
    public UserInfo regUser(Context ctx , UserInfo userInfo) {
        ChaincodeStub stub = ctx.getStub();
        String user = stub.getStringState(userInfo.getKey());

        if (StringUtils.isNotBlank(user)) {
            String errorMessage = String.format("User %s already exists", userInfo.getKey());
            log.log(Level.ALL , errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        stub.putStringState(userInfo.getKey() , JSON.toJSONString(userInfo));

        return userInfo;
    }
}

```

## 在目标类上加 JSR 303 参数校验注解

```
@DataType
@Data
public class UserInfo {

    @NotBlank(message = "key 不能为空")
    @Property
    String key;

    @NotBlank(message = "idCard 不能为空")
    @Property
    String idCard;

    @NotBlank(message = "name 不能为空")
    @Length(max = 30 , message = "name 不能超过30个字符")
    @Property
    String name;

    @NotBlank(message = "sex 不能为空")
    @Property
    String sex;

    @NotBlank(message = "birthday 不能为空")
    @Property
    String birthday;

    @Property
    String phone;


}
```

## CLI 客户端调用

这里我们故意将给字段 "key" 传空字符串。

```
peer chaincode invoke -o orderer0.example.com:7050 --ordererTLSHostnameOverride orderer0.example.com --tls --cafile /etc/hyperledger/fabric/crypto-config/ordererOrganizations/example.com/orderers/orderer0.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C businesschannel -n hyperledger-fabric-contract-java-demo --peerAddresses peer0.org1.example.com:7051 --tlsRootCertFiles /etc/hyperledger/fabric/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses peer0.org2.example.com:7051 --tlsRootCertFiles /etc/hyperledger/fabric/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"function":"UserContract:regUser","Args":["{\"key\":\"\",\"idCard\":\"610102198910210321\",\"name\":\"哈哈哈哈lx\",\"sex\":\"男\",\"birthday\":\"1980-01-27\",\"phone\":\"18729977979\"}"]}'
```

响应如下：

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ac27991d63334e6696550f3ab516bc68~tplv-k3u1fbpfcp-watermark.image?)

```
[029 03-22 09:06:36.17 UTC] [chaincodeCmd] chaincodeInvokeOrQuery -> DEBU ESCC invoke result: response:<status:500 message:"\345\217\202\346\225\260\346\240\241\351\252\214\344\270\215\351\200\232\350\277\207,\351\224\231\350\257\257\344\277\241\346\201\257 {\"key\":\"key \344\270\215\350\203\275\344\270\272\347\251\272\"}" > payload:"\n \330\375*6,X\204LK\026.\222\004J\377MEc\256\256\217D\254\332\321ngO\211\031%\347\022\314\001\nX\022V\n\n_lifecycle\022H\nF\n@namespaces/fields/hyperledger-fabric-contract-java-demo/Sequence\022\002\010\023\032B\010\364\003\022=\345\217\202\346\225\260\346\240\241\351\252\214\344\270\215\351\200\232\350\277\207,\351\224\231\350\257\257\344\277\241\346\201\257 {\"key\":\"key \344\270\215\350\203\275\344\270\272\347\251\272\"}\",\022%hyperledger-fabric-contract-java-demo\032\0035.0" interest:<> 
Error: endorsement failure during invoke. response: status:500 message:"\345\217\202\346\225\260\346\240\241\351\252\214\344\270\215\351\200\232\350\277\207,\351\224\231\350\257\257\344\277\241\346\201\257 {\"key\":\"key \344\270\215\350\203\275\344\270\272\347\251\272\"}"
```

## 查看链码日志


![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/f4b785c5c03a4efbaa2e9102ce788910~tplv-k3u1fbpfcp-watermark.image?)

```
Thread[fabric-txinvoke:1,5,main] 08:56:30:081 SEVERE  org.hyperledger.fabric.shim.impl.ChaincodeInvocationTask call                    [7f0e1107] Invoke failed with error code 500. Sending ERROR
Thread[fabric-txinvoke:2,5,main] 08:57:59:033 INFO    org.hyperledger.fabric.contract.ContractRouter processRequest                    Got invoke routing request
Thread[fabric-txinvoke:2,5,main] 08:57:59:033 INFO    org.hyperledger.fabric.contract.ContractRouter processRequest                    Got the invoke request for:UserContract:regUser [{"key":"","idCard":"610102198910210321","name":"哈哈哈哈lx","sex":"男","birthday":"1980-01-27","phone":"18729977979"}]
Thread[fabric-txinvoke:2,5,main] 08:57:59:033 INFO    org.hyperledger.fabric.contract.ContractRouter processRequest                    Got routing:regUser:org.hepeng.UserContract
Thread[fabric-txinvoke:2,5,main] 08:57:59:042 INFO    org.hepeng.ValidationJSONTransactionSerializer fromBuffer                        对请求参数执行参数校验 {"birthday":"1980-01-27","idCard":"610102198910210321","key":"","name":"\u54C8\u54C8\u54C8\u54C8lx","phone":"18729977979","sex":"\u7537"}
Thread[fabric-txinvoke:2,5,main] 08:57:59:107 INFO    org.hepeng.ValidationJSONTransactionSerializer fromBuffer                        参数校验不通过,错误信息 {"key":"key 不能为空"}
Thread[fabric-txinvoke:2,5,main] 08:57:59:108 SEVERE  org.hyperledger.fabric.Logger error                                              参数校验不通过,错误信息 {"key":"key 不能为空"}org.hyperledger.fabric.shim.ChaincodeException: 参数校验不通过,错误信息 {"key":"key 不能为空"}
	at org.hepeng.ValidationJSONTransactionSerializer.fromBuffer(ValidationJSONTransactionSerializer.java:49)
	at org.hyperledger.fabric.contract.execution.impl.ContractExecutionService.convertArgs(ContractExecutionService.java:99)
	at org.hyperledger.fabric.contract.execution.impl.ContractExecutionService.executeRequest(ContractExecutionService.java:57)
	at org.hyperledger.fabric.contract.ContractRouter.processRequest(ContractRouter.java:123)
	at org.hyperledger.fabric.contract.ContractRouter.invoke(ContractRouter.java:134)
	at org.hyperledger.fabric.shim.impl.ChaincodeInvocationTask.call(ChaincodeInvocationTask.java:106)
	at org.hyperledger.fabric.shim.impl.InvocationTaskManager.lambda$newTask$17(InvocationTaskManager.java:265)
	at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(CompletableFuture.java:1736)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
	at java.base/java.lang.Thread.run(Thread.java:834)
Thread[fabric-txinvoke:2,5,main] 08:57:59:108 SEVERE  org.hyperledger.fabric.shim.impl.ChaincodeInvocationTask call                    [fdf8606d] Invoke failed with error code 500. Sending ERROR
```

完整代码地址: https://gitee.com/kernelHP/hyperledger-fabric-contract-java-demo