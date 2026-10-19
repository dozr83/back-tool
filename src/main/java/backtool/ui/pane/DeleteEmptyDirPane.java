package backtool.ui.pane;

import backtool.service.PaneStateStore;
import backtool.ui.component.FxKit;
import backtool.util.FileUtil;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import backtool.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DeleteEmptyDirPane extends VBox {
    private static final Logger logger = Logger.getLogger(DeleteEmptyDirPane.class.getName());
    //和 DeleteBuildPane 共用，用户经常是同一个目录
    private static final String K_ROOT = "DeletePane.rootDir";
    private FxKit.DirInputFx rootDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private final Label statusLabel = new Label();
    private final VBox detailPane = new VBox();

    public DeleteEmptyDirPane() {
        rootDirInput.setSelectDir(PaneStateStore.get(K_ROOT, ""));
        rootDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_ROOT, b));

        FxKit.FGridPane grid = new FxKit.FGridPane();
        grid.addRow(new Label("选择文件夹"), rootDirInput);
        grid.addMergeRow(FxKit.getHBox(
                new FxKit.FButton("删除空文件夹", event -> deleteEmptyDirs()),
                new Label("忽略系统元数据文件（.DS_Store / ._ 等），删除后自动向上清理随之变空的父目录")
        ), 2);
        grid.addMergeRow(statusLabel, 2);
        grid.addMergeRow(detailPane, 2);
        grid.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        this.getChildren().add(grid);
    }

    private void deleteEmptyDirs() {
        File root = checkRoot();
        if (root == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("将删除所选文件夹下的空目录，并递归清理随之变空的父目录");
        confirm.setContentText("空目录只含系统元数据。确认删除？");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        statusLabel.setText("正在删除空文件夹...");
        Thread thread = new Thread(() -> {
            List<File> emptyDirs = new ArrayList<>();
            FileUtil.walkAllDirIgnoreHiddenDir(root, file -> {
                if (FileUtil.isEmptyDirIgnoreMetadata(file)) {
                    emptyDirs.add(file);
                }
            });
            //先删最深的空目录，父目录随后被向上清理；浅层条目删前再判存在
            emptyDirs.sort(Comparator.comparingInt((File file) -> file.toPath().getNameCount()).reversed());
            int success = 0;
            List<String> failures = new ArrayList<>();
            for (File dir : emptyDirs) {
                if (!dir.exists()) {
                    continue;
                }
                if (FileUtil.deleteEmptyDirIgnoreMetadata(dir)) {
                    success++;
                    File parent = dir.getParentFile();
                    while (parent != null && !parent.equals(root) && FileUtil.isEmptyDirIgnoreMetadata(parent)
                            && FileUtil.deleteEmptyDirIgnoreMetadata(parent)) {
                        success++;
                        parent = parent.getParentFile();
                    }
                } else {
                    failures.add("非空，未删除: " + dir.getAbsolutePath());
                }
            }
            int successFinal = success;
            List<String> failuresFinal = failures;
            javafx.application.Platform.runLater(() -> {
                StringBuilder message = new StringBuilder("删除空文件夹完成：成功 " + successFinal + " 个");
                if (!failuresFinal.isEmpty()) {
                    message.append("，失败 ").append(failuresFinal.size()).append(" 个");
                }
                statusLabel.setText(message.toString());
                detailPane.getChildren().clear();
                for (String f : failuresFinal) {
                    detailPane.getChildren().add(new Label("  ❌ " + f));
                }
            });
        }, "delete-empty-dirs");
        thread.setDaemon(true);
        thread.start();
    }

    private File checkRoot() {
        if (StringUtil.isBlank(rootDirInput.getSelectDir())) {
            statusLabel.setText("未选择文件夹");
            return null;
        }
        File root = new File(rootDirInput.getSelectDir());
        if (!root.isDirectory()) {
            statusLabel.setText("选择的文件夹不存在");
            return null;
        }
        return root;
    }

}
