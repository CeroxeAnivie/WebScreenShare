package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ScreenShareController {

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "text/html")
    @ResponseBody
    public String getIndexPage() {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Auto Screen Share (WebSocket)</title>
                <style>
                    body { margin: 0; padding: 0; background-color: #000; overflow: hidden; }
                    #videoContainer { display: flex; justify-content: center; align-items: center; height: 100vh; width: 100vw; overflow: hidden; }
                    #screenImage { max-width: 100%%; max-height: 100vh; width: 100%%; height: 100%%; object-fit: cover; display: block; }
                    #status { position: absolute; top: 10px; left: 10px; color: white; font-family: Arial, sans-serif; z-index: 10; }
                </style>
            </head>
            <body>
                <div id="status">Connecting...</div>
                <div id="videoContainer">
                    <img id="screenImage" alt="Screen Stream">
                </div>

                <script>
                    let ws;
                    const screenImage = document.getElementById('screenImage');
                    const statusDiv = document.getElementById('status');

                    function connect() {
                        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                        const wsUrl = protocol + '//' + window.location.host + '/ws-screen';

                        ws = new WebSocket(wsUrl);

                        ws.onopen = function(event) {
                            console.log('WebSocket connection opened');
                            statusDiv.textContent = 'Connected';
                        };

                        ws.onmessage = function(event) {
                            if (event.data instanceof Blob) {
                                const blob = event.data;
                                const imageUrl = URL.createObjectURL(blob);
                                screenImage.src = imageUrl;

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
                            statusDiv.textContent = 'Disconnected (' + event.code + ')';
                        };

                        ws.onerror = function(error) {
                            console.error('WebSocket error:', error);
                            statusDiv.textContent = 'Error';
                        };
                    }

                    window.onload = connect;
                </script>
            </body>
            </html>
            """;
    }
}