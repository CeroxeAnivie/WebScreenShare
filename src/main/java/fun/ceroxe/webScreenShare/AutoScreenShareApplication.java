package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;
import java.util.Locale;
import java.util.Scanner;

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
        // 检查环境变量 TERM
        String term = System.getenv("TERM");
        if (term != null) {
            // 常见的支持颜色的 TERM 值
            return !term.equals("dumb") && !term.equals("unknown");
        }

        // 检查是否在 IDE 中运行 (通常 IDE 的控制台支持颜色)
        String ideaRunning = System.getProperty("idea.running");
        if (ideaRunning != null && ideaRunning.equals("true")) {
            return true;
        }

        // 检查是否在 Eclipse 中运行 (通常 Eclipse 的控制台支持颜色)
        String eclipseRunning = System.getProperty("eclipse.launcher");
        if (eclipseRunning != null) {
            return true;
        }

        // 检查操作系统 (Windows 10 1511 及以上版本的 CMD 支持 ANSI)
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            String osVersion = System.getProperty("os.version");
            try {
                // 简单检查 Windows 版本，10.0.10586 (1511) 是一个关键版本
                // 但更简单的方法是检查是否为 Windows 10/11
                // 这里做一个较保守的判断：如果是 Windows 10/11，且未明确设置 TERM=dumb，则认为支持
                if (osName.contains("windows 10") || osName.contains("windows 11")) {
                    // Windows 10/11 默认 CMD 通常支持，除非用户特意设置
                    // 也可以尝试启用 Windows ANSI 支持，但这比较复杂
                    // 这里先假设 10/11 支持
                    return true;
                }
                // 对于 Windows 7/8，CMD 默认不支持 ANSI，除非使用特殊工具
                // 这里可以更保守地返回 false，或者只检查 TERM
                // 最简单还是依赖 TERM
            } catch (Exception e) {
                // 如果解析版本出错，也依赖 TERM
            }
        }

        // 如果 TERM 不存在或无法判断，且不是在 IDE 中，可以检查 System.console()
        // 但 System.console() != null 不能完全保证支持颜色
        // return System.console() != null;

        // 最保守的策略：如果 TERM 未设置或为 dumb，则不使用颜色
        // 否则，假设支持 (这在大多数现代终端中是安全的)
        return false; // 如果 TERM 存在（即使是 "windows-ansi" 或其他），则认为支持
    }

    // 根据颜色支持情况返回颜色代码或空字符串
    private static String colorIfSupported(String ansiCode) {
        return isColorSupported() ? ansiCode : "";
    }

    public static void main(String[] args) {
        boolean useColor = isColorSupported();
        // System.out.println("Color supported: " + useColor); // Debug line

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
        String enterScreenIndexMsg = isChinese ? "请选择要共享的屏幕索引 (输入数字): " : "Select screen index to share (Enter number): ";
        String invalidIndexMsg = isChinese ? "错误: 无效索引。请输入 0 到 " : "Error: Invalid index. Please enter a number between 0 and ";
        String enterPortMsg = isChinese ? "请输入 HTTP 服务端口 (例如 8080): " : "Enter HTTP server port (e.g., 8080): ";
        String portRangeMsg = isChinese ? "错误: 端口必须在 1 到 65535 之间。" : "Error: Port must be between 1 and 65535.";
        String pleaseEnterNumberMsg = isChinese ? "错误: 请输入一个有效数字。" : "Error: Please enter a valid number.";
        String bindingToPortMsg = isChinese ? "服务将绑定到端口: " : "Service will bind to port: ";
        String selectedScreenMsg = isChinese ? "已选择屏幕: " : "Selected Screen: ";
        String enterResolutionMsg = isChinese ?
                "请选择输出分辨率:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "1080p 压缩 (节省带宽)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "原画输出 (最高质量)\n" + colorIfSupported(ANSI_GREEN) + "请输入 (0 或 1): " + colorIfSupported(ANSI_RESET) :
                "Choose output resolution:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "Compress to 1080p (Saves bandwidth)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "Original output (Highest quality)\n" + colorIfSupported(ANSI_GREEN) + "Enter (0 or 1): " + colorIfSupported(ANSI_RESET);
        String invalidResolutionMsg = isChinese ? "错误: 无效选择。请输入 0 或 1。" : "Error: Invalid choice. Please enter 0 or 1.";
        String resolution1080pMsg = isChinese ? "1080p 压缩" : "Compress to 1080p";
        String resolutionOriginalMsg = isChinese ? "原画输出" : "Original output";
        String selectedResolutionMsg = isChinese ? "已选择输出模式: " : "Selected output mode: ";
        String enterFpsMsg = isChinese ?
                "请选择输出帧率:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "30fps (流畅度与性能平衡)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "60fps (极致流畅体验)\n" + colorIfSupported(ANSI_GREEN) + "请输入 (0 或 1): " + colorIfSupported(ANSI_RESET) :
                "Choose output frame rate:\n" + colorIfSupported(ANSI_YELLOW) + "  0) " + colorIfSupported(ANSI_RESET) + "30fps (Balance smoothness & performance)\n" + colorIfSupported(ANSI_YELLOW) + "  1) " + colorIfSupported(ANSI_RESET) + "60fps (Ultimate smoothness)\n" + colorIfSupported(ANSI_GREEN) + "Enter (0 or 1): " + colorIfSupported(ANSI_RESET);
        String invalidFpsMsg = isChinese ? "错误: 无效选择。请输入 0 或 1。" : "Error: Invalid choice. Please enter 0 or 1.";
        String fps30Msg = "30fps";
        String fps60Msg = "60fps";
        String selectedFpsMsg = isChinese ? "已选择帧率: " : "Selected frame rate: ";
        String applicationStartedMsg = isChinese ? "服务已在端口 " : "Service started successfully on port ";
        String sharingScreenIndexMsg = isChinese ? " 启动。正在共享屏幕索引 " : ". Sharing screen index ";
        String accessStreamMsg = isChinese ? "访问地址: http://localhost:" : "Access URL: http://localhost:";
        String pressCtrlCToStopMsg = isChinese ? "提示: 按 Ctrl+C 停止服务。" : "Tip: Press Ctrl+C to stop the service.";
        String loadingMsg = isChinese ? "正在加载..." : "Loading...";
        String configSummaryMsg = isChinese ? "\n--- 配置摘要 ---" : "\n--- Configuration Summary ---";
        String screenConfigMsg = isChinese ? "屏幕: " : "Screen: ";
        String resolutionConfigMsg = isChinese ? "分辨率: " : "Resolution: ";
        String fpsConfigMsg = isChinese ? "帧率: " : "Frame Rate: ";
        String portConfigMsg = isChinese ? "端口: " : "Port: ";
        String startMsg = isChinese ? "正在启动服务..." : "Starting service...";
        String colorNotSupportedMsg = " (Color not supported by terminal)";
        // -------------------

        System.out.println(welcomeMsg);
        if (!useColor) {
            System.out.println(colorNotSupportedMsg);
        }
        System.out.println(); // 空行

        try (Scanner scanner = new Scanner(System.in)) {
            // --- 1. 获取显示器信息并让用户选择 ---
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
                System.out.printf(colorIfSupported(ANSI_YELLOW) + "  Index %d: " + colorIfSupported(ANSI_RESET) + "%s (%dx%d @ %dHz)%s%n",
                        i, screen.getIDstring(), dm.getWidth(), dm.getHeight(), dm.getRefreshRate(),
                        screen.getDefaultConfiguration().getBounds());
            }
            System.out.println(); // 空行

            int screenIndex;
            while (true) {
                System.out.print(colorIfSupported(BRIGHT_GREEN) + enterScreenIndexMsg + colorIfSupported(ANSI_RESET));
                try {
                    screenIndex = Integer.parseInt(scanner.nextLine().trim());
                    if (screenIndex >= 0 && screenIndex < screens.length) {
                        break;
                    } else {
                        System.out.println(colorIfSupported(ANSI_RED) + invalidIndexMsg + (screens.length - 1) + "." + colorIfSupported(ANSI_RESET));
                    }
                } catch (NumberFormatException e) {
                    System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                }
            }
            System.out.println(colorIfSupported(BRIGHT_BLUE) + selectedScreenMsg + colorIfSupported(ANSI_WHITE) + screens[screenIndex].getIDstring() + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 2. 让用户选择分辨率 ---
            int resolutionChoice;
            while (true) {
                System.out.print(enterResolutionMsg);
                try {
                    resolutionChoice = Integer.parseInt(scanner.nextLine().trim());
                    if (resolutionChoice == 0 || resolutionChoice == 1) {
                        break;
                    } else {
                        System.out.println(colorIfSupported(ANSI_RED) + invalidResolutionMsg + colorIfSupported(ANSI_RESET));
                    }
                } catch (NumberFormatException e) {
                    System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                }
            }
            String resolutionStr = resolutionChoice == 0 ? resolution1080pMsg : resolutionOriginalMsg;
            System.out.println(colorIfSupported(BRIGHT_BLUE) + selectedResolutionMsg + colorIfSupported(ANSI_WHITE) + resolutionStr + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 3. 让用户选择帧率 ---
            int fpsChoice;
            while (true) {
                System.out.print(enterFpsMsg);
                try {
                    fpsChoice = Integer.parseInt(scanner.nextLine().trim());
                    if (fpsChoice == 0 || fpsChoice == 1) {
                        break;
                    } else {
                        System.out.println(colorIfSupported(ANSI_RED) + invalidFpsMsg + colorIfSupported(ANSI_RESET));
                    }
                } catch (NumberFormatException e) {
                    System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                }
            }
            String fpsStr = fpsChoice == 0 ? fps30Msg : fps60Msg;
            System.out.println(colorIfSupported(BRIGHT_BLUE) + selectedFpsMsg + colorIfSupported(ANSI_WHITE) + fpsStr + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 4. 让用户输入端口 ---
            int port;
            while (true) {
                System.out.print(colorIfSupported(BRIGHT_GREEN) + enterPortMsg + colorIfSupported(ANSI_RESET));
                try {
                    port = Integer.parseInt(scanner.nextLine().trim());
                    if (port > 0 && port < 65536) {
                        break;
                    } else {
                        System.out.println(colorIfSupported(ANSI_RED) + portRangeMsg + colorIfSupported(ANSI_RESET));
                    }
                } catch (NumberFormatException e) {
                    System.out.println(colorIfSupported(ANSI_RED) + pleaseEnterNumberMsg + colorIfSupported(ANSI_RESET));
                }
            }
            System.out.println(colorIfSupported(BRIGHT_BLUE) + bindingToPortMsg + colorIfSupported(ANSI_WHITE) + port + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            // --- 5. 显示配置摘要 ---
            System.out.println(colorIfSupported(BRIGHT_CYAN) + configSummaryMsg + colorIfSupported(ANSI_RESET));
            System.out.println(colorIfSupported(BRIGHT_YELLOW) + screenConfigMsg + colorIfSupported(ANSI_WHITE) + screens[screenIndex].getIDstring() + colorIfSupported(ANSI_RESET));
            System.out.println(colorIfSupported(BRIGHT_YELLOW) + resolutionConfigMsg + colorIfSupported(ANSI_WHITE) + resolutionStr + colorIfSupported(ANSI_RESET));
            System.out.println(colorIfSupported(BRIGHT_YELLOW) + fpsConfigMsg + colorIfSupported(ANSI_WHITE) + fpsStr + colorIfSupported(ANSI_RESET));
            System.out.println(colorIfSupported(BRIGHT_YELLOW) + portConfigMsg + colorIfSupported(ANSI_WHITE) + port + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

            System.out.print(colorIfSupported(BRIGHT_GREEN) + startMsg + colorIfSupported(ANSI_RESET));
            System.out.println(); // 换行

            // --- 6. 启动 Spring Boot 应用 ---
            System.setProperty("server.port", String.valueOf(port));
            System.setProperty("screen.index", String.valueOf(screenIndex));
            System.setProperty("output.resolution", String.valueOf(resolutionChoice));
            System.setProperty("output.fps", String.valueOf(fpsChoice));

            System.setProperty("logging.level.org.springframework", "WARN");
            System.setProperty("logging.level.org.springframework.boot", "WARN");
            System.setProperty("logging.level.org.springframework.web", "WARN");
            System.setProperty("logging.level.org.springframework.security", "WARN");
            System.setProperty("logging.level.reactor.netty", "WARN");
            System.setProperty("logging.level.fun.ceroxe.webScreenShare", "INFO");

            SpringApplication app = new SpringApplication(AutoScreenShareApplication.class);
            app.setDefaultProperties(java.util.Map.of(
                    "server.port", String.valueOf(port),
                    "screen.index", String.valueOf(screenIndex),
                    "output.resolution", String.valueOf(resolutionChoice),
                    "output.fps", String.valueOf(fpsChoice)
            ));
            app.run(args);

            System.out.println(); // 空行
            System.out.println(colorIfSupported(BRIGHT_GREEN) + applicationStartedMsg + port + sharingScreenIndexMsg + screenIndex + "." + colorIfSupported(ANSI_RESET));
            System.out.println(colorIfSupported(BRIGHT_YELLOW) + accessStreamMsg + port + colorIfSupported(ANSI_RESET));
            System.out.println(colorIfSupported(BRIGHT_CYAN) + pressCtrlCToStopMsg + colorIfSupported(ANSI_RESET));
            System.out.println(); // 空行

        }
    }
}