<template>
  <div class="emp-page">
    <div class="page-toolbar">
      <h3 class="page-title">员工管理</h3>
      <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>新增员工</el-button>
    </div>

    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="username" label="账号" width="130" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">{{ row.role === 'MERCHANT' ? '商家' : '店员' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" min-width="220">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text :type="row.status===1?'warning':'success'" @click="toggleStatus(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
          <el-button size="small" text type="primary" @click="resetPwd(row)">重置密码</el-button>
          <el-button size="small" text type="danger" @click="handleDel(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager" v-if="total > 0">
      <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <el-dialog v-model="dlgVisible" :title="editing?'编辑员工':'新增员工'" width="440px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号" required><el-input v-model="form.username" :disabled="editing" /></el-form-item>
        <el-form-item label="密码" :required="!editing"><el-input v-model="form.password" type="password" :placeholder="editing?'留空不修改':'请输入密码'" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.role" style="width:100%"><el-option label="店员" value="STAFF" /></el-select></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing?'保存':'新增' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dlgVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
const form = reactive({ id: null, username: '', password: '', phone: '', role: 'STAFF' })

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/employee/page', { params: { page: pageNum.value, size: pageSize.value } })
    list.value = res?.records || []
    total.value = res?.total || 0
  } catch (e) { console.error('员工列表加载失败:', e) } finally { loading.value = false }
}

const openAdd = () => { editing.value = false; Object.assign(form, { id: null, username: '', password: '', phone: '', role: 'STAFF' }); dlgVisible.value = true }
const openEdit = (row) => { editing.value = true; Object.assign(form, row); form.password = ''; dlgVisible.value = true }

const save = async () => {
  saving.value = true
  try {
    if (editing.value) {
      await request.put('/admin/employee', { id: form.id, phone: form.phone, role: form.role })
      if (form.password) await request.put('/admin/employee/' + form.id + '/reset-pwd', null, { params: { password: form.password } })
    } else {
      await request.post('/admin/employee', { ...form })
    }
    ElMessage.success('保存成功')
    dlgVisible.value = false
    load()
  } catch (e) { ElMessage.error(e?.message || '失败') } finally { saving.value = false }
}

const toggleStatus = (row) => { const s = row.status === 1 ? 0 : 1; const label = s === 0 ? '禁用' : '启用'; request.put('/admin/employee/' + row.id + '/status', null, { params: { status: s } }).then(() => { row.status = s; ElMessage.success('已' + label) }) }

const resetPwd = (row) => {
  ElMessageBox.prompt('请输入新密码', '重置密码', { confirmButtonText: '确定', cancelButtonText: '取消' }).then(({ value }) => {
    if (value) request.put('/admin/employee/' + row.id + '/reset-pwd', null, { params: { password: value } }).then(() => ElMessage.success('密码已重置'))
  }).catch(() => {})
}

const handleDel = (row) => {
  ElMessageBox.confirm('确定删除员工 ' + row.username + ' ？', '确认', { type: 'warning' }).then(() => {
    request.delete('/admin/employee/' + row.id).then(() => { ElMessage.success('已删除'); load() })
  }).catch(() => {})
}

onMounted(() => load())
</script>

<style scoped>
.emp-page { padding: 16px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { margin: 0; font-size: 18px; font-weight: 700; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
