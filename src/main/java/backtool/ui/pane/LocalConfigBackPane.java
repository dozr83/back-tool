package backtool.ui.pane;

import backtool.ui.component.FxKit;
import backtool.service.ConfigStore;
import javafx.scene.control.Label;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LocalConfigBackPane extends FxKit.FGridPane {
    private static final Logger logger = Logger.getLogger(LocalConfigBackPane.class.getName());
    private final FxKit.LogTextArea outputArea = new FxKit.LogTextArea("执行结果");

    public LocalConfigBackPane() {
        try {
            showCommonScriptConfigTip();
            this.addRow(new Label("配置文件路径："), new Label(ConfigStore.CONFIG_FILE_PATH.toString()));
            this.addMergeRow(FxKit.getHBox(
                    new FxKit.FButton("读取配置", event -> {
                        outputArea.clear();
                        StringBuilder sb = new StringBuilder("==== 当前加载的配置 ====\n");
                        ConfigStore.PROPERTIES.forEach((k, v) -> {
                            sb.append(k).append("=").append(v).append("\n");
                        });
                        outputArea.append(sb.toString());
                    }),
                    new FxKit.FButton("打开配置目录", event -> {
                        outputArea.clear();
                        try {
                            Path dir = ConfigStore.createDirAndEmptyConfigFile();
                            FxKit.openFile(dir.toString());
                            outputArea.append("已打开配置目录：" + dir);
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "打开配置目录失败", e);
                            outputArea.append("打开失败：" + e.getMessage());
                        }
                    })
            ), 2);
            this.addMergeRow(outputArea, 2);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "初始化配置面板失败", e);
            outputArea.clear();
            outputArea.append("初始化失败：" + e.getMessage());
        }
    }

    private void showCommonScriptConfigTip() {
        String tip = """
                ================================
                【通用脚本按钮配置说明】
                ================================
                本工具采用双层配置，Python脚本全部外置，不打包进程序
                
                一、程序主配置（当前 config.properties）
                仅配置外部脚本目录与配置文件名，固定两个参数：
                python.script.dir=/path/to/your/scripts
                python.script.config=script.conf
                
                二、外部脚本配置（script.conf）
                存放所有按钮与执行命令，格式规范：
                script.序号.name=按钮显示名称
                script.序号.cmd=脚本文件名 运行参数

                🔸 按钮命名规范（必须遵守）
                   1. 长度控制在 8 个汉字以内
                   2. 简洁清晰，见名知意
                   3. 超长会自动截断，不影响布局

                🔸 命令配置规范
                   1. 仅填写 脚本名 + 运行参数，无需添加 python3
                   2. 仅支持外部 Python 脚本执行
                   3. 所有 .py 文件统一放在上述外部目录中

                🔸 配置示例
                   script.1.name=正式文件备份
                   script.1.cmd=md5_tool.py -g -d /path/to/source -m /path/to/md5 -p category -s .dmg --git

                ================================
                提示：按钮与命令全部在外部 script.conf 中维护
                ================================
                """;
        outputArea.append(tip);
    }
}