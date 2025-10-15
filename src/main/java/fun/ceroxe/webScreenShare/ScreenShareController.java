package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Locale;

@Controller
public class ScreenShareController {

    @Value("${access.password:}") // 从配置中获取密码，空字符串表示跳过认证
    private String ACCESS_PASSWORD;

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/html")
    @ResponseBody
    public String getLoginPageOrStream() { // 返回登录页面或流页面
        Locale defaultLocale = Locale.getDefault();
        boolean isChinese = defaultLocale.getLanguage().toLowerCase().startsWith("zh");

        if (ACCESS_PASSWORD.isEmpty()) {
            // 如果密码为空，则直接返回流页面
            return getStreamPage(isChinese);
        } else {
            // 否则返回登录页面
            return getLoginPage(isChinese);
        }
    }

    private String getLoginPage(boolean isChinese) {
        String titleMsg = isChinese ? "屏幕共享" : "Screen Share";
        String h2Msg = isChinese ? "屏幕共享访问" : "Screen Share Access";
        String placeholderMsg = isChinese ? "输入密码" : "Enter password";
        String buttonMsg = isChinese ? "登录" : "Login";
        String errorMsg = isChinese ? "密码无效。请重试。" : "Invalid password. Please try again.";
        String connectionErrorMsg = isChinese ? "连接错误。请重试。" : "Connection error. Please try again.";

        String loginHtml = String.format("""
            <!DOCTYPE html>
            <html lang="%s">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { margin: 0; padding: 0; background-color: #f0f0f0; display: flex; justify-content: center; align-items: center; height: 100vh; font-family: Arial, sans-serif; }
                    .login-container { background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); width: 300px; text-align: center; }
                    .login-container h2 { margin-top: 0; color: #333; }
                    .login-container input[type="password"] { width: 100%%; padding: 10px; margin: 10px 0; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }
                    .login-container button { width: 100%%; padding: 10px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
                    .login-container button:hover { background-color: #0056b3; }
                    .error-message { color: red; margin-top: 10px; display: none; }
                </style>
            </head>
            <body>
                <div class="login-container">
                    <h2>%s</h2>
                    <form id="loginForm">
                        <input type="password" id="passwordInput" placeholder="%s" required>
                        <button type="submit">%s</button>
                    </form>
                    <div id="errorMessage" class="error-message">%s</div>
                </div>

                <script>
                    const loginForm = document.getElementById('loginForm');
                    const passwordInput = document.getElementById('passwordInput');
                    const errorMessage = document.getElementById('errorMessage');
                    const isChinese = %s;

                    loginForm.addEventListener('submit', function(event) {
                        event.preventDefault();

                        const inputPassword = passwordInput.value.trim();

                        fetch('/verify-password', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify({ password: inputPassword })
                        })
                        .then(response => response.json())
                        .then(data => {
                            if (data.success) {
                                localStorage.setItem('screenSharePassword', inputPassword);
                                window.location.href = '/stream';
                            } else {
                                errorMessage.style.display = 'block';
                                passwordInput.value = '';
                            }
                        })
                        .catch(error => {
                            console.error('Error verifying password:', error);
                            errorMessage.textContent = '%s';
                            errorMessage.style.display = 'block';
                        });
                    });
                </script>
            </body>
            </html>
            """,
                isChinese ? "zh" : "en",
                titleMsg, // 标题
                h2Msg,
                placeholderMsg,
                buttonMsg,
                errorMsg,
                isChinese ? "true" : "false",
                connectionErrorMsg
        );
        return loginHtml;
    }

