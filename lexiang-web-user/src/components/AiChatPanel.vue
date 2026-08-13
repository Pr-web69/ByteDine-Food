<template>
  <el-drawer
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    direction="rtl"
    size="400px"
    title="AI 智能客服"
  >
    <AiServiceComp />
  </el-drawer>
</template>

<script setup>
import { defineAsyncComponent } from 'vue'

defineProps({ visible: Boolean })
defineEmits(['update:visible'])

const AiServiceComp = defineAsyncComponent(() => import('@/views/admin/AiService.vue'))
</script>
