import { ref } from 'vue'

const enabled = ref(localStorage.getItem('voiceEnabled') !== 'false')
const speaking = ref(false)

let pendingQueue = []

/** 播放语音 */
function speak(text, priority = false) {
  if (!enabled.value) return
  if (!window.speechSynthesis) {
    console.warn('浏览器不支持语音播报')
    return
  }

  // 高优先级立即打断当前播放
  if (priority) {
    window.speechSynthesis.cancel()
    speaking.value = false
    pendingQueue = []
  }

  if (speaking.value) {
    pendingQueue.push(text)
    return
  }

  _doSpeak(text)
}

function _doSpeak(text) {
  // 去除 emoji 和特殊字符，保留中文
  const clean = text.replace(/[🎤🔊💬🔥📊🏆📦🛒💰💳✅❌⚠️☑️✔️✖️⭐]/g, '')
  if (!clean.trim()) return

  const u = new SpeechSynthesisUtterance(clean)
  u.lang = 'zh-CN'
  u.rate = 1.1
  u.pitch = 1.0
  u.volume = 1.0

  u.onstart = () => { speaking.value = true }
  u.onend = () => {
    speaking.value = false
    if (pendingQueue.length) {
      _doSpeak(pendingQueue.shift())
    }
  }
  u.onerror = () => {
    speaking.value = false
    if (pendingQueue.length) {
      _doSpeak(pendingQueue.shift())
    }
  }

  window.speechSynthesis.speak(u)
}

/** 停止播放 */
function stop() {
  window.speechSynthesis?.cancel()
  speaking.value = false
  pendingQueue = []
}

/** 切换开关 */
function toggle() {
  enabled.value = !enabled.value
  localStorage.setItem('voiceEnabled', String(enabled.value))
  if (!enabled.value) stop()
  return enabled.value
}

/** 便捷语音方法 */
function speakNewOrder() {
  speak('您有新的订单，请及时处理', true)
}

function speakOrderStatus(statusText) {
  speak(`订单状态已更新为${statusText}`)
}

export function useVoice() {
  return {
    enabled,
    speaking,
    speak,
    stop,
    toggle,
    speakNewOrder,
    speakOrderStatus,
  }
}
