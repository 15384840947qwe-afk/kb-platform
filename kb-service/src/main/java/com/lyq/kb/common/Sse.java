package com.lyq.kb.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * SSE小工具：把"建emitter→抓身份→异步跑业务→发事件"的样板收进来。
 * 关键点：UserContext是ThreadLocal，异步线程拿不到，必须在主线程先抓住、
 * 进异步任务后重新set，否则Service里UserContext.get()是null
 */
public class Sse {

    /** 建一个60秒超时的emitter，task在异步线程里带着UserContext跑，拿emitter发事件 */
    public static SseEmitter run(Consumer<SseEmitter> task) {
        return run(task, 60_000L);
    }

    /**
     * 可指定超时：emitter超时是绝对时间（从建连算起），必须盖住AI流式耗时，
     * 否则连接先被断、done事件发不出去，前端就卡在loading里
     */
    public static SseEmitter run(Consumer<SseEmitter> task, long timeoutMs) {
        UserContext.CurrentUser user = UserContext.get();
        SseEmitter emitter = new SseEmitter(timeoutMs);
        CompletableFuture.runAsync(() -> {
            UserContext.set(user);
            try {
                task.accept(emitter);
            } catch (Exception e) {
                send(emitter, "error", java.util.Map.of("message", String.valueOf(e.getMessage())));
            } finally {
                UserContext.clear();
                complete(emitter);
            }
        });
        return emitter;
    }

    /** 发一个具名事件；连接已断就吞掉，不往上抛 */
    public static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data));
        } catch (Exception ignored) {
            // 客户端已断开，后续发送也都会失败，任务里继续跑完落库等收尾即可
        }
    }

    public static void complete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    /**
     * 流式分割器：模型按约定输出"正文 + 换行<<<RESULT>>> + JSON"，
     * 对外只吐标记之前的正文增量。标记没出现时尾部留 markerLen-1 字符缓冲，
     * 防止标记被拆在两个delta里把半个标记泄漏到界面上
     */
    public static class Splitter {
        private static final String MARK = "<<<RESULT>>>";
        private final StringBuilder full = new StringBuilder();
        private int sent = 0;

        public void accept(String delta, Consumer<String> out) {
            full.append(delta);
            int idx = full.indexOf(MARK);
            emit(idx >= 0 ? idx : Math.max(0, full.length() - (MARK.length() - 1)), out);
        }

        /** 流结束时冲掉剩余缓冲；没出现过标记就整段当正文 */
        public void flush(Consumer<String> out) {
            int idx = full.indexOf(MARK);
            emit(idx >= 0 ? idx : full.length(), out);
        }

        private void emit(int visible, Consumer<String> out) {
            if (visible > sent) {
                out.accept(full.substring(sent, visible));
                sent = visible;
            }
        }

        /** 正文：标记前的部分，即要点流式上屏的那段 */
        public String text() {
            int idx = full.indexOf(MARK);
            return (idx >= 0 ? full.substring(0, idx) : full.toString()).trim();
        }

        /** 结构化部分：标记后的JSON文本；模型没按约定输出时退化取整段 */
        public String json() {
            int idx = full.indexOf(MARK);
            return (idx >= 0 ? full.substring(idx + MARK.length()) : full.toString()).trim();
        }
    }

    /** 截取第一对花括号之间的JSON解析；解析失败返回null */
    public static JsonNode parseJson(String content, ObjectMapper mapper) {
        try {
            int s = content.indexOf('{');
            int e = content.lastIndexOf('}');
            if (s < 0 || e <= s) {
                return null;
            }
            return mapper.readTree(content.substring(s, e + 1));
        } catch (Exception ex) {
            return null;
        }
    }
}
