package org.hepeng;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import lombok.extern.java.Log;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.QueryResultsIteratorWithMetadata;

import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * author he peng
 * date 2022/1/19 14:56
 */


@Contract(
        name = "CatContract",
        info = @Info(
                title = "Cat contract",
                description = "The hyperlegendary car contract",
                version = "0.0.1-SNAPSHOT",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "f.carr@example.com",
                        name = "F Carr",
                        url = "https://hyperledger.example.com")))
@Default
@Log
public class CatContract implements ContractInterface {


    @Transaction
    public void initLedger(final Context ctx) {

        ChaincodeStub stub = ctx.getStub();
        for (int i = 0; i < 10; i++ ) {
            Cat cat = new Cat().setName("cat-" + i)
                    .setAge(new Random().nextInt())
                    .setBreed("橘猫")
                    .setColor("橘黄色");
            stub.putStringState(cat.getName() , JSON.toJSONString(cat));
        }

    }

    @Transaction
    public Cat queryCat(final Context ctx, final String key) {

        ChaincodeStub stub = ctx.getStub();
        String catState = stub.getStringState(key);

        if (StringUtils.isBlank(catState)) {
            String errorMessage = String.format("Cat %s does not exist", key);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        return JSON.parseObject(catState , Cat.class);
    }

    @Transaction
    public CatQueryResultList queryCatByName(final Context ctx, String name) {

        log.info(String.format("使用 name 查询 cat , name = %s" , name));

        String query = String.format("{\"selector\":{\"name\":\"%s\"} , \"use_index\":[\"_design/indexNameColorDoc\", \"indexNameColor\"]}", name);

        log.info(String.format("query string = %s" , query));
        return queryCat(ctx.getStub() , query);
    }

    @Transaction
    public CatQueryPageResult queryCatPageByName(final Context ctx, String name , Integer pageSize , String bookmark) {

        log.info(String.format("使用 name 分页查询 cat , name = %s" , name));

        String query = String.format("{\"selector\":{\"name\":\"%s\"} , \"use_index\":[\"_design/indexNameColorDoc\", \"indexNameColor\"]}", name);

        log.info(String.format("query string = %s" , query));

        ChaincodeStub stub = ctx.getStub();
        QueryResultsIteratorWithMetadata<KeyValue> queryResult = stub.getQueryResultWithPagination(query, pageSize, bookmark);

        List<CatQueryResult> cats = Lists.newArrayList();

        if (! IterableUtils.isEmpty(queryResult)) {
            for (KeyValue kv : queryResult) {
                cats.add(new CatQueryResult().setKey(kv.getKey()).setCat(JSON.parseObject(kv.getStringValue() , Cat.class)));
            }
        }

        return new CatQueryPageResult()
                .setCats(cats)
                .setBookmark(queryResult.getMetadata().getBookmark());
    }

    @Transaction
    public CatQueryResultList queryCatByNameAndColor(final Context ctx, String name , String color) {

        log.info(String.format("使用 name & color 查询 cat , name = %s , color = %s" , name , color));

        String query = String.format("{\"selector\":{\"name\":\"%s\" , \"color\":\"%s\"} , \"use_index\":[\"_design/indexNameColorDoc\", \"indexNameColor\"]}", name , color);
        return queryCat(ctx.getStub() , query);
    }


    private CatQueryResultList queryCat(ChaincodeStub stub , String query) {

        CatQueryResultList resultList = new CatQueryResultList();
        QueryResultsIterator<KeyValue> queryResult = stub.getQueryResult(query);
        List<CatQueryResult> results = Lists.newArrayList();

        if (! IterableUtils.isEmpty(queryResult)) {
            for (KeyValue kv : queryResult) {
                results.add(new CatQueryResult().setKey(kv.getKey()).setCat(JSON.parseObject(kv.getStringValue() , Cat.class)));
            }
            resultList.setCats(results);
        }

        return resultList;
    }

    @Transaction
    public Cat createCat(final Context ctx, final String key , String name , Integer age , String color , String breed) {

        ChaincodeStub stub = ctx.getStub();
        String catState = stub.getStringState(key);

        if (StringUtils.isNotBlank(catState)) {
            String errorMessage = String.format("Cat %s already exists", key);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        Cat cat = new Cat().setName(name)
                .setAge(age)
                .setBreed(breed)
                .setColor(color);

        String json = JSON.toJSONString(cat);
        stub.putStringState(key, json);

        stub.setEvent("createCatEvent" , org.apache.commons.codec.binary.StringUtils.getBytesUtf8(json));
        return cat;
    }

    @Transaction
    public Cat updateCat(final Context ctx, final String key , String name , Integer age , String color , String breed) {

        ChaincodeStub stub = ctx.getStub();
        String catState = stub.getStringState(key);

        if (StringUtils.isBlank(catState)) {
            String errorMessage = String.format("Cat %s does not exist", key);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        Cat cat = new Cat().setName(name)
                .setAge(age)
                .setBreed(breed)
                .setColor(color);

        stub.putStringState(key, JSON.toJSONString(cat));

        return cat;
    }

    @Transaction
    public Cat deleteCat(final Context ctx, final String key) {

        ChaincodeStub stub = ctx.getStub();
        String catState = stub.getStringState(key);

        if (StringUtils.isBlank(catState)) {
            String errorMessage = String.format("Cat %s does not exist", key);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        stub.delState(key);

        return JSON.parseObject(catState , Cat.class);
    }

    @Transaction
    public byte[] queryPrivateCatHash(final Context ctx, final String collection ,final String key) {

        ChaincodeStub stub = ctx.getStub();

        byte[] hash = stub.getPrivateDataHash(collection, key);

        if (ArrayUtils.isEmpty(hash)) {
            String errorMessage = String.format("Private Cat %s does not exist", key);
            log.log(Level.WARNING , errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        return hash;
    }

    @Transaction
    public PrivateCat queryPrivateCat(final Context ctx, final String collection , final String key) {

        ChaincodeStub stub = ctx.getStub();

        log.info(String.format("查询私有数据 , collection [%s] key [%s] , mspId [%s] " , collection , stub.getMspId() , key));

        String catState = stub.getPrivateDataUTF8(collection , key);

        if (StringUtils.isBlank(catState)) {
            String errorMessage = String.format("Private Cat %s does not exist", key);
            log.log(Level.WARNING , errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        return JSON.parseObject(catState , PrivateCat.class);
    }


    @Transaction
    public PrivateCat createPrivateCat(final Context ctx, final String collection , final String key , String name , Integer age , String color , String breed) {

        ChaincodeStub stub = ctx.getStub();
        log.info(String.format("创建私有数据 , collection [%s] , mspId [%s] , key [%s] , name [%s] age [%s] color [%s] breed [%s] " , collection , stub.getMspId() , key , name , age , color , breed));

        String catState = stub.getPrivateDataUTF8(collection , key);

        if (StringUtils.isNotBlank(catState)) {
            String errorMessage = String.format("Private Cat %s already exists", key);
            log.log(Level.WARNING , errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        PrivateCat cat = new PrivateCat()
                .setCat(new Cat().setName(name)
                .setAge(age)
                .setBreed(breed)
                .setColor(color))
                .setCollection(collection);

        String json = JSON.toJSONString(cat);

        log.info(String.format("要保存的数据 %s" , json));

        stub.putPrivateData(collection , key , json);

        return cat;
    }

    @Transaction
    public PrivateCat updatePrivateCat(final Context ctx, final String collection, final String key , String name , Integer age , String color , String breed) {

        ChaincodeStub stub = ctx.getStub();
        log.info(String.format("更新私有数据 , collection [%s] , mspId [%s] , key [%s] , name [%s] age [%s] color [%s] breed [%s] " , collection,stub.getMspId() , key , name , age , color , breed));

        String catState = stub.getPrivateDataUTF8(collection , key);

        if (StringUtils.isBlank(catState)) {
            String errorMessage = String.format("Private Cat %s does not exist", key);
            log.log(Level.WARNING , errorMessage);
            throw new ChaincodeException(errorMessage);
        }

        PrivateCat cat = new PrivateCat()
                .setCat(new Cat().setName(name)
                        .setAge(age)
                        .setBreed(breed)
                        .setColor(color))
                .setCollection(collection);

        String json = JSON.toJSONString(cat);

        log.info(String.format("要保存的数据 %s" , json));

        stub.putPrivateData(collection , key , json);

        return cat;
    }

    @Transaction
    public PrivateCat deletePrivateCat(final Context ctx, final String collection ,final String key) {

        ChaincodeStub stub = ctx.getStub();

        log.info(String.format("删除私有数据 , collection [%s] , mspId [%s] , key [%s] " , collection , stub.getMspId() , key));

        String catState = stub.getPrivateDataUTF8(collection , key);

        if (StringUtils.isBlank(catState)) {
            String errorMessage = String.format("Private Cat %s does not exist", key);
            log.log(Level.WARNING , errorMessage);

            throw new ChaincodeException(errorMessage);
        }

        stub.delPrivateData(collection , key);

        return JSON.parseObject(catState , PrivateCat.class);
    }

    @Override
    public void beforeTransaction(Context ctx) {
        log.info("*************************************** beforeTransaction ***************************************");
    }

    @Override
    public void afterTransaction(Context ctx, Object result) {
        log.info("*************************************** afterTransaction ***************************************");
        System.out.println("result --------> " + result);
    }
}
