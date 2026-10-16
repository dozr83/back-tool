package backtool.ui.pane;

import backtool.ui.component.FxKit;
import backtool.util.FileUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FileUtils;
import backtool.util.StringUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;


public class DeleteEmptyDirPane extends VBox {
    private static final Logger logger = Logger.getLogger(DeleteEmptyDirPane.class.getName());
    //选择对比文件夹
    private FxKit.DirInputFx compareDirSelectPan = new FxKit.DirInputFx(FxKit.inputLength);

    private FxKit.FGridPane imgGridPane = new FxKit.FGridPane();
    private VBox vBoxOutputPane = FxKit.getVBox();
    private TextField countFiled = FxKit.getNumberInputTextField(100);
    private Set<String> cleanDirs = Set.of( ".git", "target", ".idea",  "node_modules", ".mvn", "build", ".venv");
    // 每个Pane自己的输出区域
    private final FxKit.LogTextArea outputArea = new FxKit.LogTextArea("执行结果");

    public DeleteEmptyDirPane() {
        outputArea.append("执行结果");
        imgGridPane.addRow(new Label("选择文件夹"), compareDirSelectPan);
        imgGridPane.addRow(new Label("显示操作文件数量"), countFiled);
        var buttons = FxKit.getHBox(
                new FxKit.FButton("查看空文件夹", event -> {
                    processEmptyDirs(false);
                }),
                new FxKit.FButton("删除空文件夹", event -> {
                    processEmptyDirs(true);
                }),
                new FxKit.FButton("查看清理文件", event -> {
                    cleanBuildDirs(false);
                }),
                new FxKit.FButton("清理文件", event -> {
                    cleanBuildDirs(true);
                }),
                new Label("空文件夹规则：忽略" + FileUtil.systemMetadataFiles + "，跳过.开头目录"));
        imgGridPane.addMergeRow(buttons, 2);

        imgGridPane.addMergeRow(outputArea, 2);
        imgGridPane.setAlignment(Pos.TOP_LEFT);
        this.getChildren().add(imgGridPane);
        this.getChildren().add(vBoxOutputPane);
        vBoxOutputPane.setPadding(new javafx.geometry.Insets(10, 5, 5, 10));

    }

    private void processEmptyDirs(boolean delete) throws IOException {
        clear();
        if (StringUtil.isBlank(compareDirSelectPan.getSelectDir())) {
            outputArea.append("未选择");
            return;
        }
        outputArea.append(delete ? "执行删除..." : "扫描空文件夹...");
        FileUtil.walkAllDirIgnoreHiddenDir(Path.of(compareDirSelectPan.getSelectDir()).toFile(), file -> {
            if (FileUtil.isEmptyDirIgnoreMetadata(file)) {
                if (delete) {
                    FileUtil.deleteEmptyDirIgnoreMetadata(file);
                    outputArea.append(file.getAbsolutePath() + " deleted");
                } else {
                    outputArea.append(file.getAbsolutePath());
                    vBoxOutputPane.getChildren().add(FxKit.getHBox(
                            new FxKit.FButton("删除", e -> {
                                FileUtil.deleteEmptyDirIgnoreMetadata(file);
                                processEmptyDirs(false);
                            }),
                            new FxKit.FButton("打开", e -> FxKit.openFile(file)),
                            new Label(file.getAbsolutePath())
                    ));
                }
            }
        });
        outputArea.append(delete ? "删除完成" : "扫描完成");
    }


    private void clear() {
        outputArea.clear();
        vBoxOutputPane.getChildren().clear();
    }

    private void cleanBuildDirs(boolean delete) {
        clear();
        if (StringUtil.isBlank(compareDirSelectPan.getSelectDir())) {
            outputArea.append("未选择");
            return;
        }
        outputArea.append(delete ? "执行清理..." : "扫描清理文件...");
        try {
            Files.walk(Path.of(compareDirSelectPan.getSelectDir())).forEach(path -> {
                File file = path.toFile();
                if (file.isDirectory() && cleanDirs.contains(file.getName())) {
                    if (delete) {
                        try {
                            FileUtils.deleteDirectory(file);
                            outputArea.append(file.getAbsolutePath() + " deleted");
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "删除文件夹失败: " + file.getAbsolutePath(), e);
                            outputArea.append("删除失败: " + file.getAbsolutePath() + " - " + e.getMessage());
                        }
                    } else {
                        outputArea.append(file.getAbsolutePath());
                    }
                }
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, "遍历文件夹失败: " + compareDirSelectPan.getSelectDir(), e);
            outputArea.append("遍历失败: " + e.getMessage());
        }
        outputArea.append(delete ? "清理完成" : "扫描完成");
    }

}