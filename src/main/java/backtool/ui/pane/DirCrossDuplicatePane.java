package backtool.ui.pane;

import backtool.service.PaneStateStore;
import backtool.service.core.TwoDirSameResult;
import backtool.service.core.DirCompareUtil;
import backtool.ui.component.FxKit;
import backtool.AppConfig;
import backtool.util.FileUtil;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import backtool.util.StringUtil;

import java.io.File;


public class DirCrossDuplicatePane extends VBox {
    private static final String K_STANDARD = "DirCrossDuplicatePane.standardDir";
    private static final String K_KILL = "DirCrossDuplicatePane.killDir";
    private final FxKit.LogTextArea outputPane;
    private FxKit.DirInputFx standardDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private FxKit.DirInputFx doubleKillDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private FxKit.FGridPane imgGridPane = new FxKit.FGridPane();
    private VBox vBoxOutputPane = FxKit.getVBox();

    public DirCrossDuplicatePane() {
        outputPane = new FxKit.LogTextArea("执行结果");
        outputPane.append("执行结果");
        standardDirInput.setSelectDir(PaneStateStore.get(K_STANDARD, ""));
        doubleKillDirInput.setSelectDir(PaneStateStore.get(K_KILL, ""));
        standardDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_STANDARD, b));
        doubleKillDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_KILL, b));
        imgGridPane.addRow(new Label("选择标准参考文件夹："), standardDirInput);
        imgGridPane.addRow(new Label("选择删除重复文件文件夹："), doubleKillDirInput);
        imgGridPane.addRow(new FxKit.FButton("查看重复文件", event -> dirCompare()), new FxKit.FButton("删除重复文件", event -> doubleKill()));
        imgGridPane.addMergeRow(outputPane, 2);
        vBoxOutputPane.setPadding(new javafx.geometry.Insets(10, 5, 5, 10));
        this.getChildren().add(imgGridPane);
        this.getChildren().add(vBoxOutputPane);
    }

    private void delete(File file, File repeatFile, ActionEvent event) {
        clear();
        ((FxKit.FButton) event.getSource()).setDisable(true);
        FileUtil.deleteRepeatFile(file, repeatFile);
        dirCompare();
    }

    private void clear() {
        outputPane.clear();
        vBoxOutputPane.getChildren().clear();
    }

    private void dirCompare() {
        clear();
        if (StringUtil.isBlank(standardDirInput.getSelectDir()) || StringUtil.isBlank(doubleKillDirInput.getSelectDir())) {
            outputPane.append("请选择");
            return;
        }
        StringBuilder resultBuilder = compareTwoSame(standardDirInput.getSelectDir(), doubleKillDirInput.getSelectDir());
        outputPane.append(resultBuilder.toString());
    }

    private StringBuilder compareTwoSame(String leftDirStr, String rightDirStr) {
        vBoxOutputPane.getChildren().clear();
        StringBuilder resultBuilder = new StringBuilder();
        TwoDirSameResult result = DirCompareUtil.compareTwoByContent(leftDirStr, rightDirStr);
        resultBuilder.append("将要删除的重复文件:").append(result.sames.size()).append(StringUtil.LF);
        result.sames.forEach(sameObj -> {
            sameObj.lefts().forEach(repeatFile -> {
                resultBuilder.append("existFile:").append(repeatFile.getAbsolutePath()).append(StringUtil.LF);
            });
            sameObj.rights().forEach(deleteFile -> {
                resultBuilder.append("deleteFile:").append(deleteFile.getAbsolutePath()).append(StringUtil.LF);
            });
        });
        result.sames.forEach(sameObj -> {
            HBox hBoxImage = FxKit.getHBox();
            sameObj.lefts().forEach(repeatFile -> {
                HBox hBox = FxKit.getHBox();
                hBox.getChildren().add(new FxKit.FButton("打开", event -> FxKit.openFile(repeatFile.getParentFile().getAbsolutePath())));
                hBox.getChildren().add(new Label("保留：" + repeatFile.getAbsolutePath()));
                vBoxOutputPane.getChildren().add(hBox);
                if (FileUtil.isImg(repeatFile)) {
                    hBoxImage.getChildren().add(FxKit.imageView(repeatFile, 200, 200));
                }
            });
            sameObj.rights().forEach(deleteFile -> {
                HBox hBox = FxKit.getHBox();
                hBox.getChildren().add(new FxKit.FButton("打开", event -> FxKit.openFile(deleteFile.getParentFile().getAbsolutePath())));
                hBox.getChildren().add(new Label("删除：" + deleteFile.getAbsolutePath()));
                hBox.getChildren().add(new FxKit.FButton("删除", event -> delete(deleteFile, sameObj.lefts().get(0), event)));
                vBoxOutputPane.getChildren().add(hBox);
                if (FileUtil.isImg(deleteFile)) {
                    hBoxImage.getChildren().add(FxKit.imageView(deleteFile, 200, 200));
                }
            });
            if (hBoxImage.getChildren().size() > 0) {
                vBoxOutputPane.getChildren().add(hBoxImage);
                vBoxOutputPane.getChildren().add(new Label(AppConfig.SPLITE_LINE_100));
            }
        });
        return resultBuilder;
    }

    private void doubleKill() {
        clear();
        if (StringUtil.isBlank(standardDirInput.getSelectDir()) || StringUtil.isBlank(doubleKillDirInput.getSelectDir())) {
            outputPane.append("请选择");
            return;
        }
        StringBuilder resultBuilder = doubleKillTowSame(standardDirInput.getSelectDir(), doubleKillDirInput.getSelectDir());
        outputPane.append(resultBuilder.toString());
    }

    private StringBuilder doubleKillTowSame(String leftDirStr, String rightDirStr) {
        StringBuilder resultBuilder = new StringBuilder();
        TwoDirSameResult result = DirCompareUtil.compareTwoByContent(leftDirStr, rightDirStr);
        resultBuilder.append("开始删除文件:").append(result.sames.size()).append(StringUtil.LF);
        result.sames.forEach(sameObj -> {
            sameObj.rights().forEach(deleteFile -> {
                resultBuilder.append("deleteFile:").append(deleteFile.getAbsolutePath()).append(StringUtil.LF);
                resultBuilder.append("existFile:").append(sameObj.lefts().get(0).getAbsolutePath()).append(StringUtil.LF);
                FileUtil.deleteRepeatFile(deleteFile, sameObj.lefts().get(0));
            });
        });
        return resultBuilder;
    }
}