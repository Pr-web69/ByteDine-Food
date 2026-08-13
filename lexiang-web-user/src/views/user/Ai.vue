<template>
  <div class="ai-page">
    <!-- 对话头部 -->
    <div class="chat-header">
      <AiBot class="chat-avatar" :loading="loading" />
      <div class="chat-header-info">
        <div class="chat-header-name">字节餐饮 AI 助手</div>
        <div class="chat-header-status" :class="{ 'status-error': aiError }">
          {{ aiError ? '服务异常' : '在线 · 随时为你推荐' }}
        </div>
      </div>
    </div>

    <!-- 快捷提示 -->
    <div class="quick-hints" v-if="messages.length <= 1 && !aiError">
      <div
        v-for="hint in quickHints" :key="hint"
        class="hint-chip"
        @click="input = hint; send()"
      >{{ hint }}</div>
    </div>

    <!-- AI 异常提示 -->
    <div class="ai-error-bar" v-if="aiError">
      <el-alert type="warning" :closable="false" show-icon>
        <template #title>AI 服务暂时不可用</template>
        <template #default>
          <div style="font-size:12px;line-height:1.5">
            {{ aiError }}，请检查后端 AI 配置
          </div>
        </template>
      </el-alert>
    </div>

    <!-- 聊天内容区 -->
    <div class="chat-box" ref="chatBox">
      <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
        <div class="msg-avatar" v-if="m.role === 'bot'">
          <div class="mini-bot"></div>
        </div>
        <div class="msg-content" v-html="formatContent(m.content)"></div>
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

    <!-- 底部输入区 -->
    <div class="input-area">
      <el-input
        v-model="input"
        placeholder="想吃辣的，预算30以内..."
        @keyup.enter="send"
        clearable
      />
      <el-button type="primary" :loading="loading" @click="send" :disabled="!input.trim()">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import AiBot from '@/components/AiBot.vue'
import { aiSuggest } from '@/api/ai'

const messages = ref([
  {
    role: 'bot',
    content: '你好！我是字节餐饮点餐助手。\n\n可以帮你：\n🔥 根据口味推荐菜品\n📦 查询订单状态\n💬 餐饮相关咨询\n\n直接输入你的需求即可~'
  }
])
const input = ref('')
const loading = ref(false)
const chatBox = ref(null)
const aiError = ref('')

const quickHints = ['想吃辣的，预算30以内', '低脂健康餐推荐', '今天有什么推荐', '想吃米饭类']

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
  try {
    const reply = await aiSuggest(text)
    messages.value.push({ role: 'bot', content: reply || 'AI服务繁忙，请稍后重试' })
    if (reply && (reply.includes('不可用') || reply.includes('认证失败') || reply.includes('网络异常'))) {
      aiError.value = reply
    }
  } catch (e) {
    messages.value.push({ role: 'bot', content: 'AI 服务暂时不可用，请稍后重试' })
    aiError.value = e?.message || '请求失败'
  } finally {
    loading.value = false
    await scrollBottom()
  }
}
</script>

<style scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 92px);
  background: var(--bg-page);
}

/* 对话头部 */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  border-radius: 16px 16px 0 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.chat-avatar { width: 40px; height: 40px; }
.chat-header-name { font-size: 15px; font-weight: 600; color: var(--text-main); }
.chat-header-status { font-size: 12px; color: var(--color-success); margin-top: 2px; }
.chat-header-status.status-error { color: var(--color-danger); }

/* 快捷提示 */
.quick-hints {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid rgba(37, 99, 235, 0.04);
}
.hint-chip {
  padding: 6px 14px;
  background: #f1f5f9;
  border-radius: 20px;
  font-size: 12px;
  color: #475569;
  cursor: pointer;
  transition: all .2s;
  border: 1px solid transparent;
}
.hint-chip:hover {
  background: #eff6ff;
  color: var(--color-primary);
  border-color: var(--color-primary);
  transform: translateY(-1px);
}

/* AI 异常提示 */
.ai-error-bar { padding: 8px 16px; background: var(--bg-card); }

/* 聊天滚动区 */
.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--bg-page);
}
.chat-box::-webkit-scrollbar { width: 4px; }
.chat-box::-webkit-scrollbar-thumb { background: rgba(37, 99, 235, 0.15); border-radius: 2px; }

/* 消息气泡 */
.msg { display: flex; margin-bottom: 16px; gap: 8px; }
.msg.user { justify-content: flex-end; }
.msg-avatar { flex-shrink: 0; }
.mini-bot {
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  box-shadow: 0 2px 8px rgba(6, 182, 212, 0.3);
}
.msg-content {
  max-width: 78%;
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
}
.bot .msg-content {
  background: var(--bg-card);
  color: var(--text-main);
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 10px rgba(37, 99, 235, 0.06);
}
.user .msg-content {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.2);
}

/* 打字动画 */
.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
}
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

/* 底部输入区 */
.input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
  background: var(--bg-card);
  border-top: 1px solid var(--border-color);
  border-radius: 0 0 16px 16px;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.04);
}
.input-area .el-input { flex: 1; }
</style>
