package backtool.ui.pane;

import backtool.service.core.OneDirResult;
import backtool.service.core.DirCompareUtil;
import backtool.AppConfig;
import backtool.ui.component.FxKit;
import backtool.util.FileUtil;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import backtool.util.StringUtil;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;


public class DirDuplicatePane extends VBox {
    private final FxKit.LogTextArea outputPane;
    private FxKit.DirInputFx compareDirSelectPan = new FxKit.DirInputFx(FxKit.inputLength);
    private FxKit.FGridPane imgGridPane = new FxKit.FGridPane();
    private VBox vBoxOutputPane = FxKit.getVBox();
    private TextField countFiled = FxKit.getNumberInputTextField(100);

    public DirDuplicatePane() {
        outputPane = new FxKit.LogTextArea("执行结果");
        outputPane.append("执行结果");
        imgGridPane.addRow(new Label("选择文件夹"), compareDirSelectPan);
        imgGridPane.addRow(new Label("显示操作文件数量"), countFiled);
        var buttons = FxKit.getHBox(
                new FxKit.FButton("查找重复文件", event -> find())
        );
        imgGridPane.addMergeRow(buttons, 2);
        imgGridPane.addMergeRow(outputPane, 2);
        imgGridPane.setAlignment(Pos.TOP_LEFT);
        this.getChildren().add(imgGridPane);
        this.getChildren().add(vBoxOutputPane);
        vBoxOutputPane.setPadding(new javafx.geometry.Insets(10, 5, 5, 10));
    }

    private void find() {
        outputPane.clear();
        if (StringUtil.isBlank(compareDirSelectPan.getSelectDir())) {
            outputPane.append("未选择");
            return;
        }
        if (!StringUtil.isNumeric(countFiled.getText())) {
            outputPane.append("文件数量必须为数字");
            return;
        }
        int max = Integer.parseInt(countFiled.getText());
        clear();
        int baseIndex = compareDirSelectPan.getSelectDir().length() - 1;
        OneDirResult singleDuplicates = DirCompareUtil.compareOneDir(compareDirSelectPan.getSelectDir());
        AtomicInteger count = new AtomicInteger(0);
        singleDuplicates.duplicates.forEach((sameObj) -> {
            if (count.incrementAndGet() > max) {
                return;
            }
            var files = sameObj.sames();
            HBox hBoxImage = FxKit.getHBox();
            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);
                File repeatFile = i == 0 ? files.get(1) : files.get(0);
                vBoxOutputPane.getChildren().add(FxKit.getHBox(
                        new FxKit.FButton("删除", event -> delete(file, repeatFile, event)),
                        new FxKit.FButton("打开", event -> FxKit.openFile(file)),
                        new Label(file.getAbsolutePath().substring(baseIndex + 2))
                ));
                if (FileUtil.isImg(file)) {
                    hBoxImage.getChildren().add(FxKit.imageView(file, FxKit.image_width, FxKit.image_height));
                }
            }
            vBoxOutputPane.getChildren().add(hBoxImage);
            vBoxOutputPane.getChildren().add(new Label(AppConfig.SPLITE_LINE_100));
        });
        StringBuilder resultBuilder = new StringBuilder();
        if (singleDuplicates.duplicates.size() == 0) {
            resultBuilder.append("没有重复文件").append(StringUtil.LF);
        } else {
            resultBuilder.append("重复文件数量：").append(singleDuplicates.duplicates.size()).append(StringUtil.LF);
        }
        singleDuplicates.duplicates.forEach(sameObj -> {
            var files = sameObj.sames();
            resultBuilder.append("重复次数：").append(files.size()).append(StringUtil.LF);
            files.forEach(file -> resultBuilder.append(file.getAbsolutePath()).append(StringUtil.LF));
            resultBuilder.append(StringUtil.LF);
        });
        outputPane.append(resultBuilder.toString());
    }

    private void clear() {
        outputPane.clear();
        vBoxOutputPane.getChildren().clear();
    }

    private void delete(File file, File repeatFile, ActionEvent event) {
        clear();
        ((FxKit.FButton) event.getSource()).setDisable(true);
        FileUtil.deleteRepeatFile(file, repeatFile);
        find();
    }
}