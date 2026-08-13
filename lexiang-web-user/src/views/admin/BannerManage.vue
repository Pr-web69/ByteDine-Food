<template>
  <div class="page-wrap">
    <div class="page-toolbar">
      <h3 class="page-title">轮播图管理</h3>
      <el-button type="primary" @click="openDialog(null)"><el-icon><Plus /></el-icon>新增轮播图</el-button>
    </div>

    <el-table :data="list" stripe v-loading="loading" empty-text="暂无轮播图">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="120" />
      <el-table-column prop="imageUrl" label="图片" width="120"><template #default="{ row }"><img :src="row.imageUrl" style="width:80px;height:40px;object-fit:cover;border-radius:4px" /></template></el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="70" />
      <el-table-column prop="status" label="状态" width="70"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '启用' : '禁用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" link @click="toggle(row)">{{ row.status ? '禁用' : '启用' }}</el-button>
          <el-button size="small" link type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dVisible" :title="eId ? '编辑轮播图' : '新增轮播图'" width="450px" destroy-on-close>
      <el-form ref="fRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <div style="display:flex;align-items:center;gap:10px;width:100%">
            <el-upload :show-file-list="false" :http-request="handleUpload" accept="image/*">
              <el-button>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.imageUrl" placeholder="http://..." />
            <img v-if="form.imageUrl" :src="form.imageUrl" style="width:60px;height:40px;object-fit:cover;border-radius:4px;border:1px solid #eee" />
          </div>
        </el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { uploadImage } from '@/api/upload'

const list = ref([]), loading = ref(false)
const dVisible = ref(false), eId = ref(null), saving = ref(false)
const fRef = ref(null)
const form = reactive({ title: '', imageUrl: '', linkUrl: '', sortOrder: 0 })
const rules = { title: [{ required: true, message: '请输入标题' }], imageUrl: [{ required: true, message: '请输入图片URL' }] }

const load = async () => {
  loading.value = true
  try { const r = await request.get('/admin/banner/list', { params: { page: 1, pageSize: 100 } }); list.value = r.records || [] } catch (e) { console.error('加载轮播图列表失败:', e) } finally { loading.value = false }
}
load()

const openDialog = (row) => {
  eId.value = row?.id || null
  Object.assign(form, { title: row?.title || '', imageUrl: row?.imageUrl || '', linkUrl: row?.linkUrl || '', sortOrder: row?.sortOrder || 0 })
  dVisible.value = true
}

const handleUpload = async ({ file }) => {
  try {
    form.imageUrl = await uploadImage(file)
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败，请检查 MinIO 服务是否启动')
  }
}

const save = async () => {
  const v = await fRef.value.validate().catch(() => false); if (!v) return
  saving.value = true
  try {
    if (eId.value) { await request.put(`/admin/banner/${eId.value}`, form); ElMessage.success('修改成功') }
    else { await request.post('/admin/banner', { ...form, status: 1 }); ElMessage.success('新增成功') }
    dVisible.value = false; load()
  } catch (e) { console.error('保存轮播图失败:', e); ElMessage.error('操作失败') } finally { saving.value = false }
}

const toggle = async (row) => {
  const s = row.status ? 0 : 1
  try { await request.put(`/admin/banner/${row.id}/status?status=${s}`); ElMessage.success(s ? '已启用' : '已禁用'); await load() } catch (e) { console.error('轮播图启禁用失败:', e) }
}

const del = async (id) => {
  try { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/admin/banner/${id}`); ElMessage.success('已删除'); load() } catch (e) { if (e !== 'cancel') console.error('删除轮播图失败:', e) }
}
</script>

<style scoped>
.page-wrap { max-width: 1200px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
</style>
