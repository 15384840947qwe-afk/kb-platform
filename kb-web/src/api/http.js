import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router.js'

// axios实例：baseURL=/kb，配合vite代理转到网关9001
const http = axios.create({ baseURL: '/kb' })

// 请求拦截器：自动带上token，后端拦截器靠它认人
http.interceptors.request.use(cfg => {
  const t = localStorage.getItem('kb-token')
  if (t) cfg.headers.Authorization = 'Bearer ' + t
  return cfg
})

// 响应拦截器：统一拆包。
// 约定body.code=200才算成功，成功直接返回data，页面代码不用层层解包
http.interceptors.response.use(
  r => {
    const body = r.data
    if (body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body.data
  },
  e => {
    // HTTP层401=没登录/过期：清token踢回登录页
    if (e.response && e.response.status === 401) {
      localStorage.removeItem('kb-token')
      localStorage.removeItem('kb-user')
      router.push('/login')
      ElMessage.error('未登录或登录已过期')
    } else {
      // 优先用服务端给的文案（如限流429的"请求太频繁"），兼容旧响应结构
      const body = e.response && e.response.data
      ElMessage.error((body && body.message) || '网络错误')
    }
    return Promise.reject(e)
  }
)

export default http

/** multipart上传：传FormData时axios自动加boundary，Authorization照常带 */
export function upload(url, formData) {
  return http.post(url, formData)
}

/**
 * 拉纯文本（如简历导出的Markdown）：响应不是Result包装，
 * 走axios会被拦截器误判失败，所以用fetch带token直接取原文
 */
export async function fetchText(url) {
  const token = localStorage.getItem('kb-token')
  const res = await fetch('/kb' + url, {
    headers: token ? { Authorization: 'Bearer ' + token } : {}
  })
  if (res.status === 401) {
    localStorage.removeItem('kb-token')
    localStorage.removeItem('kb-user')
    router.push('/login')
    throw new Error('未登录或登录已过期')
  }
  if (!res.ok) throw new Error('HTTP ' + res.status)
  return res.text()
}

/**
 * SSE流式POST：EventSource没法带Authorization头，所以用fetch+ReadableStream自己解SSE。
 * 后端事件约定：delta=增量文本、done=完整结果、fallback=AI不可用降级、error=异常。
 * 返回AbortController，组件卸载/用户打断时可 abort()
 */
export function streamPost(url, body, { onDelta, onDone, onFallback, onError, onClose } = {}) {
  const ctrl = new AbortController()
  const token = localStorage.getItem('kb-token')
  fetch('/kb' + url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {})
    },
    body: JSON.stringify(body ?? {}),
    signal: ctrl.signal
  }).then(async res => {
    if (res.status === 401) {
      // 和普通请求一致：token失效踢回登录页
      localStorage.removeItem('kb-token')
      localStorage.removeItem('kb-user')
      router.push('/login')
      ElMessage.error('未登录或登录已过期')
      onClose && onClose()
      return
    }
    if (!res.ok) {
      // 非200（如限流429）：尝试解析Result包装取服务端文案，拿不到就用状态码兑底
      let msg = 'HTTP ' + res.status
      try {
        const body = await res.json()
        if (body && body.message) msg = body.message
      } catch { /* 非JSON响应，用状态码提示 */ }
      // serverError标记：catch里统一弹服务端原文案（如"提问太快了"）
      throw Object.assign(new Error(msg), { serverError: true })
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      // SSE事件以空行分隔；逐段累积到缓冲区再切
      buf += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
      let idx
      while ((idx = buf.indexOf('\n\n')) >= 0) {
        const raw = buf.slice(0, idx)
        buf = buf.slice(idx + 2)
        let event = 'message'
        let data = ''
        for (const line of raw.split('\n')) {
          if (line.startsWith('event:')) event = line.slice(6).trim()
          else if (line.startsWith('data:')) data += line.slice(5).trim()
        }
        if (!data) continue
        let payload
        try { payload = JSON.parse(data) } catch { payload = data }
        if (event === 'delta') onDelta && onDelta(payload)
        else if (event === 'done') onDone && onDone(payload)
        else if (event === 'fallback') onFallback && onFallback(payload)
        else if (event === 'error') onError && onError(payload)
      }
    }
    // 流正常读完：无论收没收到done/fallback都通知调用方收尾
    onClose && onClose()
  }).catch(e => {
    if (e.name !== 'AbortError') {
      // 服务端明确拒绝（限流/403等）：弹原文案，让用户知道为什么被拒
      if (e.serverError) ElMessage.error(e.message)
      onError && onError(e)
    }
    onClose && onClose()
  })
  return ctrl
}
