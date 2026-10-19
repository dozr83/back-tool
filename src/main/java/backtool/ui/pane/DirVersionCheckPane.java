package backtool.ui.pane;

import backtool.service.PaneStateStore;
import backtool.ui.component.FxKit;
import backtool.service.DigestCalculator;
import backtool.service.DigestManifest;
import backtool.service.DirVersionProcessor;
import backtool.util.DateUtil;
import backtool.util.StringConst;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import backtool.util.StringUtil;
import javafx.application.Platform;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DirVersionCheckPane extends FxKit.FGridPane {
    private static final Logger logger = Logger.getLogger(DirVersionCheckPane.class.getName());
    private static final String K_CHECK_DIR = "DirVersionCheckPane.checkDir";
    private FxKit.DirInputFx checkDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private TextArea textArea = new TextArea();
    private final FxKit.LogTextArea outputArea = new FxKit.LogTextArea("执行结果");

    public DirVersionCheckPane() {
        outputArea.append("执行结果");
        textArea.setMaxHeight(100);
        checkDirInput.setSelectDir(PaneStateStore.get(K_CHECK_DIR, ""));
        checkDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_CHECK_DIR, b));
        this.addRow(new Label("选择要检查的文件夹:"), checkDirInput);
        this.addRow(new Label("手工输入资产清单:"), textArea);
        this.addMergeRow(new FxKit.FButton("检查资产清单", event -> check()), 2);
        this.addMergeRow(outputArea, 2);
    }

    private void check() {
        outputArea.clear();
        if (StringUtil.isBlank(textArea.getText())) {
            outputArea.append("请选输入清单");
            return;
        }
        if (StringUtil.isBlank(checkDirInput.getSelectDir())) {
            outputArea.append("请选择检查文件");
            return;
        }
        final String inputStr = textArea.getText();
        final String checkDir = checkDirInput.getSelectDir();
        outputArea.append("已经开始检查摘要文件：" + DateUtil.getCurrDateStr());
        new Thread(() -> {
            try {
                // 使用公共方法解析字符串清单
                LinkedHashMap<String, String> historyMap = DigestManifest.parseVersionStr(inputStr);
                DirVersionProcessor.VersionCheckResult compareResult = DirVersionProcessor.checkDirChangeByMap(checkDir, historyMap);

                var resultBuilder = new StringBuilder();
                resultBuilder.append(StringConst.LF);
                if (compareResult.deleteFiles().size() == 0) {
                    resultBuilder.append("没有丢失或修改的资产文件,资产总数:").append(compareResult.okFiles().size()).append(StringConst.LF);
                } else {
                    resultBuilder.append("丢失或修改的资产文件：").append(compareResult.deleteFiles().size()).append(StringConst.LF);
                    compareResult.deleteFiles().forEach(fileStr -> resultBuilder.append(fileStr).append(StringConst.LF));
                    resultBuilder.append(StringConst.LF);
                }
                if (compareResult.okFiles().size() > 0) {
                    resultBuilder.append("保留的正确资产文件：").append(compareResult.okFiles().size()).append(StringConst.LF);
                    compareResult.okFiles().forEach(fileStr -> resultBuilder.append(fileStr).append(StringConst.LF));
                }
                resultBuilder.append(StringConst.LF);
                // 计算结果回 FX 主线程渲染
                Platform.runLater(() -> outputArea.append(resultBuilder.toString()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "检查资产清单失败: checkDir=" + checkDir, e);
                Platform.runLater(() -> outputArea.append("检查失败: " + e.getMessage()));
            }
        }).start();
    }
}
