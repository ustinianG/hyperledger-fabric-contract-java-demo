# HyperLedger Fabric Java 合约参数传 JSON 字符串

> 合约中交易函数可以传多个参数，但是对于一些复杂参数这种方式就比较低效。对于复杂参数可以使用传 json 字符串的方式简化开发。

## 定义参数接收对象

```
@DataType
@Data
public class UserInfo {

    @Property
    String key;

    @Property
    String idCard;

    @Property
    String name;

    @Property
    String sex;

    @Property
    String birthday;

    @Property
    String phone;


}
```

## 编写合约交易函数

```
@Contract(
        name = "UserContract",
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

## Fabric 实现方式

`fabric-chaincode-shim` 先将 json 字符串反序列化为 `org.json.JSONObject` 再通过反射的方式调用参数接收对象中字段的 set 函数将值绑定的目标参数接收对象中。

org.hyperledger.fabric.contract.execution.JSONTransactionSerializer#createComponentInstance
```
    Object createComponentInstance(final String format, final String jsonString, final TypeSchema ts) {

        final DataTypeDefinition dtd = this.typeRegistry.getDataType(format);
        Object obj;
        try {
            obj = dtd.getTypeClass().getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e1) {
            throw new ContractRuntimeException("Unable to to create new instance of type", e1);
        }

        final JSONObject json = new JSONObject(jsonString);
        // request validation of the type may throw an exception if validation fails
        ts.validate(json);
        try {
            final Map<String, PropertyDefinition> fields = dtd.getProperties();
            for (final Iterator<PropertyDefinition> iterator = fields.values().iterator(); iterator.hasNext();) {
                final PropertyDefinition prop = iterator.next();

                final Field f = prop.getField();
                f.setAccessible(true);
                final Object newValue = convert(json.get(prop.getName()).toString(), prop.getSchema());

                f.set(obj, newValue);

            }
            return obj;
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InstantiationException | JSONException e) {
            throw new ContractRuntimeException("Unable to convert JSON to object", e);
        }

    }
```

## CLI 客户端调用

```
peer chaincode invoke -o orderer0.example.com:7050 --ordererTLSHostnameOverride orderer0.example.com --tls --cafile /etc/hyperledger/fabric/crypto-config/ordererOrganizations/example.com/orderers/orderer0.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C businesschannel -n hyperledger-fabric-contract-java-demo --peerAddresses peer0.org1.example.com:7051 --tlsRootCertFiles /etc/hyperledger/fabric/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses peer0.org2.example.com:7051 --tlsRootCertFiles /etc/hyperledger/fabric/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"function":"UserContract:regUser","Args":["{\"key\":\"user-0\",\"idCard\":\"610102198910210321\",\"name\":\"哈哈哈哈lx\",\"sex\":\"男\",\"birthday\":\"1980-01-27\"}"]}
```

响应结果如图：


![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/0a60aaa5243d443aaead9eff760eef51~tplv-k3u1fbpfcp-watermark.image?)