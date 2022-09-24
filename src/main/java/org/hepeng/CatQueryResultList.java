package org.hepeng;

import lombok.Data;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

/**
 * @author he peng
 * @date 2022/2/8
 */

@DataType
@Data
public class CatQueryResultList {

    @Property
    List<CatQueryResult> cats;
}
