<template>
  <div class="ai-page">
    <div class="bot-header">
      <AiBot :loading="loading" :speaking="isSpeaking" />
      <div class="header-info">
        <span class="bot-name">AI 智能客服</span>
        <span class="bot-role" :class="{ 'status-error': aiError }">
          {{ aiError ? '服务异常' : '经营数据查询 · 菜品文案生成' }}
        </span>
      </div>
    </div>

    <!-- 快捷提示 -->
    <div class="quick-hints" v-if="messages.length <= 1 && !aiError">
      <el-tag v-for="hint in quickHints" :key="hint" class="hint-tag" effect="plain" @click="input = hint; send()">{{ hint }}</el-tag>
    </div>

    <!-- AI 异常提示 -->
    <div class="ai-error-bar" v-if="aiError">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>AI 服务暂时不可用</template>
        <template #default>
          <div style="font-size:12px;line-height:1.5">{{ aiError }}，请检查后端 AI 配置</div>
        </template>
      </el-alert>
    </div>

    <div class="chat-box" ref="chatBox">
      <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
        <div class="msg-avatar" v-if="m.role === 'bot'">
          <div class="mini-bot"></div>
        </div>
        <div class="msg-content" v-html="formatContent(m.content)"></div>
        <el-button v-if="m.role==='bot' && i===messages.length-1" circle size="small" class="speak-btn" :type="speaking ? 'primary' : ''" @click="toggleSpeak(m.content, i)" :title="speaking ? '停止朗读' : '朗读回复'">
          <el-icon :size="12"><component :is="speaking ? 'VideoPause' : 'VideoPlay'" /></el-icon>
        </el-button>
      </div>
      <!-- AI 正在输入 -->
      <div v-if="loading" class="msg bot">
        <div class="msg-avatar">
          <div class="mini-bot"></div>
        </div>
        <div class="msg-content typing">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-button circle :type="listening ? 'danger' : ''" :class="{ mic: true, active: listening }" :loading="voiceLoading" @click="toggleVoice" :title="listening ? '停止录音' : '语音输入'">
        <el-icon :size="16"><component :is="listening ? 'Microphone' : 'Microphone'" /></el-icon>
      </el-button>
      <el-input v-model="input" :placeholder="listening ? '正在聆听...' : placeholder" @keyup.enter="send" clearable />
      <el-button type="primary" :loading="loading" @click="send" :disabled="!input.trim()">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Microphone, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import AiBot from '@/components/AiBot.vue'

const route = useRoute()
const messages = ref([])
const input = ref('')
const loading = ref(false)
const isSpeaking = ref(false)
const chatBox = ref(null)
const aiError = ref('')
const listening = ref(false)
const voiceLoading = ref(false)
const currentUtterance = ref(null)

// 语音识别
let recognition = null
const initSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) return null
  const rec = new SpeechRecognition()
  rec.lang = 'zh-CN'
  rec.interimResults = false
  rec.continuous = false
  rec.onresult = (e) => {
    const text = e.results[0][0].transcript
    input.value = (input.value + text).trim()
    listening.value = false
  }
  rec.onerror = (e) => {
    console.error('Speech error:', e.error)
    listening.value = false
    voiceLoading.value = false
    if (e.error !== 'aborted') ElMessage.warning('语音识别失败，请重试')
  }
  rec.onend = () => { listening.value = false; voiceLoading.value = false }
  return rec
}

const toggleVoice = () => {
  if (listening.value) {
    recognition?.abort()
    listening.value = false
    return
  }
  if (!recognition) recognition = initSpeechRecognition()
  if (!recognition) { ElMessage.warning('当前浏览器不支持语音识别'); return }
  voiceLoading.value = true
  listening.value = true
  recognition.start()
}

// TTS 语音朗读
const toggleSpeak = (text, idx) => {
  if (speaking.value) {
    window.speechSynthesis?.cancel()
    speaking.value = false
    return
  }
  if (!window.speechSynthesis) { ElMessage.warning('当前浏览器不支持语音朗读'); return }
  // 去掉 HTML 标签
  const plain = text.replace(/<[^>]*>/g, '').replace(/&[^;]+;/g, '')
  const u = new SpeechSynthesisUtterance(plain)
  u.lang = 'zh-CN'
  u.rate = 1.0
  u.pitch = 1.0
  u.onend = () => { speaking.value = false }
  u.onerror = () => { speaking.value = false }
  speaking.value = true
  window.speechSynthesis.speak(u)
}

onBeforeUnmount(() => {
  recognition?.abort()
  window.speechSynthesis?.cancel()
})

