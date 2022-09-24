package org.hepeng;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

/**
 * author he peng
 * date 2022/2/12 22:33
 */

@DataType
@Data
@Accessors(chain = true)
public class CatQueryPageResult {

    @Property
    String bookmark;

    @Property
    List<CatQueryResult> cats;
}
