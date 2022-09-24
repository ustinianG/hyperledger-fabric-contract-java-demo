package org.hepeng;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * @author he peng
 * @date 2022/3/22
 */

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
