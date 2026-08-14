import { ref } from 'vue'

/**
 * 语音能力封装：TTS播报 + ASR识别，全部走浏览器自带Web Speech API（免费、文本兜底）。
 * speaking/listening状态供虚拟形象联动动画。
 */
export function useSpeech() {
  const voiceOn = ref(true)
  const speaking = ref(false)
  const listening = ref(false)
  const ttsSupported = typeof window !== 'undefined' && 'speechSynthesis' in window
  const asrSupported = typeof window !== 'undefined' &&
    !!(window.SpeechRecognition || window.webkitSpeechRecognition)

  let rec = null
  let finalText = '' // 已确认的识别文本（持续模式下累加）

  function pickVoice() {
    const vs = window.speechSynthesis.getVoices()
    return vs.find(v => v.lang && v.lang.toLowerCase().startsWith('zh')) || null
  }

  /** 朗读一段话；关掉语音或不支持时直接resolve，不阻塞流程 */
  function speak(text) {
    return new Promise(resolve => {
      if (!voiceOn.value || !ttsSupported || !text) return resolve()
      window.speechSynthesis.cancel() // 打断上一句，保持节拍
      const u = new SpeechSynthesisUtterance(text.replace(/[#*`]/g, ''))
      u.lang = 'zh-CN'
      u.rate = 1.05
      const v = pickVoice()
      if (v) u.voice = v
      u.onstart = () => (speaking.value = true)
      u.onend = () => { speaking.value = false; resolve() }
      u.onerror = () => { speaking.value = false; resolve() }
      window.speechSynthesis.speak(u)
    })
  }

  function stopSpeak() {
    if (ttsSupported) window.speechSynthesis.cancel()
    speaking.value = false
  }

  /**
   * 开始听写（持续模式）：一直识别直到调用 stopListen()，时长由用户控制。
   * 识别结果实时累加回调（填进输入框），结束回调。
   * 不支持返回false，调用方提示用打字。
   */
  function listen(onResult, onEnd, onError) {
    if (!asrSupported || listening.value) return false
    finalText = '' // 每轮听写重置累加文本

    // 先通过 getUserMedia 请求麦克风权限（Chrome 必定弹窗），拿到权限后再启动识别
    navigator.mediaDevices.getUserMedia({ audio: true })
      .then(stream => {
        stream.getTracks().forEach(t => t.stop()) // 拿到权限就关，不占麦克风

        const SR = window.SpeechRecognition || window.webkitSpeechRecognition
        rec = new SR()
        rec.lang = 'zh-CN'
        rec.interimResults = true
        rec.continuous = true // 持续监听，不自动停止
        rec.onstart = () => (listening.value = true)
        rec.onresult = e => {
          // 累加已确认的最终结果，拼上当前未确认的临时结果
          let interim = ''
          for (let i = e.resultIndex; i < e.results.length; i++) {
            const r = e.results[i]
            if (r.isFinal) finalText += r[0].transcript
            else interim += r[0].transcript
          }
          onResult(finalText + interim)
        }
        rec.onend = () => {
          listening.value = false
          rec = null
          onEnd && onEnd()
        }
        rec.onerror = ev => {
          // no-speech 在持续模式下会频繁触发（停顿太久），不当作致命错误
          if (ev.error === 'no-speech') return
          listening.value = false
          rec = null
          onError && onError(ev.error)
          onEnd && onEnd()
        }
        try {
          rec.start()
        } catch {
          listening.value = false
          rec = null
          onError && onError('start-failed')
        }
      })
      .catch(() => {
        onError && onError('not-allowed')
        onEnd && onEnd()
      })

    return true
  }

  /** 手动停止听写 */
  function stopListen() {
    if (rec) rec.stop()
  }

  return { voiceOn, speaking, listening, ttsSupported, asrSupported, speak, stopSpeak, listen, stopListen }
}
