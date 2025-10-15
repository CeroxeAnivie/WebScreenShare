package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner; // 导入 Scanner

@SpringBootApplication
public class AutoScreenShareApplication {

    // ANSI 颜色代码 (用于美化控制台输出)
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    // 自定义颜色
    private static final String BRIGHT_GREEN = "\u001B[92m";
    private static final String BRIGHT_YELLOW = "\u001B[93m";
    private static final String BRIGHT_BLUE = "\u001B[94m";
    private static final String BRIGHT_CYAN = "\u001B[96m";

    // 颜色支持判断
    private static boolean isColorSupported() {
        String term = System.getenv("TERM");
        if (term != null) {
            return !term.equals("dumb") && !term.equals("unknown");
        }
        String ideaRunning = System.getProperty("idea.running");
        if (ideaRunning != null && ideaRunning.equals("true")) {
            return true;
        }
        String eclipseRunning = System.getProperty("eclipse.launcher");
        if (eclipseRunning != null) {
            return true;
        }
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            if (osName.contains("windows 10") || osName.contains("windows 11")) {
                return true;
            }
        }
        return false;
    }

    private static String colorIfSupported(String ansiCode) {
        return isColorSupported() ? ansiCode : "";
    }

    public static void main(String[] args) {
        boolean useColor = isColorSupported();

        // --- 解析启动参数 ---
        Map<String, String> parsedArgs = parseArguments(args);
        String passwordArg = parsedArgs.get("password");
        Integer portArg = parsedArgs.get("port") != null ? Integer.parseInt(parsedArgs.get("port")) : null;
        Integer screenArg = parsedArgs.get("screen") != null ? Integer.parseInt(parsedArgs.get("screen")) : null;
        Integer resolutionArg = parsedArgs.get("resolution") != null ? Integer.parseInt(parsedArgs.get("resolution")) : null;
        Integer fpsArg = parsedArgs.get("fps") != null ? Integer.parseInt(parsedArgs.get("fps")) : null;
        boolean hasDebugArg = parsedArgs.containsKey("debug");
        // -------------------

        Locale defaultLocale = Locale.getDefault();
        boolean isChinese = defaultLocale.getLanguage().toLowerCase().startsWith("zh");

        // --- 中英文消息 (使用条件颜色) ---
        String welcomeMsg = isChinese ?
                colorIfSupported(ANSI_CYAN) + "=== 欢迎使用高性能屏幕共享服务 ===\n   _____                                     \n" +
                        "  / ____|                                    \n" +
                        " | |        ___   _ __    ___   __  __   ___ \n" +
                        " | |       / _ \\ | '__|  / _ \\  \\ \\/ /  / _ \\\n" +
                        " | |____  |  __/ | |    | (_) |  >  <  |  __/\n" +
                        "  \\_____|  \\___| |_|     \\___/  /_/\\_\\  \\___|\n" +
                        "                                             " + colorIfSupported(ANSI_RESET) :
                colorIfSupported(ANSI_CYAN) + "=== Welcome to High-Performance Screen Sharing Service ===\n   _____                                     \n" +
                        "  / ____|                                    \n" +
                        " | |        ___   _ __    ___   __  __   ___ \n" +
                        " | |       / _ \\ | '__|  / _ \\  \\ \\/ /  / _ \\\n" +
                        " | |____  |  __/ | |    | (_) |  >  <  |  __/\n" +
                        "  \\_____|  \\___| |_|     \\___/  /_/\\_\\  \\___|\n" +
                        "                                             " + colorIfSupported(ANSI_RESET);

        String availableScreensMsg = isChinese ? "可用屏幕列表:" : "Available Screens:";
        String enterScreenIndexMsg = isChinese ? "请选择要共享的屏幕编号 (输入数字): " : "Select screen number to share (Enter number): ";
        String invalidIndexMsg = isChinese ? "错误: 无效编号。请输入 0 到 " : "Error: Invalid number. Please enter a number between 0 and ";
        String enterPortMsg = isChinese ? "请输入 HTTP 服务端口 (例如 8080): " : "Enter HTTP server port (e.g., 8080): ";
        String portRangeMsg = isChinese ? "错误: 端口必须在 1 到 65535 之间。" : "Error: Port must be between 1 and 65535.";
        String pleaseEnterNumberMsg = isChinese ? "错误: 请输入一个有效数字。" : "Error: Please enter a valid number.";
        String bindingToPortMsg = isChinese ? "服务将绑定到端口: " : "Service will bind to port: ";
        String selectedScreenMsg = isChinese ? "已选择屏幕: " : "Selected Screen: ";
        String enterResolutionMsg = isChinese ?
                "请选择输出分辨率:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "720p (节省带宽)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "1080p (平衡质量与性能)\n" + colorIfSupported(ANSI_YELLOW) + "  2) " + colorIfSupported(ANSI_RESET) + "原画输出 (最高质量)\n" + colorIfSupported(ANSI_GREEN) + "请输入 (0, 1 或 2): " + colorIfSupported(ANSI_RESET) :
                "Choose output resolution:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "720p (Saves bandwidth)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "1080p (Balance quality & performance)\n" + colorIfSupported(ANSI_YELLOW) + "  2) " + colorIfSupported(ANSI_RESET) + "Original output (Highest quality)\n" + colorIfSupported(ANSI_GREEN) + "Enter (0, 1, or 2): " + colorIfSupported(ANSI_RESET);
        String invalidResolutionMsg = isChinese ? "错误: 无效选择。请输入 0, 1 或 2。" : "Error: Invalid choice. Please enter 0, 1, or 2.";
        String resolution720pMsg = isChinese ? "720p 压缩" : "Compress to 720p";
        String resolution1080pMsg = isChinese ? "1080p 压缩" : "Compress to 1080p";
        String resolutionOriginalMsg = isChinese ? "原画输出" : "Original output";
        String selectedResolutionMsg = isChinese ? "已选择输出模式: " : "Selected output mode: ";
        String enterFpsMsg = isChinese ?
                "请选择输出帧率:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "30fps (流畅度与性能平衡)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "60fps (极致流畅体验)\n" + colorIfSupported(ANSI_YELLOW) + "  2) " + colorIfSupported(ANSI_RESET) + "120fps (超高速流畅体验)\n" + colorIfSupported(ANSI_GREEN) + "请输入 (0, 1 或 2): " + colorIfSupported(ANSI_RESET) :
                "Choose output frame rate:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "30fps (Balance smoothness & performance)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "60fps (Ultimate smoothness)\n" + colorIfSupported(ANSI_YELLOW) + "  2) " + colorIfSupported(ANSI_RESET) + "120fps (Ultra-high smoothness)\n" + colorIfSupported(ANSI_GREEN) + "Enter (0, 1, or 2): " + colorIfSupported(ANSI_RESET);
        String invalidFpsMsg = isChinese ? "错误: 无效选择。请输入 0, 1 或 2。" : "Error: Invalid choice. Please enter 0, 1, or 2.";
        String fps30Msg = "30fps";
        String fps60Msg = "60fps";
        String fps120Msg = "120fps";
        String selectedFpsMsg = isChinese ? "已选择帧率: " : "Selected frame rate: ";

        // --- 密码设置 ---
        String enterPasswordMsg = isChinese ?
                "请设置访问密码 (直接按回车跳过认证): " :
                "Please set an access password (Press Enter to skip authentication): ";
        String passwordSetMsg = isChinese ?
                "访问密码已设置。" :
                "Access password set.";
        String passwordSkippedMsg = isChinese ?
                "已跳过密码认证。" :
                "Password authentication skipped.";
        String authenticationDisabledMsg = isChinese ?
                "认证已禁用。" :
                "Authentication disabled.";
        // -------------------

        String applicationStartedMsg = isChinese ? "服务已在端口 " : "Service started successfully on port ";
        String sharingScreenIndexMsg = isChinese ? " 启动。正在共享屏幕编号 " : ". Sharing screen number ";
        String accessStreamMsg = isChinese ? "访问地址: http://localhost:" : "Access URL: http://localhost:";
        String pressCtrlCToStopMsg = isChinese ? "提示: 按 Ctrl+C 停止服务。" : "Tip: Press Ctrl+C to stop the service.";
        String configSummaryMsg = isChinese ? "\n--- 配置摘要 ---" : "\n--- Configuration Summary ---";
        String screenConfigMsg = isChinese ? "屏幕: " : "Screen: ";
        String resolutionConfigMsg = isChinese ? "分辨率: " : "Resolution: ";
        String fpsConfigMsg = isChinese ? "帧率: " : "Frame Rate: ";
        String portConfigMsg = isChinese ? "端口: " : "Port: ";
        String passwordConfigMsg = isChinese ? "访问密码: " : "Access Password: ";
        String startMsg = isChinese ? "正在启动服务..." : "Starting service...";
        String colorNotSupportedMsg = " (Color not supported by terminal)";
        String debugEnabledMsg = " (Debug enabled)";

        System.out.println(welcomeMsg);
        if (!useColor) {
            System.out.println(colorNotSupportedMsg);
        }
        if (hasDebugArg) {
            System.out.println(debugEnabledMsg);
        }
        System.out.println(); // 空行

        // --- 1. 获取显示器信息 ---
        GraphicsDevice[] screens;
        try {
            screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        } catch (HeadlessException e) {
            System.err.println(colorIfSupported(ANSI_RED) + "错误: 无法在无头环境中访问屏幕设备。" + colorIfSupported(ANSI_RESET));
            System.err.println(colorIfSupported(ANSI_RED) + e.getMessage() + colorIfSupported(ANSI_RESET));
            return; // Exit if headless
        }
        System.out.println(colorIfSupported(BRIGHT_CYAN) + availableScreensMsg + colorIfSupported(ANSI_RESET));
        for (int i = 0; i < screens.length; i++) {
            GraphicsDevice screen = screens[i];
            DisplayMode dm = screen.getDisplayMode();
            System.out.printf(colorIfSupported(ANSI_YELLOW) + "  编号 %d: " + colorIfSupported(ANSI_RESET) + "%s (%dx%d @ %dHz)%s%n",
                    i, screen.getIDstring(), dm.getWidth(), dm.getHeight(), dm.getRefreshRate(),
                    screen.getDefaultConfiguration().getBounds());
        }
        System.out.println(); // 空行

        // --- 2. 使用单一 Scanner 进行所有交互 ---
        String password;
        Integer port;
        Integer screenIndex;
        String fpsStr;
        Integer resolutionChoice;
        Integer fpsChoice;
        String resolutionStr;
        try (Scanner scanner = new Scanner(System.in)) {

            // --- 2.1. 选择屏幕 ---
            screenIndex = screenArg;
            if (screenIndex == null) {
                while (true) {
                    System.out.print(colorIfSupported(BRIGHT_GREEN) + enterScreenIndexMsg + colorIfSupported(ANSI_RESET));
                    try {
                        String input = scanner.nextLine().trim();
                        screenIndex = Integer.parseInt(input);
                        if (screenIndex >= 0 && screenIndex < screens.length) {
                            break;
                        } else {
                            System.out.println(colorIfSupported(ANSI_RED) + invalidIndexMsg + (screens.length - 1) + "." + colorIfSupported(ANSI_RESET));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                    }
                }
            } else {
                if (screenIndex < 0 || screenIndex >= screens.length) {
                    System.err.println(colorIfSupported(ANSI_RED) + "错误: 参数 --screen " + screenIndex + " 超出范围。" + colorIfSupported(ANSI_RESET));
                    return;
                }
            }
            System.out.println(colorIfSupported(BRIGHT_BLUE) + selectedScreenMsg + colorIfSupported(ANSI_WHITE) + screens[screenIndex].getIDstring() + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 2.2. 选择分辨率 ---
            resolutionChoice = resolutionArg;
            if (resolutionChoice == null) {
                while (true) {
                    System.out.print(enterResolutionMsg);
                    try {
                        String input = scanner.nextLine().trim();
                        resolutionChoice = Integer.parseInt(input);
                        if (resolutionChoice == 0 || resolutionChoice == 1 || resolutionChoice == 2) {
                            break;
                        } else {
                            System.out.println(colorIfSupported(ANSI_RED) + invalidResolutionMsg + colorIfSupported(ANSI_RESET));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                    }
                }
            } else {
                if (resolutionChoice != 0 && resolutionChoice != 1 && resolutionChoice != 2) {
                    System.err.println(colorIfSupported(ANSI_RED) + "错误: 参数 --resolution " + resolutionChoice + " 无效。" + colorIfSupported(ANSI_RESET));
                    return;
                }
            }
            resolutionStr = resolutionChoice == 0 ? resolution720pMsg : (resolutionChoice == 1 ? resolution1080pMsg : resolutionOriginalMsg);
            System.out.println(colorIfSupported(BRIGHT_BLUE) + selectedResolutionMsg + colorIfSupported(ANSI_WHITE) + resolutionStr + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 2.3. 选择帧率 ---
            fpsChoice = fpsArg;
            if (fpsChoice == null) {
                while (true) {
                    System.out.print(enterFpsMsg);
                    try {
                        String input = scanner.nextLine().trim();
                        fpsChoice = Integer.parseInt(input);
                        if (fpsChoice == 0 || fpsChoice == 1 || fpsChoice == 2) {
                            break;
                        } else {
                            System.out.println(colorIfSupported(ANSI_RED) + invalidFpsMsg + colorIfSupported(ANSI_RESET));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                    }
                }
            } else {
                if (fpsChoice != 0 && fpsChoice != 1 && fpsChoice != 2) {
                    System.err.println(colorIfSupported(ANSI_RED) + "错误: 参数 --fps " + fpsChoice + " 无效。" + colorIfSupported(ANSI_RESET));
                    return;
                }
            }
            fpsStr = fpsChoice == 0 ? fps30Msg : (fpsChoice == 1 ? fps60Msg : fps120Msg);
            System.out.println(colorIfSupported(BRIGHT_BLUE) + selectedFpsMsg + colorIfSupported(ANSI_WHITE) + fpsStr + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 2.4. 选择端口 ---
            port = portArg;
            if (port == null) {
                while (true) {
                    System.out.print(colorIfSupported(BRIGHT_GREEN) + enterPortMsg + colorIfSupported(ANSI_RESET));
                    try {
                        String input = scanner.nextLine().trim();
                        port = Integer.parseInt(input);
                        if (port > 0 && port < 65536) {
                            break;
                        } else {
                            System.out.println(colorIfSupported(ANSI_RED) + portRangeMsg + colorIfSupported(ANSI_RESET));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                    }
                }
            } else {
                if (port <= 0 || port >= 65536) {
                    System.err.println(colorIfSupported(ANSI_RED) + "错误: 参数 --port " + port + " 超出范围。" + colorIfSupported(ANSI_RESET));
                    return;
                }
            }
            System.out.println(colorIfSupported(BRIGHT_BLUE) + bindingToPortMsg + colorIfSupported(ANSI_WHITE) + port + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 2.5. 设置密码 ---
            password = passwordArg;
            if (password == null) {
                System.out.print(colorIfSupported(BRIGHT_GREEN) + enterPasswordMsg + colorIfSupported(ANSI_RESET));
                password = scanner.nextLine();
                if (password.isEmpty()) {
                    password = null;
                    System.out.println(colorIfSupported(BRIGHT_BLUE) + passwordSkippedMsg + colorIfSupported(ANSI_RESET));
                } else {
                    System.out.println(colorIfSupported(BRIGHT_BLUE) + passwordSetMsg + colorIfSupported(ANSI_RESET));
                }
            } else {
                if (password.isEmpty()) {
                    password = null;
                    System.out.println(colorIfSupported(BRIGHT_BLUE) + passwordSkippedMsg + colorIfSupported(ANSI_RESET));
                } else {
                    System.out.println(colorIfSupported(BRIGHT_BLUE) + passwordSetMsg + colorIfSupported(ANSI_RESET));
                }
            }
            System.out.println(); // 空行

        } // Scanner 在这里关闭
        // -------------------

        // --- 3. 显示配置摘要 (使用已获取的值) ---
        System.out.println(colorIfSupported(BRIGHT_CYAN) + configSummaryMsg + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_YELLOW) + screenConfigMsg + colorIfSupported(ANSI_WHITE) + screens[screenArg != null ? screenArg : (int) screenIndex].getIDstring() + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_YELLOW) + resolutionConfigMsg + colorIfSupported(ANSI_WHITE) + (resolutionArg != null ? (resolutionArg == 0 ? resolution720pMsg : (resolutionArg == 1 ? resolution1080pMsg : resolutionOriginalMsg)) : resolutionStr) + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_YELLOW) + fpsConfigMsg + colorIfSupported(ANSI_WHITE) + (fpsArg != null ? (fpsArg == 0 ? fps30Msg : (fpsArg == 1 ? fps60Msg : fps120Msg)) : fpsStr) + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_YELLOW) + portConfigMsg + colorIfSupported(ANSI_WHITE) + (portArg != null ? portArg : port) + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_YELLOW) + passwordConfigMsg + colorIfSupported(ANSI_WHITE) + (password != null ? "[Set]" : authenticationDisabledMsg) + colorIfSupported(ANSI_RESET));
        System.out.println(); // 空行

        System.out.print(colorIfSupported(BRIGHT_GREEN) + startMsg + colorIfSupported(ANSI_RESET));
        System.out.println(); // 换行

        // --- 4. 启动 Spring Boot 应用 ---
        int finalPort = portArg != null ? portArg : port;
        int finalScreenIndex = screenArg != null ? screenArg : screenIndex;
        int finalResolutionChoice = resolutionArg != null ? resolutionArg : resolutionChoice;
        int finalFpsChoice = fpsArg != null ? fpsArg : fpsChoice;

        System.setProperty("server.address", "0.0.0.0");
        System.setProperty("server.port", String.valueOf(finalPort));
        System.setProperty("screen.index", String.valueOf(finalScreenIndex));
        System.setProperty("output.resolution", String.valueOf(finalResolutionChoice));
        System.setProperty("output.fps", String.valueOf(finalFpsChoice));
        if (password != null) {
            System.setProperty("access.password", password);
        } else {
            System.setProperty("access.password", "");
        }

        if (hasDebugArg) {
            System.setProperty("logging.level.org.springframework", "DEBUG");
            System.setProperty("logging.level.org.springframework.boot", "DEBUG");
            System.setProperty("logging.level.org.springframework.web", "DEBUG");
            System.setProperty("logging.level.org.springframework.security", "DEBUG");
            System.setProperty("logging.level.reactor.netty", "DEBUG");
        } else {
            System.setProperty("logging.level.org.springframework", "WARN");
            System.setProperty("logging.level.org.springframework.boot", "WARN");
            System.setProperty("logging.level.org.springframework.web", "WARN");
            System.setProperty("logging.level.org.springframework.security", "WARN");
            System.setProperty("logging.level.reactor.netty", "WARN");
        }
        System.setProperty("logging.level.fun.ceroxe.webScreenShare", "INFO");

        SpringApplication app = new SpringApplication(AutoScreenShareApplication.class);
        app.setDefaultProperties(java.util.Map.of(
                "server.address", "0.0.0.0",
                "server.port", String.valueOf(finalPort),
                "screen.index", String.valueOf(finalScreenIndex),
                "output.resolution", String.valueOf(finalResolutionChoice),
                "output.fps", String.valueOf(finalFpsChoice),
                "access.password", password != null ? password : ""
        ));

        ConfigurableApplicationContext context = app.run(args);

        System.out.println(); // 空行
        System.out.println(colorIfSupported(BRIGHT_GREEN) + applicationStartedMsg + finalPort + sharingScreenIndexMsg + finalScreenIndex + "." + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_YELLOW) + accessStreamMsg + finalPort + colorIfSupported(ANSI_RESET));
        System.out.println(colorIfSupported(BRIGHT_CYAN) + pressCtrlCToStopMsg + colorIfSupported(ANSI_RESET));
        System.out.println(); // 空行

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down gracefully...");
            if (context != null) {
                context.close();
            }
            System.out.println("Application stopped.");
        }));
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> parsedArgs = new java.util.HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                String key = parts[0];
                String value = (parts.length > 1) ? parts[1] : (i + 1 < args.length && !args[i + 1].startsWith("--")) ? args[i + 1] : "true";
                if ("password".equals(key) || "port".equals(key) || "screen".equals(key) || "resolution".equals(key) || "fps".equals(key)) {
                    parsedArgs.put(key, value);
                } else if ("debug".equals(key)) {
                    parsedArgs.put(key, "true");
                }
            }
        }
        return parsedArgs;
    }
}