package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

// 移除 @Component，通过 WebSocketConfig 手动注册
public class PasswordHandshakeInterceptor extends HttpSessionHandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(PasswordHandshakeInterceptor.class);

    // 通过 @Value 注入密码
    @Value("${access.password:}")
    private String ACCESS_PASSWORD;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // 检查是否设置了密码
        if (this.ACCESS_PASSWORD == null || this.ACCESS_PASSWORD.isEmpty()) {
            logger.info("WebSocket handshake successful (authentication skipped).");
            return true; // 密码为空，允许握手
        }

        // 从请求参数中获取密码
        String providedPassword = request.getURI().getQuery();
        if (providedPassword != null) {
            // 解析查询字符串，例如 "password=12345678"
            String[] params = providedPassword.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=", 2); // 使用 2 限制分割次数，处理值中包含 '=' 的情况
                if (keyValue.length == 2 && "password".equals(keyValue[0])) {
                    providedPassword = java.net.URLDecoder.decode(keyValue[1], "UTF-8"); // 解码可能的 URL 编码
                    break;
                }
                // 如果参数是 "password" 但没有值 (e.g., "...?password&...")，keyValue.length == 1
                // 这种情况下，password 值应被视为 "" (空字符串)
                if (keyValue.length == 1 && "password".equals(keyValue[0])) {
                    providedPassword = "";
                    break;
                }
            }
        }

        // 验证密码
        if (this.ACCESS_PASSWORD.equals(providedPassword)) {
            attributes.put("password", providedPassword);
            logger.info("WebSocket handshake successful with valid password.");
            return true; // 密码正确，允许握手
        } else {
            logger.warn("WebSocket handshake failed due to invalid password. Provided: '{}', Expected: '{}'", providedPassword, this.ACCESS_PASSWORD);
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false; // 密码错误，拒绝握手
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, org.springframework.http.server.ServerHttpResponse response, WebSocketHandler wsHandler, Exception ex) {
        super.afterHandshake(request, response, wsHandler, ex);
    }
}