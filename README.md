# WebScreenShare 🖥️🔗

**基于 WebRTC 的实时屏幕共享应用，支持多接收端！**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-orange?logo=java)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)

## 🌟 特性

*   **🔐 安全访问**: 可选密码保护，保障您的共享安全。
*   **👥 多人观看**: 单一发送端，支持**多个接收端**同时观看屏幕共享。
*   **🔊 音频共享**: 支持共享屏幕的同时共享系统音频（需在浏览器共享选择器中勾选）。
*   **🌐 局域网访问**: 服务绑定到 `0.0.0.0`，方便局域网内其他设备访问。
*   **🖱️ 易于使用**: 简洁直观的网页界面，轻松开始/停止共享。
*   **🎨 友好交互**: 发送端和接收端均提供清晰的状态提示和交互反馈。
*   **🛠️ 调试友好**: 启动时添加 `--debug` 参数可获得详细的信令和连接日志。

## 🚀 快速开始

### 📋 前置要求

*   **Java 21 JDK**: 确保您的系统已安装 Java 21。 ([Eclipse Temurin](https://adoptium.net/) 是一个不错的选择)
*   **Maven**: 用于构建项目。 (通常随 JDK 一起安装，或可单独下载)
*   **支持 WebRTC 的现代浏览器**: 用于访问 `sender.html` 和 `receiver.html` (例如 Chrome, Edge, Firefox)。

### ⬇️ 获取代码

```bash
git clone https://github.com/ceroxe/WebScreenShare.git
cd WebScreenShare
```

### 🔨 构建项目

```bash
# 使用 Maven Wrapper (推荐)
./mvnw clean package

# 或者，如果您已全局安装 Maven
mvn clean package
```

### ▶️ 运行应用

```bash
# 使用 Maven 插件运行 (推荐开发)
./mvnw spring-boot:run

# 或者，运行打包好的 JAR 文件
java -jar target/*.jar

# 启用调试模式 (输出详细日志)
# ./mvnw spring-boot:run -Dspring-boot.run.arguments="--debug"
# java -jar target/*.jar --debug
```

### 🛠️ 配置

应用启动后，会在控制台提示您输入：

1.  **HTTP 端口**: 默认为 `8080`。
2.  **访问密码**: 可按回车留空表示不设置密码。

启动成功后，控制台将显示类似以下的访问链接：

```
--- 服务器已启动 ---
发送端页面 (本地): http://localhost:8080/sender.html
接收端页面 (局域网): http://<您的局域网IP>:8080/receiver.html
访问密码: '<您设置的密码>'
-------------------
```

*   **发送端**: 在需要共享屏幕的设备上打开 **发送端页面** (`sender.html`)。
*   **接收端**: 在需要观看共享的设备上打开 **接收端页面** (`receiver.html`)。可以在多个设备/浏览器标签页中打开。

### 🔒 使用

1.  在 **发送端** 页面输入密码（如果设置了密码）并点击“验证”。
2.  点击“开始共享”按钮，浏览器会弹出屏幕共享选择器。
3.  选择要共享的屏幕或窗口，并**根据需要勾选“共享音频”**。
4.  在 **接收端** 页面输入相同的密码（如果设置了密码）并点击“验证”。
5.  共享画面和音频（如果启用）将自动在接收端页面显示。

## 🏗️ 技术架构

*   **后端**: [Spring Boot 3.3.4](https://spring.io/projects/spring-boot) (Java 21)
*   **信令**: WebSocket
*   **实时通信**: WebRTC
    *   **发送端**: `getDisplayMedia` API
    *   **接收端**: `RTCPeerConnection` API
*   **前端**: HTML5, CSS3, Vanilla JavaScript (ES6+)

## 📜 许可证

本项目采用 **MIT 许可证**。详情请见 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---