const isMerchant = computed(() => route.path.startsWith('/admin'))

const placeholder = computed(() => isMerchant.value ? '查询今日营收、热销菜品...' : '想吃辣的，预算30以内...')

const quickHints = computed(() => isMerchant.value
  ? ['今日营收多少？', '热销菜品 TOP5', '待处理订单数']
  : ['想吃辣的，预算30以内', '低脂健康餐推荐', '查询订单状态']
)

onMounted(() => {
  messages.value = [{
    role: 'bot',
    content: isMerchant.value
      ? '你好！我是字节餐饮智能客服。\n\n你可以问我：\n📊 今日营业额 / 订单量\n🏆 热销菜品排行榜\n📦 指定订单状态\n💡 经营分析与建议'
      : '你好！我是字节餐饮智能客服。\n\n可以帮你：\n🔥 推荐菜品\n📦 查询订单\n💬 餐饮相关咨询'
  }]
})

const formatContent = (text) => text.replace(/\n/g, '<br>')

const scrollBottom = async () => {
  await nextTick()
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  aiError.value = ''
  await scrollBottom()

  loading.value = true
  isSpeaking.value = true
  try {
    const api = isMerchant.value
      ? (await import('@/api/ai')).merchantAiQuery
      : (await import('@/api/ai')).aiSuggest

    const reply = await api(text)
    messages.value.push({ role: 'bot', content: reply || 'AI 服务繁忙，请稍后重试' })
    if (reply && (reply.includes('不可用') || reply.includes('认证失败') || reply.includes('网络异常'))) {
      aiError.value = reply
    }
  } catch (e) {
    messages.value.push({ role: 'bot', content: 'AI 服务暂时不可用，请稍后重试' })
    aiError.value = e?.message || '请求失败'
  } finally {
    loading.value = false
    setTimeout(() => isSpeaking.value = false, 1000)
  }
  await scrollBottom()
}
</script>

<style scoped>
.ai-page { display: flex; flex-direction: column; height: calc(100vh - 92px); background: var(--bg-page); }

.bot-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  border-radius: 16px 16px 0 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.header-info { display: flex; flex-direction: column; }
.bot-name { font-size: 15px; font-weight: 600; color: var(--text-main); }
.bot-role { font-size: 11px; color: var(--color-success); }
.bot-role.status-error { color: var(--color-danger); }

.quick-hints { display: flex; gap: 8px; flex-wrap: wrap; padding: 10px 16px; background: var(--bg-card); }
.hint-tag { cursor: pointer; font-size: 12px; transition: all .2s; }
.hint-tag:hover { transform: translateY(-1px); box-shadow: 0 2px 6px rgba(0,0,0,.08); }

.ai-error-bar { padding: 8px 16px; background: var(--bg-card); }

.chat-box { flex: 1; overflow-y: auto; padding: 16px; }
.chat-box::-webkit-scrollbar { width: 4px; }
.chat-box::-webkit-scrollbar-thumb { background: rgba(37, 99, 235, 0.15); border-radius: 2px; }

.msg { display: flex; margin-bottom: 16px; gap: 8px; }
.msg.user { justify-content: flex-end; }
.msg-avatar { flex-shrink: 0; }
.mini-bot {
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.3);
}
.msg-content {
  max-width: 78%; padding: 10px 14px; border-radius: 16px;
  font-size: 14px; line-height: 1.65; word-break: break-word;
}
.bot .msg-content { background: var(--bg-card); color: var(--text-main); border-bottom-left-radius: 4px; box-shadow: 0 2px 10px rgba(37, 99, 235, 0.06); }
.user .msg-content { background: var(--color-primary); color: #fff; border-bottom-right-radius: 4px; box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2); }

/* 打字动画 */
.typing { display: flex; align-items: center; gap: 4px; padding: 12px 16px; }
.typing .dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--text-placeholder);
  animation: typingBounce 1.4s infinite ease-in-out;
}
.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.input-area {
  display: flex; align-items: center; gap: 8px; padding: 12px 16px;
  background: var(--bg-card); border-top: 1px solid var(--border-color);
  border-radius: 0 0 16px 16px;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.04);
}

.mic { flex-shrink: 0; transition: all .25s; }
.mic.active { animation: micPulse 1.2s ease-in-out infinite; box-shadow: 0 0 0 4px rgba(245, 34, 45, 0.2); }

@keyframes micPulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.speak-btn {
  flex-shrink: 0; width: 24px; height: 24px; margin-top: 6px;
  transition: all .2s; opacity: 0.6;
}
.speak-btn:hover { opacity: 1; transform: scale(1.1); }
</style>
