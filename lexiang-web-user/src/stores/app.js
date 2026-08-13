import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const isOpen = ref(true)

  const initStatus = async () => {
    try {
      const { getShopStatus } = await import('@/api/shop')
      const res = await getShopStatus()
      if (res && typeof res.open === 'boolean') isOpen.value = res.open
    } catch (e) { console.error('Store init failed:', e) }
  }

  const toggleOpen = async (val) => {
    isOpen.value = val
    try {
      const { toggleBusinessStatus } = await import('@/api/shop')
      await toggleBusinessStatus(val)
    } catch {
    }
  }

  return { isOpen, initStatus, toggleOpen }
})
