package fun.ceroxe.webScreenShare;

import fun.ceroxe.webScreenShare.service.ScreenShareService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class WebScreenShareApplication {

    // --- Command-line argument parsing ---
    private static boolean DEBUG_MODE = false;
    private static String passwordInput = null;

    public static void main(String[] args) {
        List<String> argList = Arrays.asList(args);
        DEBUG_MODE = argList.contains("--debug");

        if (DEBUG_MODE) {
            logDebug("Debug mode enabled via command line argument '--debug'.");
        }

        // --- Parse port from command line or prompt ---
        int port = 8080; // Default port
        Scanner scanner = new Scanner(System.in);

        // Check for --port argument
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[i + 1]);
                    logDebug("Port set via command line argument '--port " + port + "'.");
                } catch (NumberFormatException e) {
                    System.err.println("无效的端口号 '" + args[i + 1] + "'，使用默认端口 8080。");
                    port = 8080;
                }
                break;
            }
        }

        // If port wasn't set via args, prompt the user
        if (port == 8080 && !argList.contains("--port")) {
            System.out.print("请输入HTTP端口 (默认 8080): ");
            String portInput = scanner.nextLine().trim();
            if (!portInput.isEmpty()) {
                try {
                    port = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    System.err.println("无效的端口号，使用默认端口 8080。");
                }
            }
        }

        System.setProperty("server.port", String.valueOf(port));
        System.out.println("服务器将在端口 " + port + " 上启动，绑定到 0.0.0.0 (局域网可访问)。");

        // --- Parse password from command line or prompt ---
        // Check for --password argument
        for (int i = 0; i < args.length; i++) {
            if ("--password".equals(args[i]) && i + 1 < args.length) {
                passwordInput = args[i + 1];
                logDebug("Password set via command line argument '--password'.");
                break;
            }
        }

        // If password wasn't set via args, prompt the user
        if (passwordInput == null) {
            System.out.print("请输入访问密码 (按回车表示无密码): ");
            passwordInput = scanner.nextLine().trim();
            if (!passwordInput.isEmpty()) {
                System.out.println("密码已设置。");
            } else {
                System.out.println("未设置密码。");
            }
        } else {
            // If password was provided via args, confirm it (optional, can be removed for silence)
            System.out.println("密码已通过命令行参数设置。");
        }

        ConfigurableApplicationContext context = SpringApplication.run(WebScreenShareApplication.class, args);

        ScreenShareService screenShareService = context.getBean(ScreenShareService.class);
        if (passwordInput != null && !passwordInput.isEmpty()) {
            screenShareService.setPassword(passwordInput);
        }

        scanner.close();
    }

    @Bean
    public CommandLineRunner commandLineRunner(ScreenShareService screenShareService) {
        return args -> {
            System.out.println("\n--- 服务器已启动 ---");
            String localIP = "localhost";
            try {
                localIP = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                logDebug("无法获取本机IP，使用 localhost");
            }

            String serverPortStr = System.getProperty("server.port", "8080");
            int serverPort;
            try {
                serverPort = Integer.parseInt(serverPortStr);
            } catch (NumberFormatException e) {
                serverPort = 8080;
                System.err.println("无法解析 server.port 系统属性 '" + serverPortStr + "'，使用 8080 作为显示端口。");
            }

            String baseUrlLocal = "http://localhost:" + serverPort;
            String baseUrlLan = "http://" + localIP + ":" + serverPort;
            String senderUrl = baseUrlLocal + "/sender.html";

            String passwordDisplay = "无";
            if (passwordInput != null && !passwordInput.isEmpty()) {
                passwordDisplay = passwordInput;
            }

            System.out.println("-------------------\n");
            System.out.println("发送端页面 (本地): " + senderUrl);
            System.out.println("接收端页面 (局域网): " + baseUrlLan);
            System.out.println("访问密码: " + passwordDisplay);
            System.out.println("-------------------\n");
        };
    }

    public static void logDebug(String message) {
        if (DEBUG_MODE) {
            System.err.println("[DEBUG] " + message);
        }
    }

    @Controller
    static class RootRedirectController {

        @GetMapping("/")
        public String redirectToReceiver() {
            logDebug("Root path accessed, redirecting to /receiver.html");
            return "redirect:/receiver.html";
        }
    }
}