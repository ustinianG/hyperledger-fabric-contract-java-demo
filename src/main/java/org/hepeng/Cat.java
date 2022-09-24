package org.hepeng;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * author he peng
 * date 2022/1/19 15:02
 */

@DataType
@Data
@Accessors(chain = true)
public class Cat {

    @Property
    String name;

    @Property
    Integer age;

    @Property
    String color;

    @Property
    String breed;
}