    @RequestMapping(value = "/verify-password", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public java.util.Map<String, Object> verifyPassword(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        String inputPassword = request.get("password");

        if (ACCESS_PASSWORD.equals(inputPassword)) {
            response.put("success", true);
        } else {
            response.put("success", false);
            response.put("message", "Invalid password");
        }
        return response;
    }

    @RequestMapping(value = "/stream", method = RequestMethod.GET, produces = "text/html")
    @ResponseBody
    public String getStreamPage() {
        Locale defaultLocale = Locale.getDefault();
        boolean isChinese = defaultLocale.getLanguage().toLowerCase().startsWith("zh");
        return getStreamPage(isChinese);
    }

    private String getStreamPage(boolean isChinese) {
        String titleMsg = isChinese ? "屏幕共享" : "Screen Share";
        String connectingMsg = isChinese ? "连接中..." : "Connecting...";
        String connectedMsg = isChinese ? "已连接" : "Connected";
        String disconnectedMsg = isChinese ? "已断开连接 (" : "Disconnected (";
        String errorMsg = isChinese ? "错误" : "Error";

        String streamHtml = String.format("""
            <!DOCTYPE html>
            <html lang="%s">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { margin: 0; padding: 0; background-color: #000; overflow: hidden; }
                    #videoContainer { display: flex; justify-content: center; align-items: center; height: 100vh; width: 100vw; overflow: hidden; }
                    #screenImage { max-width: 100%%; max-height: 100vh; width: 100%%; height: 100%%; object-fit: contain; display: block; } /* 使用 contain 保证完整显示 */
                    #status { position: absolute; top: 10px; left: 10px; color: white; font-family: Arial, sans-serif; z-index: 10; }
                </style>
            </head>
            <body>
                <div id="status">%s</div>
                <div id="videoContainer">
                    <img id="screenImage" alt="Screen Stream">
                </div>

                <script>
                    let ws;
                    const screenImage = document.getElementById('screenImage');
                    const statusDiv = document.getElementById('status');
                    const isChinese = %s;

                    function connect() {
                        // 检查是否有密码 (从 localStorage 获取)
                        const storedPassword = localStorage.getItem('screenSharePassword');
                        if (!storedPassword && window.location.pathname === '/stream') {
                            // 如果没有存储密码，且在 /stream 页面，检查是否需要密码
                            // 这里假设如果不需要密码，可以直接连接
                            // 如果需要密码，应该重定向到登录页
                            // 但前端无法直接知道后端是否需要密码，所以暂时不做判断
                            // 或者，可以在 /stream 页面也进行密码检查（但这需要后端API）
                        }

                        // 构建 WebSocket URL，包含密码参数 (不安全，但满足当前简单需求)
                        // 实际生产中应通过握手协议或 HTTP 头传递认证信息
                        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                        let wsUrl = protocol + '//' + window.location.host + '/ws-screen';

                        if (storedPassword) {
                            wsUrl += '?password=' + encodeURIComponent(storedPassword);
                        }

                        ws = new WebSocket(wsUrl);

                        ws.onopen = function(event) {
                            console.log('WebSocket connection opened');
                            statusDiv.textContent = isChinese ? '%s' : '%s';
                        };

                        ws.onmessage = function(event) {
                            // 接收到二进制数据 (JPEG 图片)
                            if (event.data instanceof Blob) {
                                const blob = event.data;
                                const imageUrl = URL.createObjectURL(blob);
                                screenImage.src = imageUrl;

                                // 释放旧的 URL 对象以避免内存泄漏
                                if (screenImage.currentImageUrl) {
                                    URL.revokeObjectURL(screenImage.currentImageUrl);
                                }
                                screenImage.currentImageUrl = imageUrl;
                            } else {
                                console.warn('Received non-Blob ', event.data);
                            }
                        };

                        ws.onclose = function(event) {
                            console.log('WebSocket connection closed:', event.code, event.reason);
                            statusDiv.textContent = (isChinese ? '%s' : '%s') + event.code + ')';
                            // 尝试重连 (可选)
                            // setTimeout(connect, 5000);
                        };

                        ws.onerror = function(error) {
                            console.error('WebSocket error:', error);
                            statusDiv.textContent = isChinese ? '%s' : '%s';
                        };
                    }

                    window.onload = connect;
                </script>
            </body>
            </html>
            """,
                isChinese ? "zh" : "en",
                titleMsg,
                connectingMsg,
                isChinese ? "true" : "false",
                connectedMsg, "Connected",
                disconnectedMsg, "(Code: ",
                errorMsg, "Error"
        );
        return streamHtml;
    }
}