package com.lexiang.server.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.lexiang.server.entity.Dish;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DishWithSpecsVO extends Dish {
    private Boolean hasSpec;
    private List<SpecGroupVO> specGroups;
}
