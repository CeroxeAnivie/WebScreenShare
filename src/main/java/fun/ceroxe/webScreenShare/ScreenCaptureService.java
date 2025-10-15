package fun.ceroxe.webScreenShare; // 确保包名与你的项目结构匹配

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Service
public class ScreenCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(ScreenCaptureService.class);

    @Value("${screen.index:0}")
    private int screenIndex;

    @Value("${output.resolution:0}") // 0 for 720p, 1 for 1080p, 2 for original
    private int outputResolutionChoice;

    @Value("${output.fps:0}") // 0 for 30fps, 1 for 60fps, 2 for 120fps
    private int outputFpsChoice;

    @Autowired
    private ScreenShareWebSocketHandler webSocketHandler;

    // JavaCV 抓取器和录制器相关
    private volatile FFmpegFrameGrabber grabber; // volatile 保证可见性
    private volatile FFmpegFrameRecorder recorder; // volatile 保证可见性
    private volatile ByteArrayOutputStream baos; // volatile 保证可见性
    private int TARGET_FPS;
    private Duration captureInterval;
    private final AtomicBoolean capturing = new AtomicBoolean(false);
    private volatile Thread captureThread; // volatile 保证可见性

    private int OUTPUT_WIDTH;
    private int OUTPUT_HEIGHT;

    @PostConstruct
    public void initialize() throws Exception {
        // 根据选择设置目标帧率和捕获间隔
        switch (outputFpsChoice) {
            case 0:
                this.TARGET_FPS = 30;
                break;
            case 1:
                this.TARGET_FPS = 60;
                break;
            case 2:
                this.TARGET_FPS = 120;
                break;
            default:
                logger.warn("Invalid fps choice: {}. Defaulting to 30fps.", outputFpsChoice);
                this.TARGET_FPS = 30;
                break;
        }
        this.captureInterval = Duration.ofNanos(1_000_000_000L / TARGET_FPS);
        logger.info("Target frame rate set to: {} FPS", TARGET_FPS);

        logger.info("Initializing ScreenCaptureService with JavaCV for screen index: {}", screenIndex);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        Rectangle screenBounds;
        if (screenIndex >= 0 && screenIndex < screens.length) {
            screenBounds = screens[screenIndex].getDefaultConfiguration().getBounds();
            logger.info("Target screen bounds (logical pixels): {}", screenBounds);
        } else {
            logger.warn("Invalid screen index {}, defaulting to main screen (index 0).", screenIndex);
            screenBounds = new Rectangle(0, 0, 0, 0);
            if (screens.length > 0) {
                screenBounds = screens[0].getDefaultConfiguration().getBounds();
            } else {
                throw new RuntimeException("No screens found!");
            }
        }

        // 根据选择设置输出分辨率
        switch (outputResolutionChoice) {
            case 0: // 720p
                this.OUTPUT_WIDTH = 1280;
                this.OUTPUT_HEIGHT = 720;
                logger.info("Output resolution set to 720p.");
                break;
            case 1: // 1080p
                this.OUTPUT_WIDTH = 1920;
                this.OUTPUT_HEIGHT = 1080;
                logger.info("Output resolution set to 1080p.");
                break;
            case 2: // original
                // 预先计算分辨率（如果需要原始分辨率）
                FFmpegFrameGrabber tempGrabber = null;
                try {
                    String input = "desktop";
                    tempGrabber = new FFmpegFrameGrabber(input);
                    tempGrabber.setFormat("gdigrab");
                    tempGrabber.setOption("framerate", String.valueOf(TARGET_FPS));
                    tempGrabber.start();
                    this.OUTPUT_WIDTH = tempGrabber.getImageWidth();
                    this.OUTPUT_HEIGHT = tempGrabber.getImageHeight();
                    logger.info("Output resolution set to original capture resolution: {}x{}", OUTPUT_WIDTH, OUTPUT_HEIGHT);
                } finally {
                    if (tempGrabber != null) {
                        try {
                            tempGrabber.stop();
                            tempGrabber.release();
                        } catch (Exception e) {
                            logger.warn("Error stopping/releasing temporary grabber during init: {}", e.getMessage());
                        }
                    }
                }
                break;
            default: // 处理无效值，回退到 1080p
                logger.warn("Invalid resolution choice: {}. Defaulting to 1080p.", outputResolutionChoice);
                this.OUTPUT_WIDTH = 1920;
                this.OUTPUT_HEIGHT = 1080;
                logger.info("Output resolution set to 1080p (default).");
                break;
        }

        String input = "desktop";
        this.grabber = new FFmpegFrameGrabber(input);
        this.grabber.setFormat("gdigrab");
        this.grabber.setOption("framerate", String.valueOf(TARGET_FPS));
        this.grabber.start();

        this.baos = new ByteArrayOutputStream();
        this.recorder = new FFmpegFrameRecorder(baos, OUTPUT_WIDTH, OUTPUT_HEIGHT); // 使用确定的输出尺寸
        this.recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MJPEG);
        this.recorder.setVideoOption("q:v", "1");
        this.recorder.setVideoOption("qmin", "1");
        this.recorder.setVideoOption("qmax", "5");
        this.recorder.setFormat("mjpeg");
        this.recorder.setFrameRate(TARGET_FPS); // 设置录制帧率

        this.recorder.setPixelFormat(org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P);
        this.recorder.setVideoOption("color_range", "jpeg");
        this.recorder.setVideoOption("colorspace", "bt709");
        this.recorder.setVideoOption("color_trc", "bt709");
        this.recorder.setVideoOption("color_primaries", "bt709");

        // --- 构建过滤器链 ---
        StringBuilder filterBuilder = new StringBuilder();
        // Crop first (if not using original full desktop)
        filterBuilder.append("crop=").append(screenBounds.width).append(":").append(screenBounds.height)
                .append(":").append(screenBounds.x).append(":").append(screenBounds.y);

        // Scale second (only if output is not original)
        if (outputResolutionChoice != 2) {
            filterBuilder.append(",scale=").append(OUTPUT_WIDTH).append(":").append(OUTPUT_HEIGHT).append(":flags=lanczos");
        }
        String filterChain = filterBuilder.toString();
        this.recorder.setVideoOption("vf", filterChain);
        // -------------------------
        this.recorder.start();

        logger.info("Screen Capture Service initialized with JavaCV. Crop area: {}x{}@{}x{}, Output: {}x{} (Choice: {}), FPS: {}", screenBounds.width, screenBounds.height, screenBounds.x, screenBounds.y, OUTPUT_WIDTH, OUTPUT_HEIGHT, outputResolutionChoice == 0 ? "720p" : (outputResolutionChoice == 1 ? "1080p" : "Original"), TARGET_FPS);

        startCaptureThread();
    }

    private void startCaptureThread() {
        if (!capturing.compareAndSet(false, true)) {
            logger.warn("Capture thread already started.");
            return;
        }
        this.captureThread = new Thread(this::captureLoop, "JavaCVScreenCaptureThread-" + screenIndex);
        this.captureThread.setDaemon(false);
        this.captureThread.start();
    }

    private void captureLoop() { // 将捕获逻辑提取到独立方法
        logger.info("Screen capture thread started with JavaCV for screen index {} at {} FPS.", screenIndex, TARGET_FPS);
        final long targetFrameTimeNanos = captureInterval.toNanos();

        try {
            Frame frame;
            while (capturing.get()) {
                long startTime = System.nanoTime();

                frame = grabber.grabImage();
                if (frame == null) {
                    logger.warn("Grabbed frame is null, skipping...");
                    // 尝试短暂休眠避免空循环占用 CPU
                    Thread.sleep(1);
                    continue;
                }

                baos.reset();
                recorder.record(frame); // 应用 crop 和可能的 scale 过滤器
                byte[] encodedBytes = baos.toByteArray();

                // --- 直接发送给 WebSocket Handler ---
                webSocketHandler.sendFrameToAll(encodedBytes);
                // -----------------------------------

                long elapsedNanos = System.nanoTime() - startTime;
                if (elapsedNanos < targetFrameTimeNanos) {
                    try {
                        Thread.sleep(Math.max(0, (targetFrameTimeNanos - elapsedNanos) / 1_000_000),
                                (int) ((targetFrameTimeNanos - elapsedNanos) % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.info("Capture thread interrupted during sleep.");
                        break; // Exit loop
                    }
                } else {
                    // logger.debug("Capture/Encode took longer than interval: {} ns", elapsedNanos);
                }
            }
        } catch (Exception e) {
            logger.error("Critical error in JavaCV capture loop for screen index {}", screenIndex, e);
            // 可以考虑通知 WebSocket Handler 或设置状态
        } finally {
            logger.info("Screen capture thread stopped for screen index {}.", screenIndex);
            // 确保 capturing 标志被重置
            capturing.set(false);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Shutting down ScreenCaptureService for screen index {}...", screenIndex);
        // 停止捕获线程
        capturing.set(false);
        Thread currentCaptureThread = this.captureThread; // 读取 volatile 变量
        if (currentCaptureThread != null && currentCaptureThread.isAlive()) {
            currentCaptureThread.interrupt();
            try {
                currentCaptureThread.join(3000); // 等待最多 3 秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for capture thread to finish for screen index {}.", screenIndex);
            }
        }

        // 关闭 JavaCV 组件
        closeRecorder();
        closeGrabber();
        // baos 通常不需要显式关闭

        logger.info("ScreenCaptureService for screen index {} shut down.", screenIndex);
    }

    private void closeRecorder() {
        FFmpegFrameRecorder currentRecorder = this.recorder; // 读取 volatile 变量
        if (currentRecorder != null) {
            try {
                currentRecorder.stop();
                currentRecorder.release();
            } catch (Exception e) {
                logger.error("Error stopping/releasing recorder for screen index {}", screenIndex, e);
            }
        }
    }

    private void closeGrabber() {
        FFmpegFrameGrabber currentGrabber = this.grabber; // 读取 volatile 变量
        if (currentGrabber != null) {
            try {
                currentGrabber.stop();
                currentGrabber.release();
            } catch (Exception e) {
                logger.error("Error stopping/releasing grabber for screen index {}", screenIndex, e);
            }
        }
    }
}