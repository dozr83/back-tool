package backtool.ui.pane;

import backtool.service.PaneStateStore;
import backtool.service.core.TwoDirSameResult;
import backtool.service.core.DirCompareUtil;
import backtool.ui.component.FxKit;
import backtool.util.FileUtil;
import backtool.util.OsUtil;
import backtool.util.StringUtil;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 按【文件名（含扩展名）+ MD5】匹配两个文件夹，路径可以不同。
 * 用于精准排查"左边文件在右边是否有误删"。
 */
public class DirCompareByMd5namePane extends FxKit.FGridPane {
    private static final Logger logger = Logger.getLogger(DirCompareByMd5namePane.class.getName());
    private static final String K_STANDARD = "DirCrossDuplicatePane.standardDir";
    private static final String K_KILL = "DirCrossDuplicatePane.killDir";

    private final FxKit.LogTextArea outputPane;
    private FxKit.DirInputFx standardDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private FxKit.DirInputFx doubleKillDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private final CheckBox leftUniqCheckBox = new CheckBox("左边独有（右边缺失，疑似误删）");
    private final CheckBox rightUniqCheckBox = new CheckBox("右边独有（左边没有）");
    private final CheckBox sameCheckBox = new CheckBox("相同（文件名+MD5一致）");

    public DirCompareByMd5namePane() {
        outputPane = new FxKit.LogTextArea("执行结果");
        outputPane.append("执行结果");
        outputPane.append("对比文件名（含扩展名）+ MD5，路径可以不同");
        standardDirInput.setSelectDir(PaneStateStore.get(K_STANDARD, ""));
        doubleKillDirInput.setSelectDir(PaneStateStore.get(K_KILL, ""));
        leftUniqCheckBox.setSelected(true);
        rightUniqCheckBox.setSelected(true);
        sameCheckBox.setSelected(false);
        standardDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_STANDARD, b));
        doubleKillDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_KILL, b));
        this.addRow(new Label("左边（参考）："), standardDirInput);
        this.addRow(new Label("右边（待查）："), doubleKillDirInput);
        this.addMergeRow(FxKit.getHBox(
                new FxKit.FButton("按文件名+MD5对比", event -> dirCompare()),
                new FxKit.FButton("删除右边与左边相同的文件", event -> deleteRightSameFiles()),
                new Label("过滤规则：" + String.join("，", backtool.service.FilterRules.DEFAULT.describeRules()))
        ), 2);
        this.addMergeRow(FxKit.getHBox(
                new Label("输出内容："),
                leftUniqCheckBox,
                rightUniqCheckBox,
                sameCheckBox
        ), 2);
        this.addMergeRow(outputPane, 2);
    }

    private void dirCompare() {
        outputPane.clear();
        if (StringUtil.isBlank(standardDirInput.getSelectDir()) || StringUtil.isBlank(doubleKillDirInput.getSelectDir())) {
            outputPane.append("请选择两个文件夹");
            return;
        }
        String leftDir = standardDirInput.getSelectDir();
        String rightDir = doubleKillDirInput.getSelectDir();
        if (leftDir.equals(rightDir)) {
            outputPane.append("左右为同一文件夹，无法对比");
            return;
        }
        TwoDirSameResult result = DirCompareUtil.compareTwoByNameAndContent(leftDir, rightDir);
        //汇总行
        outputPane.append("左边独有 " + result.leftUniq.size() + " 个，右边独有 " + result.rightUniq.size()
                + " 个，相同 " + result.sames.size() + " 组");
        outputPane.append("左边文件总数：" + countFiles(leftDir) + "，右边文件总数：" + countFiles(rightDir));
        //左边独有
        if (leftUniqCheckBox.isSelected()) {
            outputPane.append("");
            outputPane.append("━━━ 左边独有（右边缺失，疑似误删）：" + result.leftUniq.size() + " 个 ━━━");
            result.leftUniq.forEach(file -> outputPane.append("  " + file.getAbsolutePath()));
        }
        //右边独有
        if (rightUniqCheckBox.isSelected()) {
            outputPane.append("");
            outputPane.append("━━━ 右边独有（左边没有）：" + result.rightUniq.size() + " 个 ━━━");
            result.rightUniq.forEach(file -> outputPane.append("  " + file.getAbsolutePath()));
        }
        //相同
        if (sameCheckBox.isSelected()) {
            outputPane.append("");
            outputPane.append("━━━ 相同（文件名+MD5一致）：" + result.sames.size() + " 组 ━━━");
            int groupNo = 1;
            for (TwoDirSameResult.CrossIdentity same : result.sames) {
                outputPane.append("【组 " + groupNo + "】");
                same.lefts().forEach(f -> outputPane.append("  左: " + f.getAbsolutePath()));
                same.rights().forEach(f -> outputPane.append("  右: " + f.getAbsolutePath()));
                groupNo++;
            }
        }
        outputPane.append("");
        outputPane.append("对比完成");
        if (result.leftUniq.isEmpty()) {
            outputPane.append("✓ 左边所有文件在右边都有同名同 MD5 的对应项，无疑似误删");
        } else {
            outputPane.append("⚠ 右边缺失 " + result.leftUniq.size() + " 个左边文件，可能存在误删");
        }
    }

    private int countFiles(String dirStr) {
        File root = new File(dirStr);
        if (!root.isDirectory()) return 0;
        final int[] count = {0};
        FileUtil.walkAllDirIgnoreHiddenDir(root, file -> count[0]++);
        return count[0];
    }

    private void deleteRightSameFiles() {
        if (StringUtil.isBlank(standardDirInput.getSelectDir()) || StringUtil.isBlank(doubleKillDirInput.getSelectDir())) {
            outputPane.clear();
            outputPane.append("请选择两个文件夹");
            return;
        }
        String leftDir = standardDirInput.getSelectDir();
        String rightDir = doubleKillDirInput.getSelectDir();
        if (leftDir.equals(rightDir)) {
            outputPane.clear();
            outputPane.append("左右为同一文件夹，无法删除");
            return;
        }
        TwoDirSameResult result = DirCompareUtil.compareTwoByNameAndContent(leftDir, rightDir);
        //收集右边那些"有左边同名同 MD5 对应"的文件
        List<File> toDelete = new ArrayList<>();
        for (TwoDirSameResult.CrossIdentity same : result.sames) {
            toDelete.addAll(same.rights());
        }
        if (toDelete.isEmpty()) {
            outputPane.clear();
            outputPane.append("没有需要删除的文件（右边没有与左边文件名+MD5相同的项）");
            return;
        }
        var confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("将删除右边文件夹中 " + toDelete.size() + " 个与左边文件名+MD5相同的文件");
        confirm.setContentText("⚠ 强烈建议右边文件夹先整体备份，用于查看删除后剩余的文件。确认删除？");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        outputPane.clear();
        outputPane.append("删除右边与左边文件名+MD5相同的文件：" + toDelete.size() + " 个");
        int success = 0;
        List<String> failures = new ArrayList<>();
        for (File file : toDelete) {
            boolean deleted = file.delete();
            if (!deleted) {
                //mac 锁定(uchg)的文件会被直接拒删，仅 Mac 下先清不可变标志再重试一次
                if (OsUtil.isMac()) {
                    try {
                        new ProcessBuilder("chflags", "nouchg", file.getAbsolutePath()).start().waitFor();
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "清除锁定标志失败: " + file.getAbsolutePath(), e);
                    }
                    deleted = file.delete();
                }
            }
            if (deleted) {
                success++;
                outputPane.append("  ✓ " + file.getAbsolutePath());
            } else {
                failures.add("  ✗ " + file.getAbsolutePath());
            }
        }
        outputPane.append("");
        outputPane.append("删除完成：成功 " + success + " 个，失败 " + failures.size() + " 个");
        if (!failures.isEmpty()) {
            outputPane.append("失败明细：");
            failures.forEach(outputPane::append);
        }
    }
}
