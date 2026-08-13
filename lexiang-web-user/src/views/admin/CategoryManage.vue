<template>
  <div class="page-wrap">
    <div class="page-toolbar">
      <h3 class="page-title">分类管理</h3>
      <el-button type="primary" @click="openDialog(null)"><el-icon><Plus /></el-icon>新增分类</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading" empty-text="暂无分类">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="分类名" min-width="150" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" link :type="row.status ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status ? '禁用' : '启用' }}</el-button>
          <el-button size="small" link type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="400px" destroy-on-close>
      <el-form ref="fRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const list = ref([]), loading = ref(false)
const dialogVisible = ref(false), editingId = ref(null), saving = ref(false)
const fRef = ref(null)
const form = reactive({ name: '', sortOrder: 0 })
const rules = { name: [{ required: true, message: '请输入分类名' }] }

const load = async () => {
  loading.value = true
  try { const r = await request.get('/admin/category/list', { params: { page: 1, pageSize: 100 } }); list.value = r.records || [] } catch (e) { console.error('分类列表加载失败:', e) } finally { loading.value = false }
}
load()

const openDialog = (row) => {
  editingId.value = row?.id || null; form.name = row?.name || ''; form.sortOrder = row?.sortOrder || 0; dialogVisible.value = true
}

const save = async () => {
  const v = await fRef.value.validate().catch(() => false); if (!v) return
  saving.value = true
  try {
    if (editingId.value) { await request.put(`/admin/category/${editingId.value}`, form); ElMessage.success('修改成功') }
    else { await request.post('/admin/category', form); ElMessage.success('新增成功') }
    dialogVisible.value = false; load()
  } catch (e) { console.error('保存分类失败:', e); ElMessage.error('操作失败') } finally { saving.value = false }
}

const toggleStatus = async (row) => {
  const s = row.status ? 0 : 1
  try { await request.put(`/admin/category/${row.id}/status?status=${s}`); ElMessage.success(s ? '已启用' : '已禁用'); await load() } catch (e) { console.error('分类启禁用失败:', e) }
}

const del = async (id) => {
  try { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/admin/category/${id}`); ElMessage.success('已删除'); load() } catch (e) { if (e !== 'cancel') console.error('删除分类失败:', e) }
}
</script>

<style scoped>
.page-wrap { max-width: 1200px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
</style>
