package backtool.ui.pane;

import backtool.service.FilterRules;
import backtool.service.core.TwoDirPathResult;
import backtool.service.core.DirCompareUtil;
import backtool.ui.component.FxKit;
import backtool.util.StringConst;
import backtool.util.StringUtil;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

/**
 * Created by dozr83 on 2020/3/7.
 */
public class DirComparePane extends FxKit.FGridPane {
    private final FxKit.LogTextArea outputPane;
    //选择对比文件夹
    private FxKit.DirInputFx leftDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private FxKit.DirInputFx rightDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private final CheckBox showSameCheckBox = new CheckBox("同时列出相同文件");

    public DirComparePane() {
        outputPane = new FxKit.LogTextArea("执行结果");
        outputPane.append("执行结果");
        outputPane.append("左边独有 0 个，右边独有 0 个，修改 0 个，相同 0 个");
        this.addRow(new Label("左边："), leftDirInput);
        this.addRow(new Label("右边："), rightDirInput);
        this.addMergeRow(FxKit.getHBox(
                new FxKit.FButton("对比文件夹", event -> dirCompare()),
                showSameCheckBox,
                new Label("过滤规则：" + String.join("，", FilterRules.DEFAULT.describeRules()))
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

        resultBuilder.append("左边独有文件：").append(StringConst.LF);
        compareResult.leftUniqFiles.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringConst.LF));

        resultBuilder.append("右边独有文件：").append(StringConst.LF);
        compareResult.rightUniqFiles.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringConst.LF));

        resultBuilder.append("修改文件：").append(StringConst.LF);
        compareResult.modifiedFiles.forEach(delta -> resultBuilder.append(delta.left().getAbsolutePath()).append(" → ").append(delta.right().getAbsolutePath()).append(StringConst.LF));

        if (showSameCheckBox.isSelected()) {
            resultBuilder.append("相同文件：").append(StringConst.LF);
            compareResult.sameFiles.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringConst.LF));
        }

        outputPane.append(resultBuilder.toString());
    }

    private boolean checkParamBlank() {
        return StringUtil.isBlank(leftDirInput.getSelectDir()) || StringUtil.isBlank(rightDirInput.getSelectDir());
    }
}
