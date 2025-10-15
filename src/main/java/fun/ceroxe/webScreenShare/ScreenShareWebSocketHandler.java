package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap; // 使用线程安全的 Map
import java.util.concurrent.atomic.AtomicInteger; // 线程安全的计数器

@Component
public class ScreenShareWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ScreenShareWebSocketHandler.class);

    // 使用 ConcurrentHashMap 存储活跃的 WebSocket 会话，线程安全
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 使用 AtomicInteger 计数，线程安全
    private final AtomicInteger sessionCount = new AtomicInteger(0);

    private static final int SEND_TIME_LIMIT = 60000; // 发送超时时间 (毫秒)
    private static final int SEND_BUFFER_SIZE = 1024 * 256; // 发送缓冲区大小 (256KB) - 调整为合适的大小

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        ConcurrentWebSocketSessionDecorator decoratedSession =
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, SEND_BUFFER_SIZE); // 使用正确的构造函数
        sessions.put(session.getId(), decoratedSession);
        int newCount = sessionCount.incrementAndGet();
        logger.info("WebSocket connection established. Session ID: {}, Total sessions: {}", session.getId(), newCount);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        int newCount = sessionCount.decrementAndGet();
        logger.info("WebSocket connection closed. Session ID: {}, Status: {}, Total sessions: {}", session.getId(), status, newCount);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Transport error in WebSocket session: {}", session.getId(), exception);
        sessions.remove(session.getId());
        sessionCount.decrementAndGet(); // 确保计数器也减少
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR.withReason(exception.getMessage())); // 提供错误原因
            }
        } catch (IOException e) {
            logger.error("Error closing session after transport error: {}", session.getId(), e);
        }
    }

    // 提供一个方法供 ScreenCaptureService 调用，向所有活跃会话发送数据
    public void sendFrameToAll(byte[] frameData) {
        // 使用 entrySet().iterator() 避免在迭代时修改集合结构的问题（虽然 ConcurrentHashMap 相对安全）
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new BinaryMessage(frameData));
                } catch (IOException e) {
                    logger.error("Error sending frame to session: {}", session.getId(), e);
                    // 发送失败，尝试关闭会话
                    try {
                        session.close(CloseStatus.SERVER_ERROR.withReason("Send error: " + e.getMessage()));
                    } catch (IOException closeException) {
                        logger.error("Error closing session after send failure: {}", session.getId(), closeException);
                    }
                    // 从集合中移除已关闭或出错的会话
                    sessions.remove(session.getId());
                    sessionCount.decrementAndGet();
                }
            } else {
                // 如果会话已关闭，从列表中移除
                sessions.remove(session.getId());
                sessionCount.decrementAndGet();
            }
        }
    }

    // 获取活跃会话数量 (可选，用于监控)
    public int getActiveSessionCount() {
        return sessionCount.get();
    }
}