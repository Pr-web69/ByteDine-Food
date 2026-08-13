<template>
  <div class="page-wrap">
    <div class="page-toolbar">
      <h3 class="page-title">规格管理</h3>
      <el-button type="primary" @click="openGroupDialog(null)">
        <el-icon><Plus /></el-icon>新增规格分组
      </el-button>
    </div>

    <div class="main-layout">
      <!-- 左侧：规格分组列表 -->
      <div class="left-panel">
        <el-table
          :data="groupList"
          stripe
          v-loading="groupLoading"
          empty-text="暂无规格分组"
          highlight-current-row
          @current-change="onGroupSelect"
          ref="groupTableRef"
        >
          <el-table-column prop="groupId" label="ID" width="60" />
          <el-table-column prop="name" label="分组名" min-width="120" />
          <el-table-column label="必选" width="70">
            <template #default="{ row }">
              <el-tag :type="row.required ? 'danger' : 'info'" size="small">{{ row.required ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="互斥" width="70">
            <template #default="{ row }">
              <el-tag :type="row.exclusive ? 'warning' : 'info'" size="small">{{ row.exclusive ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="maxSelect" label="最多可选" width="90" />
          <el-table-column label="选项数" width="70">
            <template #default="{ row }">{{ row.items?.length || 0 }}</template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click.stop="openGroupDialog(row)">编辑</el-button>
              <el-button size="small" link type="danger" @click.stop="deleteGroup(row.groupId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 右侧：选中分组的选项详情 -->
      <div class="right-panel">
        <template v-if="currentGroup">
          <div class="panel-header">
            <h4 class="panel-title">{{ currentGroup.name }} — 选项列表</h4>
            <el-button type="primary" size="small" @click="openItemDialog(null)">
              <el-icon><Plus /></el-icon>新增选项
            </el-button>
          </div>

          <el-table
            :data="currentGroup.items || []"
            stripe
            v-loading="itemLoading"
            empty-text="暂无选项"
          >
            <el-table-column prop="itemId" label="ID" width="60" />
            <el-table-column prop="name" label="选项名" min-width="120" />
            <el-table-column label="加价" width="100">
              <template #default="{ row }">￥{{ row.priceExtra }}</template>
            </el-table-column>
            <el-table-column label="默认" width="70">
              <template #default="{ row }">
                <el-tag :type="row.isDefault ? 'success' : 'info'" size="small">{{ row.isDefault ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button size="small" link type="primary" @click="openItemDialog(row)">编辑</el-button>
                <el-button size="small" link type="danger" @click="deleteItem(row.itemId)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <template v-else>
          <el-empty description="请在左侧选择一个规格分组" />
        </template>
      </div>
    </div>

    <!-- 分组编辑弹窗 -->
    <el-dialog
      v-model="groupDialogVisible"
      :title="editingGroupId ? '编辑规格分组' : '新增规格分组'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-width="120px">
        <el-form-item label="分组名称" prop="name">
          <el-input v-model="groupForm.name" placeholder="如：口味、尺寸" />
        </el-form-item>
        <el-form-item label="必选">
          <el-switch v-model="requiredSwitch" />
          <span class="switch-hint">开启后用户必须选择该规格</span>
        </el-form-item>
        <el-form-item label="互斥（同组仅选1项）">
          <el-switch v-model="exclusiveSwitch" @change="onExclusiveChange" />
        </el-form-item>
        <el-form-item label="最多可选" v-if="!exclusiveSwitch" prop="maxSelect">
          <el-input-number v-model="groupForm.maxSelect" :min="1" :max="10" />
          <span class="switch-hint">不选互斥时，可设置多选上限</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="groupForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="statusSwitch" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSaving" @click="saveGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- 选项编辑弹窗 -->
    <el-dialog
      v-model="itemDialogVisible"
      :title="editingItemId ? '编辑选项' : '新增选项'"
      width="450px"
      destroy-on-close
    >
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="80px">
        <el-form-item label="选项名" prop="name">
          <el-input v-model="itemForm.name" placeholder="如：微辣、中辣" />
        </el-form-item>
        <el-form-item label="加价" prop="priceExtra">
          <el-input-number v-model="itemForm.priceExtra" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="itemForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="itemStatusSwitch" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSaving" @click="saveItem">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getGroupList, addGroup, updateGroup, deleteGroup as deleteGroupApi, addItem, updateItem, deleteItem as deleteItemApi } from '@/api/spec'

// ── 分组列表 ──
const groupList = ref([])
const groupLoading = ref(false)
const groupTableRef = ref(null)
const currentGroup = ref(null)

const loadGroups = async () => {
  groupLoading.value = true
  try {
    const data = await getGroupList()
    groupList.value = data || []
    // 保持右侧选中状态同步
    if (currentGroup.value) {
      const matched = groupList.value.find(g => g.groupId === currentGroup.value.groupId)
      currentGroup.value = matched || null
      if (matched) {
        // 恢复高亮行
        groupTableRef.value?.setCurrentRow(matched)
      }
    }
  } catch (e) {
    console.error('加载规格分组失败:', e)
  } finally {
    groupLoading.value = false
  }
}
loadGroups()

const onGroupSelect = (row) => {
  currentGroup.value = row
}

// ── 分组编辑 ──
const groupDialogVisible = ref(false)
const editingGroupId = ref(null)
const groupSaving = ref(false)
const groupFormRef = ref(null)
const requiredSwitch = ref(false)
const exclusiveSwitch = ref(true)
const statusSwitch = ref(true)

const groupForm = reactive({
  name: '',
  maxSelect: 1,
  sortOrder: 0
})

const groupRules = {
  name: [{ required: true, message: '请输入分组名称' }],
  maxSelect: [{ required: true, message: '请输入最多可选数' }]
}

const onExclusiveChange = (val) => {
  if (val) {
    groupForm.maxSelect = 1
  }
}

const openGroupDialog = (row) => {
  editingGroupId.value = row?.groupId || null
  groupForm.name = row?.name || ''
  groupForm.maxSelect = row?.maxSelect || 1
  groupForm.sortOrder = row?.sortOrder ?? 0
  requiredSwitch.value = !!row?.required
  exclusiveSwitch.value = row?.exclusive !== undefined ? !!row.exclusive : true
  statusSwitch.value = row?.status !== undefined ? !!row.status : true
  groupDialogVisible.value = true
}

const saveGroup = async () => {
  const valid = await groupFormRef.value.validate().catch(() => false)
  if (!valid) return
  groupSaving.value = true
  try {
    const dto = {
      name: groupForm.name,
      isRequired: requiredSwitch.value ? 1 : 0,
      maxSelect: exclusiveSwitch.value ? 1 : groupForm.maxSelect,
      isExclusive: exclusiveSwitch.value ? 1 : 0,
      sortOrder: groupForm.sortOrder,
      status: statusSwitch.value ? 1 : 0
    }
    if (editingGroupId.value) {
      await updateGroup(editingGroupId.value, dto)
      ElMessage.success('修改成功')
    } else {
      await addGroup(dto)
      ElMessage.success('新增成功')
    }
    groupDialogVisible.value = false
    await loadGroups()
  } catch (e) {
    console.error('保存规格分组失败:', e)
    ElMessage.error('操作失败')
  } finally {
    groupSaving.value = false
  }
}

const deleteGroup = async (id) => {
  try {
    await ElMessageBox.confirm('删除分组将同时删除其下所有选项，确定删除？', '提示', { type: 'warning' })
    await deleteGroupApi(id)
    ElMessage.success('已删除')
    if (currentGroup.value?.groupId === id) currentGroup.value = null
    await loadGroups()
  } catch (e) {
    if (e !== 'cancel') console.error('删除规格分组失败:', e)
  }
}

// ── 选项编辑 ──
const itemDialogVisible = ref(false)
const editingItemId = ref(null)
const itemSaving = ref(false)
const itemFormRef = ref(null)
const itemLoading = ref(false)
const itemStatusSwitch = ref(true)

const itemForm = reactive({
  name: '',
  priceExtra: 0,
  sortOrder: 0
})

const itemRules = {
  name: [{ required: true, message: '请输入选项名' }],
  priceExtra: [{ required: true, message: '请输入加价' }]
}

const openItemDialog = (row) => {
  editingItemId.value = row?.itemId || null
  itemForm.name = row?.name || ''
  itemForm.priceExtra = row?.priceExtra ?? 0
  itemForm.sortOrder = row?.sortOrder ?? 0
  itemStatusSwitch.value = row?.status !== undefined ? !!row.status : true
  itemDialogVisible.value = true
}

const saveItem = async () => {
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSaving.value = true
  try {
    const dto = {
      groupId: currentGroup.value.groupId,
      name: itemForm.name,
      priceExtra: itemForm.priceExtra,
      sortOrder: itemForm.sortOrder,
      status: itemStatusSwitch.value ? 1 : 0
    }
    if (editingItemId.value) {
      await updateItem(editingItemId.value, dto)
      ElMessage.success('修改成功')
    } else {
      await addItem(dto)
      ElMessage.success('新增成功')
    }
    itemDialogVisible.value = false
    // 刷新分组列表以同步选项数据
    await loadGroups()
  } catch (e) {
    console.error('保存选项失败:', e)
    ElMessage.error('操作失败')
  } finally {
    itemSaving.value = false
  }
}

const deleteItem = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该选项？', '提示', { type: 'warning' })
    await deleteItemApi(id)
    ElMessage.success('已删除')
    await loadGroups()
  } catch (e) {
    if (e !== 'cancel') console.error('删除选项失败:', e)
  }
}
</script>

<style scoped>
.page-wrap { max-width: 1200px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }

.main-layout { display: flex; gap: 20px; }
.left-panel { flex: 1; min-width: 0; }
.right-panel { flex: 1; min-width: 0; }

.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.panel-title { font-size: 15px; font-weight: 600; margin: 0; }

.switch-hint { margin-left: 8px; color: #909399; font-size: 12px; }
</style>
