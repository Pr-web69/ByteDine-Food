<template>
  <div class="addr-page">
    <div class="addr-header">
      <h3 class="addr-title">收货地址</h3>
      <el-button type="primary" size="small" @click="openAdd">
        <el-icon><Plus /></el-icon>
        新增地址
      </el-button>
    </div>

    <div v-if="pageLoading" class="addr-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <template v-else>
      <div v-if="list.length" class="addr-list">
        <div v-for="a in list" :key="a.id" class="addr-card" :class="{ 'is-default': a.isDefault }">
          <div class="addr-card-top">
            <span class="addr-name">{{ a.contactName }}</span>
            <span class="addr-phone">{{ a.contactPhone }}</span>
            <el-tag v-if="a.isDefault" size="small" type="primary" effect="plain" class="addr-default-tag">默认</el-tag>
          </div>
          <div class="addr-detail">{{ a.addressDetail }}</div>
          <div class="addr-actions">
            <el-button size="small" text type="primary" @click="openEdit(a)">编辑</el-button>
            <el-button size="small" text type="primary" @click="setDefault(a.id)" :disabled="a.isDefault">
              {{ a.isDefault ? '当前默认' : '设为默认' }}
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(a.id)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无收货地址，请点击新增地址" />

      <!-- 新增/编辑地址弹窗 -->
      <el-dialog
        v-model="showDialog"
        :title="editingId ? '编辑收货地址' : '新增收货地址'"
        width="480px"
        :close-on-click-modal="false"
        destroy-on-close
        @opened="onDialogOpened"
      >
        <el-form ref="fRef" :model="f" :rules="r" label-width="80px" label-position="top">
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model="f.contactName" placeholder="请输入姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="手机号" prop="contactPhone">
            <el-input v-model="f.contactPhone" placeholder="请输入11位手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="详细地址" prop="addressDetail">
            <el-input v-model="f.addressDetail" placeholder="输入详细收货地址" type="textarea" :rows="3" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showDialog=false">取消</el-button>
          <el-button type="primary" :loading="saving" @click="save">
            {{ editingId ? '保存修改' : '保存' }}
          </el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAddressList, setDefaultAddress, deleteAddress, addAddress, updateAddress } from '@/api/address'

const router = useRouter()
const route = useRoute()
const list = ref([])
const showDialog = ref(false)
const saving = ref(false)
const pageLoading = ref(true)
const editingId = ref(null)
const fRef = ref(null)
const f = reactive({
  contactName: '',
  contactPhone: '',
  addressDetail: ''
})

const r = {
  contactName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  addressDetail: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

const load = async () => {
  pageLoading.value = true
  try {
    const res = await getAddressList()
    list.value = Array.isArray(res) ? res : []
  } catch (err) {
    console.error('加载地址失败：', err)
    ElMessage.error('地址列表加载失败')
  } finally {
    pageLoading.value = false
  }
}

const refresh = async () => {
  try {
    const res = await getAddressList()
    list.value = Array.isArray(res) ? res : []
  } catch (err) {
    console.error('地址列表刷新失败：', err)
  }
}

onMounted(async () => {
  await load()
})

const resetForm = () => {
  f.contactName = ''
  f.contactPhone = ''
  f.addressDetail = ''
  editingId.value = null
}

const openAdd = () => {
  resetForm()
  showDialog.value = true
}

const openEdit = (addr) => {
  editingId.value = addr.id
  f.contactName = addr.contactName || ''
  f.contactPhone = addr.contactPhone || ''
  f.addressDetail = addr.addressDetail || ''
  showDialog.value = true
}

const onDialogOpened = async () => {
  await nextTick()
  fRef.value?.clearValidate()
}

const save = async () => {
  const valid = await fRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (editingId.value) {
      await updateAddress(editingId.value, { ...f })
      ElMessage.success('地址已更新')
    } else {
      await addAddress({ ...f })
      ElMessage.success('新增地址成功')
    }
    showDialog.value = false
    resetForm()
    await refresh()
    // 从结算页过来时自动返回，保持下单流程连贯
    if (route.query.from === 'checkout') {
      router.replace('/checkout')
    }
  } catch (err) {
    console.error('保存地址异常：', err)
    ElMessage.error(editingId.value ? '更新地址失败' : '新增地址失败')
  } finally {
    saving.value = false
  }
}

const setDefault = async (id) => {
  try {
    await setDefaultAddress(id)
    ElMessage.success('已设为默认地址')
    await refresh()
  } catch (err) {
    console.error('设置默认地址异常：', err)
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该收货地址吗？', '提示', { type: 'warning' })
    await deleteAddress(id)
    ElMessage.success('删除成功')
    await refresh()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('删除地址异常：', err)
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped>
.addr-page {
  padding: 0;
}

.addr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.addr-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-main);
}

.addr-loading {
  padding: 16px;
  background: var(--bg-card);
  border-radius: 12px;
}

.addr-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.addr-card {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid transparent;
  transition: all 0.2s;
}

.addr-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.addr-card.is-default {
  border-color: rgba(37, 99, 235, 0.2);
  background: rgba(37, 99, 235, 0.02);
}

.addr-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.addr-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main);
}

.addr-phone {
  font-size: 13px;
  color: var(--text-secondary);
}

.addr-default-tag {
  margin-left: auto;
}

.addr-detail {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.addr-actions {
  display: flex;
  gap: 8px;
}
</style>
