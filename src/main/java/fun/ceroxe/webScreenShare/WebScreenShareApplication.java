package fun.ceroxe.webScreenShare;

import fun.ceroxe.webScreenShare.service.ScreenShareService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class WebScreenShareApplication {

    // --- NEW: Command-line argument parsing ---
    private static boolean DEBUG_MODE = false;

    public static void main(String[] args) {
        // Parse command-line arguments for options like --debug
        List<String> argList = Arrays.asList(args);
        DEBUG_MODE = argList.contains("--debug");
        if (DEBUG_MODE) {
            logDebug("Debug mode enabled via command line argument '--debug'.");
        }

        ConfigurableApplicationContext context = SpringApplication.run(WebScreenShareApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ScreenShareService screenShareService) {
        return args -> {
            Scanner scanner = new Scanner(System.in);
            System.out.print(getMessage("prompt.port")); // "请输入HTTP端口 (默认 8080): "
            String portInput = scanner.nextLine().trim();
            int port = 8080;
            if (!portInput.isEmpty()) {
                try {
                    port = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    System.err.println(getMessage("error.invalid_port")); // "无效的端口号，使用默认端口 8080。"
                }
            }
            System.setProperty("server.port", String.valueOf(port));
            logDebug(getMessage("info.server_starting_on_port", port)); // "服务器将在端口 {0} 上启动..."

            System.out.print(getMessage("prompt.password")); // "请输入访问密码 (按回车表示无密码): "
            String password = scanner.nextLine(); // Use nextLine() to capture empty input as well
            if (password != null && !password.trim().isEmpty()) {
                screenShareService.setPassword(password.trim());
                System.out.println(getMessage("info.password_set")); // "密码已设置。"
            } else {
                screenShareService.setPassword(null);
                System.out.println(getMessage("info.no_password")); // "未设置密码。"
            }
            scanner.close();

            // --- ENHANCED CONSOLE OUTPUT ---
            System.out.println("\n--- " + getMessage("title.server_started") + " ---"); // "--- 服务器已启动 ---"
            String localIP = "localhost";
            try {
                localIP = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                logDebug(getMessage("debug.cannot_get_ip")); // "无法获取本机IP，使用 localhost"
            }

            // URLs
            String baseUrlLocal = "http://localhost:" + port;
            String baseUrlLan = "http://" + localIP + ":" + port;
            String senderUrl = baseUrlLocal + "/sender.html";
            String receiverUrl = baseUrlLan + "/receiver.html"; // Prefer LAN URL for receiver

            // Check terminal capabilities
            boolean supportsColor = System.console() != null && System.getenv().get("TERM") != null && !System.getenv().get("TERM").equals("dumb");
            String passwordDisplay = (password == null || password.trim().isEmpty()) ? getMessage("term.none") : password.trim(); // "无"

            if (supportsColor) {
                // Colored output with hyperlinks (if supported by terminal)
                System.out.println(ansiGreen + getMessage("label.sender_page_local") + ansiReset + " \033]8;;" + senderUrl + "\033\\" + ansiUnderline + senderUrl + ansiReset + ansiUnderlineOff); // 发送端页面 (本地):
                System.out.println(ansiBlue + getMessage("label.receiver_page_lan") + ansiReset + " \033]8;;" + receiverUrl + "\033\\" + ansiUnderline + receiverUrl + ansiReset + ansiUnderlineOff); // 接收端页面 (局域网):
                System.out.println(ansiYellow + getMessage("label.access_password") + " '" + passwordDisplay + "'" + ansiReset); // 访问密码:
            } else {
                // Plain text output
                System.out.println(getMessage("label.sender_page_local") + " " + senderUrl); // 发送端页面 (本地):
                System.out.println(getMessage("label.receiver_page_lan") + " " + receiverUrl); // 接收端页面 (局域网):
                System.out.println(getMessage("label.access_password") + " '" + passwordDisplay + "'"); // 访问密码:
            }
            System.out.println("-------------------\n");
            // --- END ENHANCED CONSOLE OUTPUT ---
        };
    }

    // --- NEW: Utility methods for internationalization and debug logging ---
    private static final String ansiGreen = "\033[32m";
    private static final String ansiBlue = "\033[34m";
    private static final String ansiYellow = "\033[33m";
    private static final String ansiRed = "\033[31m";
    private static final String ansiReset = "\033[0m";
    private static final String ansiUnderline = "\033[4m";
    private static final String ansiUnderlineOff = "\033[24m";

    // Simple message bundle simulation (in a real app, use ResourceBundle)
    private static String getMessage(String key, Object... params) {
        // This is a simplified approach. In a full app, you'd load from properties files.
        String message;
        switch (key) {
            case "prompt.port": message = "请输入HTTP端口 (默认 8080): "; break;
            case "prompt.password": message = "请输入访问密码 (按回车表示无密码): "; break;
            case "error.invalid_port": message = "无效的端口号，使用默认端口 8080。"; break;
            case "info.server_starting_on_port": message = "服务器将在端口 " + params[0] + " 上启动，绑定到 0.0.0.0 (局域网可访问)。"; break;
            case "info.password_set": message = "密码已设置。"; break;
            case "info.no_password": message = "未设置密码。"; break;
            case "title.server_started": message = "服务器已启动"; break;
            case "debug.cannot_get_ip": message = "无法获取本机IP，使用 localhost"; break;
            case "label.sender_page_local": message = "发送端页面 (本地):"; break;
            case "label.receiver_page_lan": message = "接收端页面 (局域网):"; break;
            case "label.access_password": message = "访问密码:"; break;
            case "term.none": message = "无"; break;
            case "log.debug": message = "[DEBUG] " + params[0]; break; // Generic debug prefix
            default: message = key; // Fallback to key if not found
        }
        return message;
    }

    /**
     * Logs a message only if DEBUG_MODE is true.
     * Uses a generic debug prefix.
     * @param message The debug message to log.
     */
    public static void logDebug(String message) {
        if (DEBUG_MODE) {
            // Using System.err for debug logs is common to separate from stdout
            System.err.println(getMessage("log.debug", message)); // Prefixes with [DEBUG]
        }
    }
    // --- END NEW UTILITY METHODS ---
}