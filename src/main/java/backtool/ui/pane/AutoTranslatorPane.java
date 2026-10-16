package backtool.ui.pane;

import backtool.service.ConfigStore;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 * 自动翻译组件
 * 密钥从ConfigStore配置文件统一读取
 */
public class AutoTranslatorPane extends BorderPane {

    // 从配置文件读取百度翻译密钥，不再硬编码
    private static final String BAIDU_APP_ID = ConfigStore.PROPERTIES.getProperty("baidu.trans.appid", "");
    private static final String BAIDU_SECRET = ConfigStore.PROPERTIES.getProperty("baidu.trans.secret", "");

    // 状态标记
    private volatile boolean isListening = false;
    private Thread listenThread = null;
    private String lastCopy = "";
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Pattern BAIDU_PATTERN = Pattern.compile("\"dst\":\"([^\"]+)\"");
    // Unicode转义正则，用于修复乱码
    private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    // UI组件
    private Button btnStart;
    private Button btnStop;
    private Label statusLabel;

    public AutoTranslatorPane() {
        setPadding(new Insets(10));

        // 顶部按钮区
        HBox buttonBox = new HBox(10);
        buttonBox.setPadding(new Insets(0, 0, 10, 0));

        btnStart = new Button("开始监听");
        btnStart.setPrefWidth(120);
        btnStart.setOnAction(e -> startListen());

        btnStop = new Button("停止监听");
        btnStop.setPrefWidth(120);
        btnStop.setDisable(true);
        btnStop.setOnAction(e -> stopListen());

        buttonBox.getChildren().addAll(btnStart, btnStop);
        setTop(buttonBox);

        // 中间状态区
        VBox statusBox = new VBox(10);
        statusLabel = new Label("✅ 翻译工具已就绪，点击「开始监听」启用");
        statusLabel.setFont(Font.font(14));
        statusLabel.setStyle("-fx-text-fill: #333;");
        statusBox.getChildren().add(statusLabel);
        setCenter(statusBox);
    }

    private void startListen() {
        // 先检查配置是否填写
        if (BAIDU_APP_ID.isBlank() || BAIDU_SECRET.isBlank()) {
            updateStatus("⚠️ 请先在config.properties中配置百度翻译密钥");
            return;
        }

        if (isListening) {
            updateStatus("⚠️ 已经在监听中了");
            return;
        }

        isListening = true;
        btnStart.setDisable(true);
        btnStop.setDisable(false);
        updateStatus("✅ 已开启自动翻译，复制英文即可自动翻译+朗读");

        // 启动剪贴板监听线程
        listenThread = new Thread(() -> {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            while (isListening && !Thread.currentThread().isInterrupted()) {
                try {
                    Transferable t = cb.getContents(null);
                    if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                        // 只处理英文，避免重复触发
                        if (!text.equals(lastCopy) && text.matches(".*[a-zA-Z]{3,}.*")) {
                            lastCopy = text;
                            translate(text);
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception ignored) {}
            }
        });
        listenThread.start();
    }

    /**
     * 停止监听剪贴板
     */
    private void stopListen() {
        if (!isListening) {
            updateStatus("⚠️ 还没开启监听呢");
            return;
        }

        isListening = false;
        if (listenThread != null) {
            listenThread.interrupt();
            listenThread = null;
        }

        btnStart.setDisable(false);
        btnStop.setDisable(true);
        updateStatus("⏹️ 已关闭自动翻译");
    }

    /**
     * 核心翻译+朗读逻辑
     * ✅ 修复乱码：强制UTF-8编码 + Unicode转义解析
     */
    private void translate(String text) {
        try {
            // 1. 系统原生朗读
            Runtime.getRuntime().exec(new String[]{"say", text});

            // 2. 百度翻译
            String salt = String.valueOf(System.currentTimeMillis());
            String sign = md5(BAIDU_APP_ID + text + salt + BAIDU_SECRET);

            String url = String.format(
                    "https://fanyi-api.baidu.com/api/trans/vip/translate?q=%s&from=en&to=zh&appid=%s&salt=%s&sign=%s",
                    URLEncoder.encode(text, StandardCharsets.UTF_8),
                    BAIDU_APP_ID,
                    salt,
                    sign
            );

            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder().uri(java.net.URI.create(url)).build(),
                    // ✅ 强制用UTF-8读取响应，修复第一层乱码
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            // 提取翻译结果
            Matcher matcher = BAIDU_PATTERN.matcher(resp.body());
            String result = "翻译失败";
            if (matcher.find()) {
                result = matcher.group(1);
                // ✅ 解析Unicode转义，修复第二层乱码
                result = decodeUnicode(result);
            }

            // 3. 右上角弹窗显示结果，3秒自动消失
            showPopup(result);
            updateStatus("🔄 翻译完成: " + result);

        } catch (Exception e) {
            updateStatus("⚠️ 翻译失败：" + e.getMessage());
        }
    }

    /**
     * 百度翻译MD5签名算法（官方标准）
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 更新状态文字
     */
    private void updateStatus(String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    /**
     * 右上角翻译结果弹窗
     * ✅ 修复乱码：指定系统中文字体
     */
    private void showPopup(String text) {
        SwingUtilities.invokeLater(() -> {
            JWindow win = new JWindow();
            win.setSize(360, 70);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            win.setLocation(screen.width - 380, 30);

            // ✅ 强制用系统中文字体，避免Swing默认字体乱码
            JLabel label = new JLabel("<html><body style='padding:10px;font-size:14px;font-family:PingFang SC,Hiragino Sans GB,Microsoft YaHei,sans-serif'>" + text + "</body></html>");
            win.add(label);
            win.setAlwaysOnTop(true);
            win.setVisible(true);
            new Timer(3000, e -> win.dispose()).start();
        });
    }

    /**
     * ✅ 解析Unicode转义字符，彻底修复乱码
     * 把百度返回的\u4f60\u597d转成中文"你好"
     */
    private String decodeUnicode(String input) {
        Matcher matcher = UNICODE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            char c = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, String.valueOf(c));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}