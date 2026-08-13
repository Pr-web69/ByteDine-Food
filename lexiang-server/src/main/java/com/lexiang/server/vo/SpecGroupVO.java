package com.lexiang.server.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SpecGroupVO {
    private Long groupId;
    private String name;
    private Boolean required;
    private Integer maxSelect;
    private Boolean exclusive;
    private List<SpecItemVO> items;
}
