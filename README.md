Java屏幕共享 (Java Screen Sharing)

一个基于 Java + Spring Boot + JavaCV + WebSocket 的高性能、低延迟屏幕共享解决方案。

## ✨ 特性 (Features)

*   **<img src="https://cdn-icons-png.flaticon.com/512/455/455699.png" width="20" height="20" /> 高性能共享:** 利用 JavaCV 和 FFmpeg 进行高效的屏幕捕获与 JPEG 编码。
*   **<img src="https://cdn-icons-png.flaticon.com/512/2821/2821859.png" width="20" height="20" /> WebSocket 实时传输:** 使用 WebSocket 协议推送视频帧，实现低延迟。
*   **<img src="https://cdn-icons-png.flaticon.com/512/455/455680.png" width="20" height="20" /> 多种分辨率支持:** 支持 720p、1080p 压缩以及原画输出。
*   **<img src="https://cdn-icons-png.flaticon.com/512/455/455680.png" width="20" height="20" /> 多种帧率支持:** 支持 30fps、60fps、120fps 输出。
*   **<img src="https://cdn-icons-png.flaticon.com/512/1828/1828415.png" width="20" height="20" /> 身份认证:** 可选的 8 位数字密码认证。
*   **<img src="https://cdn-icons-png.flaticon.com/512/3075/3075086.png" width="20" height="20" /> 多客户端支持:** 一个服务端可支持多个浏览器客户端同时观看。
*   **<img src="https://cdn-icons-png.flaticon.com/512/455/455679.png" width="20" height="20" /> 局域网访问:** 服务端可配置为监听所有网络接口，允许局域网内其他设备访问。
*   **<img src="https://cdn-icons-png.flaticon.com/512/3081/3081985.png" width="20" height="20" /> 自适应网页显示:** 共享内容会自适应浏览器窗口大小，完整显示。
*   **<img src="https://cdn-icons-png.flaticon.com/512/455/455676.png" width="20" height="20" /> 参数化启动:** 支持通过命令行参数配置屏幕、分辨率、帧率、端口、密码等。
*   **<img src="https://cdn-icons-png.flaticon.com/512/25/25231.png" width="20" height="20" /> 中文友好:** 控制台交互界面支持中文。
*   **<img src="https://cdn-icons-png.flaticon.com/512/3096/3096154.png" width="20" height="20" /> 优雅退出:** 支持 `Ctrl+C` 优雅关闭服务。

## 🛠️ 环境要求 (Prerequisites)

*   **<img src="https://cdn-icons-png.flaticon.com/512/25/25231.png" width="20" height="20" /> Java:** <a href="https://openjdk.org/projects/jdk/21/">Java 21</a> 或更高版本
*   **操作系统:** Windows, Linux, macOS (需支持 Java AWT 和 FFmpeg)
*   **网络:** 本地或局域网访问

## 🚀 快速开始 (Quick Start)

### 1. 编译项目

确保已安装 Maven，然后在项目根目录执行：

```bash
mvn clean package
```

这将在 `target/` 目录下生成一个可执行的 JAR 文件（例如 `auto-screen-share-1.0-SNAPSHOT.jar`）。

### 2. 运行服务

在命令行中运行 JAR 文件：

```bash
java -jar target/WebScreenShare-XXXX.jar
```

### 3. 配置服务

程序启动后，会根据系统语言显示交互界面：

1.  **选择屏幕:** 选择要共享的屏幕编号。
2.  **选择分辨率:** 选择 720p, 1080p 或原画输出。
3.  **选择帧率:** 选择 30fps, 60fps 或 120fps。
4.  **输入端口:** 输入 HTTP 服务端口 (例如 8080)。
5.  **设置密码:** (可选) 输入 8 位数字密码，或直接按回车跳过认证。

### 4. 访问共享

*   **本机访问:** 打开浏览器，访问 `http://localhost:<端口>` (例如 `http://localhost:8080`)。
*   **局域网访问:** 在局域网内的其他设备浏览器中，访问服务端的 IP 地址和端口 (例如 `http://192.168.1.100:8080`)。

如果设置了密码，需要在登录页面输入正确密码后才能观看共享内容。

## ⚙️ 参数化启动 (Parameterized Startup)

您可以使用以下命令行参数直接启动，跳过交互式配置：

*   `--screen <number>`: 指定要共享的屏幕编号 (例如 `--screen 0`)。
*   `--resolution <number>`: 指定输出分辨率 (0: 720p, 1: 1080p, 2: Original) (例如 `--resolution 1`)。
*   `--fps <number>`: 指定输出帧率 (0: 30fps, 1: 60fps, 2: 120fps) (例如 `--fps 2`)。
*   `--port <number>`: 指定 HTTP 服务端口 (例如 `--port 9090`)。
*   `--password <string>`: 设置访问密码 (例如 `--password 12345678`)。如果密码为空字符串 (`""`)，则跳过认证。
*   `--debug`: 启用详细日志输出。

**示例:**

```bash
# 启动服务，共享屏幕 0，1080p，60fps，端口 8081，密码 88888888
java -jar target/WebScreenShare-XXXX.jar --screen 0 --resolution 1 --fps 1 --port 8081 --password 88888888

# 启动服务，共享屏幕 1，720p，120fps，端口 9090，跳过认证
java -jar target/WebScreenShare-XXXX.jar --screen 1 --resolution 0 --fps 2 --port 9090 --password ""

# 启动服务并启用调试日志
java -jar target/WebScreenShare-XXXX.jar --debug
```

## 📈 性能与优化 (Performance & Optimization)

*   **分辨率与帧率:** 选择更高的分辨率或帧率会显著增加 CPU 和网络带宽的占用。根据网络条件和设备性能选择合适的设置。
*   **JPEG 质量:** 服务端使用高质量 JPEG 编码 (`q:v=1`)，以保证清晰度。这在高帧率下可能成为 CPU 负载的主要来源。
*   **网络:** WebSocket 传输效率高，但带宽消耗主要取决于视频流的大小（分辨率、帧率、质量）。720p/30fps 通常比 1080p/60fps 消耗更少带宽。

## 📄 许可证 (License)

This project is licensed under the <a href="https://opensource.org/licenses/MIT">MIT License</a>.

---