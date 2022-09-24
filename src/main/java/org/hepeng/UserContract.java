package org.hepeng;

import com.alibaba.fastjson.JSON;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;

import java.util.logging.Level;

/**
 * @author he peng
 * @date 2022/3/22
 */


@Contract(
        name = "UserContract",
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
