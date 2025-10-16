package fun.ceroxe.webScreenShare.config;

import fun.ceroxe.webScreenShare.handler.SignalingWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalingWebSocketHandler signalingHandler;

    @Autowired
    public WebSocketConfig(SignalingWebSocketHandler signalingHandler) {
        this.signalingHandler = signalingHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Register the signaling handler at the specified endpoint
        registry.addHandler(signalingHandler, "/ws/signaling")
                .setAllowedOrigins("*"); // Configure allowed origins as needed for production
    }
}