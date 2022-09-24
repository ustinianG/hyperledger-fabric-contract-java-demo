package org.hepeng;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * author he peng
 * date 2022/1/26 17:27
 */

@DataType
@Data
@Accessors(chain = true)
public class PrivateCat {

    @Property
    Cat cat;

    @Property
    String collection;
}
