package backtool.ui.pane;

import backtool.service.FilterRules;
import backtool.service.PaneStateStore;
import backtool.service.core.TwoDirPathResult;
import backtool.service.core.DirCompareUtil;
import backtool.ui.component.FxKit;
import backtool.util.FileUtil;
import backtool.util.OsUtil;
import backtool.util.StringConst;
import backtool.util.StringUtil;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dozr83 on 2020/3/7.
 */
public class DirCompareByPathPane extends FxKit.FGridPane {
    private static final Logger logger = Logger.getLogger(DirCompareByPathPane.class.getName());
    private static final String K_LEFT = "DirComparePane.leftDir";
    private static final String K_RIGHT = "DirComparePane.rightDir";
    private final FxKit.LogTextArea outputPane;
    //选择对比文件夹
    private FxKit.DirInputFx leftDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private FxKit.DirInputFx rightDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private final CheckBox leftUniqCheckBox = new CheckBox("左边独有");
    private final CheckBox rightUniqCheckBox = new CheckBox("右边独有");
    private final CheckBox modifiedCheckBox = new CheckBox("修改");
    private final CheckBox sameCheckBox = new CheckBox("相同");

    public DirCompareByPathPane() {
        outputPane = new FxKit.LogTextArea("执行结果");
        outputPane.append("执行结果");
        outputPane.append("左边独有 0 个，右边独有 0 个，修改 0 个，相同 0 个");
        //先读持久化值，再挂 listener，避免 set 时触发写回
        leftDirInput.setSelectDir(PaneStateStore.get(K_LEFT, ""));
        rightDirInput.setSelectDir(PaneStateStore.get(K_RIGHT, ""));
        leftUniqCheckBox.setSelected(true);
        rightUniqCheckBox.setSelected(true);
        modifiedCheckBox.setSelected(true);
        sameCheckBox.setSelected(false);
        leftDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_LEFT, b));
        rightDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_RIGHT, b));
        this.addRow(new Label("左边："), leftDirInput);
        this.addRow(new Label("右边："), rightDirInput);
        this.addMergeRow(FxKit.getHBox(
                new FxKit.FButton("对比文件夹", event -> dirCompare()),
                new FxKit.FButton("删除右侧文件夹相同文件", event -> deleteRightSameFiles()),
                new Label("过滤规则：" + String.join("，", FilterRules.DEFAULT.describeRules()))
        ), 2);
        this.addMergeRow(FxKit.getHBox(
                new Label("输出内容："),
                leftUniqCheckBox,
                rightUniqCheckBox,
                modifiedCheckBox,
                sameCheckBox
        ), 2);
        this.addMergeRow(outputPane, 2);
    }

    private void dirCompare() {
        outputPane.clear();
        if (checkParamBlank()) {
            outputPane.append("请选择");
            return;
        }
        outputPane.append("执行结果");

        StringBuilder resultBuilder = new StringBuilder();
        TwoDirPathResult compareResult = DirCompareUtil.compareTwoByPath(leftDirInput.getSelectDir(), rightDirInput.getSelectDir());

        //汇总在前：右边独有为空 = 左边全包含右边
        resultBuilder.append("左边独有 ").append(compareResult.leftUniqFiles.size())
                .append(" 个，右边独有 ").append(compareResult.rightUniqFiles.size())
                .append(" 个，修改 ").append(compareResult.modifiedFiles.size())
                .append(" 个，相同 ").append(compareResult.sameFiles.size()).append(" 个");
        if (compareResult.rightUniqFiles.isEmpty()) {
            resultBuilder.append("，左边已全包含右边");
        }
        resultBuilder.append(StringConst.LF).append(StringConst.LF);

        if (leftUniqCheckBox.isSelected()) {
            resultBuilder.append("左边独有文件：").append(StringConst.LF);
            compareResult.leftUniqFiles.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringConst.LF));
        }

        if (rightUniqCheckBox.isSelected()) {
            resultBuilder.append("右边独有文件：").append(StringConst.LF);
            compareResult.rightUniqFiles.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringConst.LF));
        }

        if (modifiedCheckBox.isSelected()) {
            resultBuilder.append("修改文件：").append(StringConst.LF);
            compareResult.modifiedFiles.forEach(delta -> resultBuilder.append(delta.left().getAbsolutePath()).append(" → ").append(delta.right().getAbsolutePath()).append(StringConst.LF));
        }

        if (sameCheckBox.isSelected()) {
            resultBuilder.append("相同文件：").append(StringConst.LF);
            compareResult.sameFiles.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringConst.LF));
        }

        outputPane.append(resultBuilder.toString());
    }

    private void deleteRightSameFiles() {
        outputPane.clear();
        if (checkParamBlank()) {
            outputPane.append("请选择");
            return;
        }
        String leftDir = leftDirInput.getSelectDir();
        String rightDir = rightDirInput.getSelectDir();
        if (leftDir.equals(rightDir)) {
            outputPane.append("左右为同一文件夹，无法删除");
            return;
        }
        TwoDirPathResult compareResult = DirCompareUtil.compareTwoByPath(leftDir, rightDir);

        //相同文件记录的是左侧文件，按相对路径换算为右侧文件
        List<File> sameRightFiles = compareResult.sameFiles.stream()
                .map(leftFile -> new File(rightDir, FileUtil.getSubPathUnix(leftFile.getAbsolutePath(), leftDir)))
                .toList();
        outputPane.append("执行结果");
        if (sameRightFiles.isEmpty()) {
            outputPane.append("右侧没有与左侧相同的文件");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("将删除右侧文件夹中与左侧相同的 " + sameRightFiles.size() + " 个文件");
        confirm.setContentText("强烈建议右侧文件夹先整体备份，用于查看删除后剩余的文件。确认删除？");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        int success = 0;
        int fail = 0;
        StringBuilder failBuilder = new StringBuilder();
        for (File file : sameRightFiles) {
            boolean deleted = file.delete();
            //mac 锁定(uchg)的文件会被直接拒删，仅 Mac 下先清不可变标志再重试一次
            if (!deleted && OsUtil.isMac()) {
                try {
                    new ProcessBuilder("chflags", "nouchg", file.getAbsolutePath()).start().waitFor();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "清除锁定标志失败: " + file.getAbsolutePath(), e);
                }
                deleted = file.delete();
            }
            if (deleted) {
                success++;
            } else {
                fail++;
                failBuilder.append(file.getAbsolutePath()).append(StringConst.LF);
            }
        }
        outputPane.append("删除右侧相同文件：成功 " + success + " 个，失败 " + fail + " 个");
        if (fail > 0) {
            outputPane.append("失败文件：");
            outputPane.append(failBuilder.toString());
        }
    }

    private boolean checkParamBlank() {
        return StringUtil.isBlank(leftDirInput.getSelectDir()) || StringUtil.isBlank(rightDirInput.getSelectDir());
    }
}
