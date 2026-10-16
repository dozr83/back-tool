package backtool.ui.pane;

import backtool.ui.component.FxKit;
import backtool.service.ConfigStore;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.TreeMap;

/**
 * JavaFX 一键脚本执行器
 * 对标 Python tkinter 版本，功能完全一致
 */
public class ScriptExecutorPane extends BorderPane {

    // ===================== 一键配置区（和你Python完全一样） =====================
    // 改为从配置文件读取
    private List<ScriptItem> SCRIPTS = new ArrayList<>();
    // 【新增】外部Python脚本根目录
    private File externalScriptRootDir;

    // 界面组件
    private FxKit.LogTextArea outputArea;

    public ScriptExecutorPane() {
        // 加载配置
        loadScriptsFromConfig();

        setPadding(new Insets(5));

        // ========== 顶部：自动换行按钮面板（对标Python FlowFrame） ==========
        FlowPane buttonPane = new FlowPane();
        buttonPane.setHgap(8);
        buttonPane.setVgap(8);
        buttonPane.setPadding(new Insets(5));

        for (ScriptItem item : SCRIPTS) {
            Button btn = new Button(item.name());
            btn.setPrefWidth(160);
            btn.setOnAction(e -> runCommand(item.cmd()));
            buttonPane.getChildren().add(btn);
        }

        // 按钮区 【不滚动】，直接放上去
        setTop(buttonPane);

        // ========== 底部：日志输出区域 ==========
        outputArea = new FxKit.LogTextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-font-family: 'Menlo,Monospace'; -fx-font-size: 13px;");
        setCenter(outputArea);
    }

    // ===================== 从配置文件加载脚本（支持跳号、无上限、自动排序） =====================
    private void loadScriptsFromConfig() {
        SCRIPTS.clear();
        Properties mainProps = ConfigStore.PROPERTIES;

        // 1. 读取主配置：外部脚本根目录、外部配置文件名
        String scriptDirStr = mainProps.getProperty("python.script.dir");
        String confFileName = mainProps.getProperty("python.script.config", "script.conf");

        // 校验外部根目录
        if (scriptDirStr == null || scriptDirStr.isBlank()) {
            appendText("[ERROR] 主配置缺失 python.script.dir\n");
            return;
        }
        externalScriptRootDir = new File(scriptDirStr);
        if (!externalScriptRootDir.exists() || !externalScriptRootDir.isDirectory()) {
            appendText("[ERROR] 外部脚本目录不存在：" + scriptDirStr + "\n");
            return;
        }

        // 拼接外部配置文件完整路径
        Path externalConfPath = Paths.get(externalScriptRootDir.getAbsolutePath(), confFileName);
        if (!Files.exists(externalConfPath)) {
            appendText("[ERROR] 外部配置文件不存在：" + externalConfPath + "\n");
            return;
        }

        // 2. 加载外部 script.conf（UTF-8，与 ConfigStore 读写风格统一）
        Properties scriptProps = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(externalConfPath, StandardCharsets.UTF_8)) {
            scriptProps.load(reader);
        } catch (IOException e) {
            appendText("[ERROR] 读取外部配置失败：" + e.getMessage() + "\n");
            return;
        }

        // 【原有解析逻辑 完全原样保留】解析 script.数字.name / script.数字.cmd
        Map<Integer, ScriptItem> scriptMap = new TreeMap<>();
        for (Object key : scriptProps.keySet()) {
            String propKey = String.valueOf(key);
            if (propKey.matches("script\\.\\d+\\.name")) {
                int num = Integer.parseInt(propKey.replaceAll("\\D+", ""));
                String name = scriptProps.getProperty("script." + num + ".name");
                String cmd  = scriptProps.getProperty("script." + num + ".cmd");
                if (name != null && cmd != null && !name.isBlank()) {
                    scriptMap.put(num, new ScriptItem(name, cmd));
                }
            }
        }

        SCRIPTS.addAll(scriptMap.values());
    }

    /**
     * 执行命令（子线程 + 实时输出）
     */
    private void runCommand(String cmd) {
        if (cmd == null || externalScriptRootDir == null) {
            return;
        }
        outputArea.clear();
        appendText("---------------------------------------------------\n");
        appendText("-> 正在执行: " + cmd + "\n");

        // JavaFX 标准异步任务（不会卡死界面）
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // 【废弃 RESOURCE:: 分支 + 原生exec分支，仅保留脚本拆分逻辑】
                    int spaceIndex = cmd.indexOf(' ');
                    String pyFileName;
                    String args = "";

                    if (spaceIndex > 0) {
                        pyFileName = cmd.substring(0, spaceIndex);
                        args = cmd.substring(spaceIndex);
                    } else {
                        pyFileName = cmd;
                    }

                    // 执行外部Python脚本
                    runInternalScript(pyFileName, args);

                } catch (IOException | InterruptedException e) {
                    appendText("\n[执行异常] " + e.getMessage() + "\n");
                }
                return null;
            }
        };

        // 启动线程
        new Thread(task).start();
    }

    /**
     * 运行外部Python脚本文件（原方法名保留，改为读取本地外部文件）
     * 只加了一个 args 参数，原样传递
     */
    private void runInternalScript(String pyFileName, String args) throws IOException, InterruptedException {
        // 拼接外部Python脚本完整路径
        File pyFile = new File(externalScriptRootDir, pyFileName);
        if (!pyFile.exists() || !pyFile.isFile()) {
            appendText("[ERROR] 脚本不存在：" + pyFile.getAbsolutePath() + "\n");
            return;
        }

        // 读取外部py文件流（UTF-8 统一编码）
        InputStream input = new FileInputStream(pyFile);

        // ===================== 命令拼接：原样带参数 =====================
        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add("-");  // - 表示Python从标准输入读取脚本，不是读文件

        // ==============================================
        // 【重要】这里不能直接 add 一整串带空格的字符串！
        // 因为 Java 直接调用系统，不走终端，系统不会自动切分参数！
        //
        // 错误例子（你原来的写法）：
        // command.add("-g -d /path/to/source -m /path/to/md5")
        // → 系统认为这是【1个参数】，Python 不认识！
        //
        // 正确格式必须是：
        // command.add("-g")
        // command.add("-d")
        // command.add("/path/to/source")
        // command.add("/path/to/md5")
        // ...
        // → 拆成【独立字符串】，系统 & Python 才能识别
        // ==============================================
        if (args != null && !args.isBlank()) {
            // 按空格把参数字符串切成数组
            String[] argArray = args.trim().split("\\s+");
            // 逐个加入命令列表，模拟终端自动切分的效果
            for (String arg : argArray) {
                command.add(arg);
            }
        }

        // 最终命令示例：
        // [python3, -, -g, -d, /path/to/source, -m, /path/to/md5, -p, category, -s, .dmg, --git]

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 读取脚本内容写入进程标准输入（UTF-8）
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        }

        // 输出日志（UTF-8 读取执行结果）
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                appendText(line + "\n");
            }
        }

        process.waitFor();
        appendText("\n=== 执行完成 ===\n\n");
    }

    /**
     * 安全追加文本（必须在JavaFX主线程）
     */
    private void appendText(String text) {
        Platform.runLater(() -> {
            outputArea.appendText(text);
            outputArea.setScrollTop(Double.MAX_VALUE); // 自动滚动到底部
        });
    }

    /**
     * 脚本配置实体（JDK16+ record，企业标准写法）
     */
    private record ScriptItem(String name, String cmd) {
    }

}