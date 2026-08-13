package com.lexiang.server.service;

import com.lexiang.server.dto.SpecGroupDTO;
import com.lexiang.server.dto.SpecItemDTO;
import com.lexiang.server.vo.SpecGroupVO;
import java.util.List;
import java.util.Map;

public interface SpecService {
    List<SpecGroupVO> getSpecsByDishId(Long dishId, Long categoryId);
    Map<Long, List<SpecGroupVO>> getSpecsBatch(Map<Long, Long> dishCategoryMap);

    /** 商家后台 CRUD */
    List<SpecGroupVO> listAllGroups();
    SpecGroupVO getGroupDetail(Long groupId);
    Long addGroup(SpecGroupDTO dto);
    void updateGroup(Long groupId, SpecGroupDTO dto);
    void deleteGroup(Long groupId);

    Long addItem(SpecItemDTO dto);
    void updateItem(Long itemId, SpecItemDTO dto);
    void deleteItem(Long itemId);

    /** 菜品关联规格组 */
    void bindDishSpecs(Long dishId, List<Long> groupIds);
    List<Long> getDishGroupIds(Long dishId);

    /** 分类模板 */
    void bindCategorySpecs(Long categoryId, List<Long> groupIds);
    List<Long> getCategoryGroupIds(Long categoryId);

    /** 互斥校验（加入购物车/下单时校验） */
    void validateExclusive(Long dishId, List<Long> selectedItems);
}
