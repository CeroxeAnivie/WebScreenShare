# 🖥️ Auto Screen Share (WebSocket + JavaCV)

![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)
![License](https://img.shields.io/badge/License-MIT-blue)
![Status](https://img.shields.io/badge/Status-Stable-success)

---

## 🚀 简介

**Auto Screen Share** 是一个基于 **Spring Boot + JavaCV + WebSocket** 的高性能屏幕共享服务，支持多显示器选择、帧率/分辨率可配置、终端彩色输出与国际化提示。  
通过浏览器即可实时查看屏幕画面。

---

## ✨ 特性

- 🧩 **多语言支持**：根据系统语言自动切换中英文提示  
- 🖼️ **多屏幕选择**：支持选择任意显示器共享  
- ⚙️ **自定义参数**：端口、分辨率（1080p/原画）、帧率（30/60fps）可自定义  
- ⚡ **高性能编码**：基于 JavaCV + FFmpeg 实时捕获与编码  
- 🌐 **WebSocket 推流**：浏览器端零延迟显示 MJPEG 流  
- 🧠 **彩色终端检测**：自动判断控制台是否支持 ANSI 颜色输出  
- 🪶 **轻量部署**：单文件 JAR 运行，无需额外前端服务

---

## 🧱 技术栈

| 模块 | 技术 |
|------|------|
| 后端框架 | Spring Boot |
| 视频捕获 | JavaCV / FFmpeg |
| 通信方式 | WebSocket |
| 依赖管理 | Maven |
| 语言 | Java 21+ |

---

## ⚡ 快速开始

### 1️⃣ 克隆项目
```bash
git clone https://github.com/yourname/AutoScreenShare.git
cd AutoScreenShare
```

### 2️⃣ 构建项目
```bash
mvn clean package
```

### 3️⃣ 运行应用
```bash
java -jar target/AutoScreenShare-1.0.jar
```

程序启动后，会出现交互式控制台：  
- 选择要共享的屏幕索引  
- 选择分辨率模式（1080p 或原画）  
- 选择帧率（30fps 或 60fps）  
- 输入 HTTP 服务端口  

运行成功后可访问：  
👉 `http://localhost:8080`

---

## 🧩 项目结构

```
fun.ceroxe.webScreenShare
 ├── AutoScreenShareApplication.java     # 启动入口
 ├── ScreenCaptureService.java           # 屏幕捕获与推流核心逻辑
 ├── ScreenShareWebSocketHandler.java    # WebSocket 连接管理
 ├── WebSocketConfig.java                # WebSocket 配置类
 └── ScreenShareController.java          # 简易网页前端控制器
```

---

## ⚙️ 依赖

主要依赖包括：

- `org.bytedeco:javacv`
- `org.springframework.boot:spring-boot-starter-websocket`
- `org.springframework.boot:spring-boot-starter-web`
- `jakarta.annotation:jakarta.annotation-api`

---

## 🧾 许可证

本项目使用 [MIT License](https://opensource.org/licenses/MIT) 进行开源。

---

## 💡 作者

**Ceroxe** 
📧 联系方式：QQ：1591117599
📅 项目版本：v1.0 Stable  

---

> 🖋️ *“代码让世界共享。”* — Ceroxe
