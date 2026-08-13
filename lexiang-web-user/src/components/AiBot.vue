<template>
  <div class="ai-bot" :class="{ thinking: loading, speaking: speaking }">
    <div class="bot-body">
      <div class="bot-head">
        <div class="bot-eye left" :class="{ blink: blinking }"></div>
        <div class="bot-eye right" :class="{ blink: blinking }"></div>
        <div class="bot-mouth" :class="{ open: speaking }"></div>
        <div class="bot-aura"></div>
      </div>
      <div class="bot-wave" v-if="speaking">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({ loading: Boolean, speaking: Boolean })

const blinking = ref(false)
let blinkTimer, speakTimer

onMounted(() => {
  blinkTimer = setInterval(() => { blinking.value = true; setTimeout(() => blinking.value = false, 150) }, 3000)
})

onUnmounted(() => { clearInterval(blinkTimer) })
</script>

<style scoped>
.ai-bot { display: inline-flex; flex-direction: column; align-items: center; }

.bot-body { position: relative; }
.bot-head {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, #06b6d4 0%, #3b82f6 100%);
  position: relative;
  box-shadow: 0 4px 16px rgba(6,182,212,.35);
  animation: breathe 2s ease-in-out infinite;
}

/* 思考时旋转 */
.ai-bot.thinking .bot-head { animation: spin 1.2s ease-in-out infinite; }

/* 说话时弹跳 */
.ai-bot.speaking .bot-head { animation: bounce .6s ease-in-out infinite; }

@keyframes breathe { 0%,100%{transform:scale(1)} 50%{transform:scale(1.06)} }
@keyframes spin { 0%{transform:rotate(0)} 50%{transform:rotate(8deg)} 100%{transform:rotate(0)} }
@keyframes bounce { 0%,100%{transform:translateY(0)} 40%{transform:translateY(-8px)} }

.bot-aura {
  position: absolute; inset: -4px; border-radius: 50%;
  border: 2px solid rgba(6,182,212,.2);
  animation: aura 2s ease-out infinite;
}
@keyframes aura { 0%{transform:scale(1);opacity:.6} 100%{transform:scale(1.4);opacity:0} }

.bot-eye {
  position: absolute; top: 14px; width: 7px; height: 9px; border-radius: 50%;
  background: #fff; transition: transform .1s;
}
.bot-eye.left { left: 10px; } .bot-eye.right { right: 10px; }
.bot-eye.blink { transform: scaleY(.1); }

.bot-mouth {
  position: absolute; bottom: 11px; left: 50%; transform: translateX(-50%);
  width: 12px; height: 4px; border-radius: 0 0 8px 8px;
  border: 2px solid #fff; border-top: none; transition: all .2s;
}
.bot-mouth.open {
  width: 8px; height: 8px; border-radius: 50%; border: 2px solid #fff;
  bottom: 10px; animation: talk .3s ease-in-out infinite alternate;
}
@keyframes talk { from{transform:translateX(-50%) scaleY(.6)} to{transform:translateX(-50%) scaleY(1)} }

/* 语音波形 */
.bot-wave { display: flex; align-items: end; gap: 3px; justify-content: center; margin-top: 6px; height: 20px; }
.bot-wave span { width: 3px; border-radius: 2px; background: #06b6d4; animation: wave .6s ease-in-out infinite alternate; }
.bot-wave span:nth-child(1){height:8px;animation-delay:0s} .bot-wave span:nth-child(2){height:14px;animation-delay:.15s} .bot-wave span:nth-child(3){height:10px;animation-delay:.3s}
@keyframes wave { from{transform:scaleY(.4)} to{transform:scaleY(1)} }
</style>
