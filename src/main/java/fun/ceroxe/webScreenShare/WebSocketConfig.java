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
        registry.addHandler(screenShareWebSocketHandler, "/ws-screen")
                .setAllowedOrigins("*"); // 注意：生产环境应限制 origins
    }
}