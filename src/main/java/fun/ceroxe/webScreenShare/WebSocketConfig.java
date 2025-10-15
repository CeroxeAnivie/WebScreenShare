package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private ScreenShareWebSocketHandler screenShareWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册屏幕共享的 WebSocket 处理器
        registry.addHandler(screenShareWebSocketHandler, "/ws-screen")
                .addInterceptors(new PasswordHandshakeInterceptor()) // 移除构造函数参数，因为密码现在在拦截器内部通过 @Value 获取
                .setAllowedOrigins("*");
        // 移除音频 WebSocket 端点
    }
}