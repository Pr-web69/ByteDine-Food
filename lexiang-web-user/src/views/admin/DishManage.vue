<template>
  <div class="page-wrap">
    <div class="page-toolbar">
      <h3 class="page-title">菜品管理</h3>
      <el-button type="primary" @click="dialogVisible = true; editingId = null; resetForm()">
        <el-icon><Plus /></el-icon>新增菜品
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="searchName" placeholder="菜品名称" clearable style="width:200px" @change="load" />
      <el-select v-model="searchCategoryId" placeholder="分类" clearable style="width:160px" @change="load">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="searchStatus" placeholder="状态" clearable style="width:120px" @change="load">
        <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
      </el-select>
      <el-button @click="searchName='';searchCategoryId=null;searchStatus=null;load()">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="list" stripe v-loading="loading" empty-text="暂无菜品">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="菜品名" min-width="120" />
      <el-table-column prop="price" label="价格" width="80"><template #default="{ row }">￥{{ row.price }}</template></el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column prop="sales" label="销量" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'" size="small">{{ row.status ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="editDish(row)">编辑</el-button>
          <el-button size="small" link :type="row.status ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status ? '下架' : '上架' }}</el-button>
          <el-button size="small" link type="danger" @click="deleteDish(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination" v-if="total > 10">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="10" @current-change="onPage" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑菜品' : '新增菜品'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="菜品名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="图片">
          <div style="display:flex;align-items:center;gap:10px;width:100%">
            <el-upload :show-file-list="false" :http-request="handleUpload" accept="image/*">
              <el-button>上传图片</el-button>
            </el-upload>
            <el-input v-model="form.image" placeholder="图片URL（可上传或手动填写）" />
            <img v-if="form.image" :src="form.image" style="width:60px;height:60px;object-fit:cover;border-radius:4px;border:1px solid #eee" />
          </div>
        </el-form-item>
        <el-form-item label="热门"><el-switch v-model="hotSwitch" /></el-form-item>
        <el-form-item label="今日推荐"><el-switch v-model="todaySwitch" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { uploadImage } from '@/api/upload'

const route = useRoute()
const list = ref([]), total = ref(0), page = ref(1), loading = ref(false)
const searchName = ref(''), searchCategoryId = ref(null), searchStatus = ref(null)
const categories = ref([])
const dialogVisible = ref(false), editingId = ref(null), submitting = ref(false)
const formRef = ref(null)

const form = reactive({ name: '', categoryId: null, price: 0, stock: 999, description: '', image: '' })
const hotSwitch = ref(false), todaySwitch = ref(false)

const rules = {
  name: [{ required: true, message: '请输入菜品名' }],
  categoryId: [{ required: true, message: '请选择分类' }],
  price: [{ required: true, message: '请输入价格' }],
  stock: [{ required: true, message: '请输入库存' }]
}

const resetForm = () => {
  form.name = ''; form.categoryId = null; form.price = 0; form.stock = 999; form.description = ''; form.image = ''
  hotSwitch.value = false; todaySwitch.value = false
}

const load = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/dish/list', {
      params: { page: page.value, pageSize: 10, name: searchName.value || undefined, categoryId: searchCategoryId.value || undefined, status: searchStatus.value }
    })
    list.value = res.records || []; total.value = res.total || 0
  } catch (e) { console.error('加载菜品列表失败:', e) } finally { loading.value = false }
}

const loadCategories = async () => {
  try { categories.value = await request.get('/admin/category/list', { params: { page: 1, pageSize: 100 } }).then(r => r.records || []) } catch (e) { console.error('加载分类失败:', e) }
}

onMounted(() => { loadCategories(); load() })

watch(() => route.query.name, (v) => {
  searchName.value = v || ''
  load()
}, { immediate: false })

const onPage = (p) => { page.value = p; load() }

const editDish = (row) => {
  editingId.value = row.id
  Object.assign(form, { name: row.name, categoryId: row.categoryId, price: row.price, stock: row.stock, description: row.description || '', image: row.image || '' })
  hotSwitch.value = row.isHot === 1; todaySwitch.value = row.isToday === 1
  dialogVisible.value = true
}

const handleUpload = async ({ file }) => {
  try {
    form.image = await uploadImage(file)
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败，请检查 MinIO 服务是否启动')
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false); if (!valid) return
  submitting.value = true
  try {
    const data = { ...form, isHot: hotSwitch.value ? 1 : 0, isToday: todaySwitch.value ? 1 : 0, status: 1 }
    if (editingId.value) { await request.put(`/admin/dish/${editingId.value}`, data); ElMessage.success('修改成功') }
    else { await request.post('/admin/dish', data); ElMessage.success('新增成功') }
    dialogVisible.value = false; load()
  } catch (e) { console.error('保存菜品失败:', e); ElMessage.error('操作失败') } finally { submitting.value = false }
}

const toggleStatus = async (row) => {
  const newStatus = row.status ? 0 : 1
  try { await request.put(`/admin/dish/${row.id}/status?status=${newStatus}`); ElMessage.success(newStatus ? '已上架' : '已下架'); await load() } catch (e) { console.error('上下架操作失败:', e) }
}

const deleteDish = async (id) => {
  try { await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }); await request.delete(`/admin/dish/${id}`); ElMessage.success('已删除'); load() } catch (e) { if (e !== 'cancel') console.error('删除菜品失败:', e) }
}
</script>

<style scoped>
.page-wrap { max-width: 1200px; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0; }
.search-bar { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; }
.pagination { display: flex; justify-content: center; margin-top: 16px; }
</style>
