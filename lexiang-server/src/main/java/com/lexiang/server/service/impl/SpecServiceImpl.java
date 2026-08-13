package com.lexiang.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lexiang.common.exception.BusinessException;
import com.lexiang.server.dto.SpecGroupDTO;
import com.lexiang.server.dto.SpecItemDTO;
import com.lexiang.server.entity.*;
import com.lexiang.server.mapper.*;
import com.lexiang.server.service.SpecService;
import com.lexiang.server.vo.SpecGroupVO;
import com.lexiang.server.vo.SpecItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecServiceImpl implements SpecService {

    private final DishSpecRelMapper dishSpecRelMapper;
    private final CategorySpecTemplateMapper categorySpecTemplateMapper;
    private final SpecGroupMapper specGroupMapper;
    private final SpecItemMapper specItemMapper;

    /* ========================================
       查询接口（用户端 + 商家端共用）
       ======================================== */

    @Override
    public List<SpecGroupVO> getSpecsByDishId(Long dishId, Long categoryId) {
        List<DishSpecRel> rels = dishSpecRelMapper.selectList(
                new LambdaQueryWrapper<DishSpecRel>().eq(DishSpecRel::getDishId, dishId));

        List<Long> groupIds;
        if (!rels.isEmpty()) {
            groupIds = rels.stream()
                    .sorted(Comparator.comparing(DishSpecRel::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(DishSpecRel::getGroupId)
                    .collect(Collectors.toList());
        } else {
            if (categoryId == null) return Collections.emptyList();
            List<CategorySpecTemplate> templates = categorySpecTemplateMapper.selectList(
                    new LambdaQueryWrapper<CategorySpecTemplate>().eq(CategorySpecTemplate::getCategoryId, categoryId));
            groupIds = templates.stream().map(CategorySpecTemplate::getGroupId).collect(Collectors.toList());
        }

        if (groupIds.isEmpty()) return Collections.emptyList();

        List<SpecGroup> groups = specGroupMapper.selectBatchIds(groupIds);
        Map<Long, SpecGroup> groupMap = groups.stream()
                .filter(g -> g.getStatus() == null || g.getStatus() == 1)
                .collect(Collectors.toMap(SpecGroup::getId, g -> g));

        List<SpecItem> allItems = specItemMapper.selectList(
                new LambdaQueryWrapper<SpecItem>()
                        .in(SpecItem::getGroupId, groupIds)
                        .eq(SpecItem::getStatus, 1)
                        .orderByAsc(SpecItem::getSortOrder));

        Map<Long, List<SpecItem>> itemsByGroup = allItems.stream()
                .collect(Collectors.groupingBy(SpecItem::getGroupId));

        return groupIds.stream()
                .filter(groupMap::containsKey)
                .map(gid -> buildGroupVO(groupMap.get(gid), itemsByGroup.getOrDefault(gid, Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<SpecGroupVO>> getSpecsBatch(Map<Long, Long> dishCategoryMap) {
        if (dishCategoryMap == null || dishCategoryMap.isEmpty()) return Collections.emptyMap();
        Map<Long, List<SpecGroupVO>> result = new HashMap<>();
        for (Map.Entry<Long, Long> entry : dishCategoryMap.entrySet()) {
            result.put(entry.getKey(), getSpecsByDishId(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    /* ========================================
       商家后台 CRUD
       ======================================== */

    @Override
    public List<SpecGroupVO> listAllGroups() {
        List<SpecGroup> groups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>().orderByAsc(SpecGroup::getSortOrder));
        if (groups.isEmpty()) return Collections.emptyList();

        List<Long> groupIds = groups.stream().map(SpecGroup::getId).collect(Collectors.toList());
        List<SpecItem> allItems = specItemMapper.selectList(
                new LambdaQueryWrapper<SpecItem>()
                        .in(SpecItem::getGroupId, groupIds)
                        .orderByAsc(SpecItem::getSortOrder));
        Map<Long, List<SpecItem>> itemsByGroup = allItems.stream()
                .collect(Collectors.groupingBy(SpecItem::getGroupId));

        return groups.stream()
                .map(g -> buildGroupVO(g, itemsByGroup.getOrDefault(g.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    public SpecGroupVO getGroupDetail(Long groupId) {
        SpecGroup group = specGroupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(400, "规格分组不存在");
        List<SpecItem> items = specItemMapper.selectList(
                new LambdaQueryWrapper<SpecItem>()
                        .eq(SpecItem::getGroupId, groupId)
                        .orderByAsc(SpecItem::getSortOrder));
        return buildGroupVO(group, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addGroup(SpecGroupDTO dto) {
        SpecGroup group = new SpecGroup();
        copyGroupProps(dto, group);
        if (group.getIsRequired() == null) group.setIsRequired(1);
        if (group.getMaxSelect() == null) group.setMaxSelect(1);
        if (group.getIsExclusive() == null) group.setIsExclusive(1);
        if (group.getSortOrder() == null) group.setSortOrder(0);
        if (group.getStatus() == null) group.setStatus(1);
        specGroupMapper.insert(group);
        return group.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId, SpecGroupDTO dto) {
        SpecGroup group = specGroupMapper.selectById(groupId);
        if (group == null) throw new BusinessException(400, "规格分组不存在");
        copyGroupProps(dto, group);
        specGroupMapper.updateById(group);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId) {
        // 先删该组下所有选项
        specItemMapper.delete(new LambdaQueryWrapper<SpecItem>().eq(SpecItem::getGroupId, groupId));
        // 删菜品关联
        dishSpecRelMapper.delete(new LambdaQueryWrapper<DishSpecRel>().eq(DishSpecRel::getGroupId, groupId));
        // 删分类模板关联
        categorySpecTemplateMapper.delete(new LambdaQueryWrapper<CategorySpecTemplate>().eq(CategorySpecTemplate::getGroupId, groupId));
        // 删分组本身
        specGroupMapper.deleteById(groupId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addItem(SpecItemDTO dto) {
        SpecGroup group = specGroupMapper.selectById(dto.getGroupId());
        if (group == null) throw new BusinessException(400, "所属规格分组不存在");
        SpecItem item = new SpecItem();
        copyItemProps(dto, item);
        if (item.getPriceExtra() == null) item.setPriceExtra(BigDecimal.ZERO);
        if (item.getSortOrder() == null) item.setSortOrder(0);
        if (item.getStatus() == null) item.setStatus(1);
        specItemMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItem(Long itemId, SpecItemDTO dto) {
        SpecItem item = specItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(400, "规格选项不存在");
        copyItemProps(dto, item);
        specItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        specItemMapper.deleteById(itemId);
    }

    /* ========================================
       菜品-规格关联
       ======================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDishSpecs(Long dishId, List<Long> groupIds) {
        // 先删旧关联
        dishSpecRelMapper.delete(new LambdaQueryWrapper<DishSpecRel>().eq(DishSpecRel::getDishId, dishId));
        // 再批量插入
        if (groupIds != null && !groupIds.isEmpty()) {
            for (int i = 0; i < groupIds.size(); i++) {
                DishSpecRel rel = new DishSpecRel();
                rel.setDishId(dishId);
                rel.setGroupId(groupIds.get(i));
                rel.setSortOrder(i);
                dishSpecRelMapper.insert(rel);
            }
        }
    }

    @Override
    public List<Long> getDishGroupIds(Long dishId) {
        List<DishSpecRel> rels = dishSpecRelMapper.selectList(
                new LambdaQueryWrapper<DishSpecRel>()
                        .eq(DishSpecRel::getDishId, dishId)
                        .orderByAsc(DishSpecRel::getSortOrder));
        return rels.stream().map(DishSpecRel::getGroupId).collect(Collectors.toList());
    }

    /* ========================================
       分类-规格模板
       ======================================== */

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindCategorySpecs(Long categoryId, List<Long> groupIds) {
        categorySpecTemplateMapper.delete(
                new LambdaQueryWrapper<CategorySpecTemplate>().eq(CategorySpecTemplate::getCategoryId, categoryId));
        if (groupIds != null && !groupIds.isEmpty()) {
            for (Long gid : groupIds) {
                CategorySpecTemplate t = new CategorySpecTemplate();
                t.setCategoryId(categoryId);
                t.setGroupId(gid);
                categorySpecTemplateMapper.insert(t);
            }
        }
    }

    @Override
    public List<Long> getCategoryGroupIds(Long categoryId) {
        List<CategorySpecTemplate> templates = categorySpecTemplateMapper.selectList(
                new LambdaQueryWrapper<CategorySpecTemplate>().eq(CategorySpecTemplate::getCategoryId, categoryId));
        return templates.stream().map(CategorySpecTemplate::getGroupId).collect(Collectors.toList());
    }

    /* ========================================
       互斥校验（下单时调用）
       ======================================== */

    /**
     * 校验用户选择的规格是否遵守互斥规则
     * @param dishId 菜品ID
     * @param selectedItems 用户选中的 specItemId 列表
     * @throws BusinessException 互斥违规时抛出
     */
    @Override
    public void validateExclusive(Long dishId, List<Long> selectedItems) {
        if (selectedItems == null || selectedItems.isEmpty()) return;

        // 查出选中的选项所属的分组
        List<SpecItem> items = specItemMapper.selectBatchIds(selectedItems);
        Map<Long, List<SpecItem>> itemsByGroup = items.stream()
                .collect(Collectors.groupingBy(SpecItem::getGroupId));

        for (Map.Entry<Long, List<SpecItem>> entry : itemsByGroup.entrySet()) {
            Long groupId = entry.getKey();
            List<SpecItem> groupItems = entry.getValue();

            SpecGroup group = specGroupMapper.selectById(groupId);
            if (group == null) continue;

            boolean isExclusive = group.getIsExclusive() != null && group.getIsExclusive() == 1;
            int maxSelect = group.getMaxSelect() != null ? group.getMaxSelect() : 1;

            if (isExclusive && groupItems.size() > 1) {
                throw new BusinessException(400,
                        "规格【" + group.getName() + "】为互斥选项，最多只能选1项");
            }
            if (!isExclusive && groupItems.size() > maxSelect) {
                throw new BusinessException(400,
                        "规格【" + group.getName() + "】最多可选" + maxSelect + "项，当前选了" + groupItems.size() + "项");
            }
        }
    }

    /* ========================================
       内部工具
       ======================================== */

    private SpecGroupVO buildGroupVO(SpecGroup g, List<SpecItem> items) {
        List<SpecItemVO> itemVOs = items.stream()
                .map(item -> SpecItemVO.builder()
                        .itemId(item.getId())
                        .name(item.getName())
                        .priceExtra(item.getPriceExtra() != null ? item.getPriceExtra() : BigDecimal.ZERO)
                        .isDefault(item.getSortOrder() != null && item.getSortOrder() == 0)
                        .build())
                .collect(Collectors.toList());
        return SpecGroupVO.builder()
                .groupId(g.getId())
                .name(g.getName())
                .required(g.getIsRequired() == null || g.getIsRequired() == 1)
                .maxSelect(g.getMaxSelect() != null ? g.getMaxSelect() : 1)
                .exclusive(g.getIsExclusive() == null || g.getIsExclusive() == 1)
                .items(itemVOs)
                .build();
    }

    private void copyGroupProps(SpecGroupDTO dto, SpecGroup group) {
        group.setName(dto.getName());
        group.setIsRequired(dto.getIsRequired());
        group.setMaxSelect(dto.getMaxSelect());
        group.setIsExclusive(dto.getIsExclusive());
        group.setSortOrder(dto.getSortOrder());
        group.setStatus(dto.getStatus());
    }

    private void copyItemProps(SpecItemDTO dto, SpecItem item) {
        item.setGroupId(dto.getGroupId());
        item.setName(dto.getName());
        item.setPriceExtra(dto.getPriceExtra());
        item.setSortOrder(dto.getSortOrder());
        item.setStatus(dto.getStatus());
    }
}
