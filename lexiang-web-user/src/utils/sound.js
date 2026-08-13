/**
 * 商家端通知音效工具
 * 播放规则：5秒内不重复播放，避免轰炸
 */
let lastPlayTime = 0
const COOLDOWN = 5000

export function playSound(url) {
  const now = Date.now()
  if (now - lastPlayTime < COOLDOWN) return
  lastPlayTime = now

  const audio = new Audio(url)
  audio.volume = 0.7
  audio.play().catch(() => {}) // 浏览器自动播放策略兜底
}

/** 用户取消订单提示 */
export function playCancelSound() {
  playSound('/music/user_cancel.mp3')
}

/** 新订单提示 */
export function playNewOrderSound() {
  playSound('/music/new_orders.mp3')
}
