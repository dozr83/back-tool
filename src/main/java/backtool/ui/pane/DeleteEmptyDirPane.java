package backtool.ui.pane;

import backtool.ui.component.FxKit;
import backtool.util.FileUtil;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FileUtils;
import backtool.util.StringUtil;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


public class DeleteEmptyDirPane extends VBox {
    private static final Logger logger = Logger.getLogger(DeleteEmptyDirPane.class.getName());
    //选择对比文件夹
    private FxKit.DirInputFx compareDirSelectPan = new FxKit.DirInputFx(FxKit.inputLength);

    private FxKit.FGridPane imgGridPane = new FxKit.FGridPane();
    private VBox vBoxOutputPane = FxKit.getVBox();
    private TextField countFiled = FxKit.getNumberInputTextField(100);
    //只识别构建产物目录；.git/.idea/.mvn 等非构建产物不在此列，命中即剪枝不进子目录
    private static final Set<String> BUILD_DIRS = Set.of("target", "build", ".build", "dist", "node_modules", "__pycache__");
    //最近一次操作的状态提示
    private final Label statusLabel = new Label();

    private FxKit.FButton scanBuildButton;
    private FxKit.FButton scanEmptyButton;
    private FxKit.FButton deleteButton;
    private FxKit.FButton selectAllButton;
    private FxKit.FButton selectNoneButton;
    private FxKit.FButton invertButton;
    private final Label summaryLabel = new Label();
    //扫描根目录（渲染相对路径用）
    private File scanRoot;
    //扫描结果快照：删除以勾选的快照路径为准，不重新扫描；构建产物和空目录共用同一列表
    private final List<Candidate> candidates = new ArrayList<>();

    private enum CandidateType {
        BUILD("构建产物"), EMPTY("空目录");

        final String label;

        CandidateType(String label) {
            this.label = label;
        }
    }

    private record Candidate(File file, long sizeBytes, CandidateType type, CheckBox checkBox) {
    }

    public DeleteEmptyDirPane() {
        imgGridPane.addRow(new Label("选择文件夹"), compareDirSelectPan);
        imgGridPane.addRow(new Label("显示操作文件数量"), countFiled);
        scanBuildButton = new FxKit.FButton("扫描构建产物", event -> scanBuildDirs());
        scanEmptyButton = new FxKit.FButton("扫描空文件夹", event -> scanEmptyDirs());
        deleteButton = new FxKit.FButton("删除选中", event -> deleteSelected());
        var buttons = FxKit.getHBox(
                scanBuildButton,
                scanEmptyButton,
                deleteButton,
                new Label("构建产物规则：target / build / .build / dist / node_modules / __pycache__；空目录忽略系统元数据文件；只扫描不自动删除")
        );
        imgGridPane.addMergeRow(buttons, 2);
        selectAllButton = new FxKit.FButton("全选", event -> setAllSelected(true));
        selectNoneButton = new FxKit.FButton("取消全选", event -> setAllSelected(false));
        invertButton = new FxKit.FButton("反选", event ->
                candidates.forEach(candidate -> candidate.checkBox().setSelected(!candidate.checkBox().isSelected())));
        imgGridPane.addMergeRow(FxKit.getHBox(selectAllButton, selectNoneButton, invertButton), 2);
        imgGridPane.addMergeRow(FxKit.getHBox(summaryLabel, statusLabel), 2);
        imgGridPane.setAlignment(Pos.TOP_LEFT);
        this.getChildren().add(imgGridPane);
        this.getChildren().add(vBoxOutputPane);
        vBoxOutputPane.setPadding(new javafx.geometry.Insets(10, 5, 5, 10));
        refreshSummary();
    }

    private void scanBuildDirs() {
        File root = checkRoot();
        if (root == null) {
            return;
        }
        scanRoot = root;
        setScanning(true);
        statusLabel.setText("扫描构建产物...");
        Thread thread = new Thread(() -> {
            //先收集路径和大小，CheckBox 回到 JavaFX 主线程再创建
            List<Candidate> found = new ArrayList<>();
            try {
                Files.walkFileTree(scanRoot.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        File file = dir.toFile();
                        if (BUILD_DIRS.contains(file.getName())) {
                            found.add(new Candidate(file, FileUtils.sizeOfDirectory(file), CandidateType.BUILD, null));
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
                Platform.runLater(() -> {
                    replaceCandidates(CandidateType.BUILD, found);
                    setScanning(false);
                    statusLabel.setText("构建产物扫描完成，共 " + found.size() + " 个，默认全选，取消勾选不删除");
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "扫描构建产物失败: " + scanRoot.getAbsolutePath(), e);
                Platform.runLater(() -> {
                    setScanning(false);
                    statusLabel.setText("扫描失败: " + e.getMessage());
                });
            }
        }, "scan-build-dirs");
        thread.setDaemon(true);
        thread.start();
    }

    private void scanEmptyDirs() {
        File root = checkRoot();
        if (root == null) {
            return;
        }
        scanRoot = root;
        setScanning(true);
        statusLabel.setText("扫描空文件夹...");
        Thread thread = new Thread(() -> {
            List<Candidate> found = new ArrayList<>();
            FileUtil.walkAllDirIgnoreHiddenDir(scanRoot, file -> {
                if (FileUtil.isEmptyDirIgnoreMetadata(file)) {
                    found.add(new Candidate(file, FileUtils.sizeOfDirectory(file), CandidateType.EMPTY, null));
                }
            });
            Platform.runLater(() -> {
                replaceCandidates(CandidateType.EMPTY, found);
                setScanning(false);
                statusLabel.setText("空文件夹扫描完成，共 " + found.size() + " 个，默认全选，取消勾选不删除");
            });
        }, "scan-empty-dirs");
        thread.setDaemon(true);
        thread.start();
    }

    private File checkRoot() {
        if (StringUtil.isBlank(compareDirSelectPan.getSelectDir())) {
            statusLabel.setText("未选择文件夹");
            return null;
        }
        File root = new File(compareDirSelectPan.getSelectDir());
        if (!root.isDirectory()) {
            statusLabel.setText("选择的文件夹不存在");
            return null;
        }
        return root;
    }

    //同类型重新扫描时替换该类型的旧结果，另一类型的勾选保留
    private void replaceCandidates(CandidateType type, List<Candidate> found) {
        candidates.removeIf(candidate -> candidate.type() == type);
        for (Candidate candidate : found) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(true);
            checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> refreshSummary());
            candidates.add(new Candidate(candidate.file(), candidate.sizeBytes(), candidate.type(), checkBox));
        }
        candidates.sort(Comparator.comparingLong(Candidate::sizeBytes).reversed());
        renderCandidates();
        refreshSummary();
    }

    private void renderCandidates() {
        vBoxOutputPane.getChildren().clear();
        for (Candidate candidate : candidates) {
            Label typeLabel = new Label(candidate.type().label);
            typeLabel.setMinWidth(60);
            Label sizeLabel = new Label(formatSize(candidate.sizeBytes()));
            sizeLabel.setMinWidth(90);
            Label pathLabel = new Label(scanRoot.toPath().relativize(candidate.file().toPath()).toString());
            pathLabel.setTooltip(new Tooltip(candidate.file().getAbsolutePath()));
            vBoxOutputPane.getChildren().add(FxKit.getHBox(
                    candidate.checkBox(),
                    typeLabel,
                    sizeLabel,
                    new FxKit.FButton("打开", e -> FxKit.openFile(candidate.file())),
                    pathLabel
            ));
        }
    }

    private void setScanning(boolean scanning) {
        scanBuildButton.setDisable(scanning);
        scanEmptyButton.setDisable(scanning);
    }

    private void setAllSelected(boolean selected) {
        candidates.forEach(candidate -> candidate.checkBox().setSelected(selected));
    }

    private void refreshSummary() {
        long buildCount = candidates.stream().filter(c -> c.type() == CandidateType.BUILD && c.checkBox().isSelected()).count();
        long emptyCount = candidates.stream().filter(c -> c.type() == CandidateType.EMPTY && c.checkBox().isSelected()).count();
        long bytes = candidates.stream().filter(c -> c.checkBox().isSelected())
                .mapToLong(Candidate::sizeBytes).sum();
        summaryLabel.setText("已选 " + (buildCount + emptyCount) + " 个（构建产物 " + buildCount + "，空目录 " + emptyCount
                + "），将释放 " + formatSize(bytes));
        boolean empty = candidates.isEmpty();
        deleteButton.setDisable(buildCount + emptyCount == 0);
        selectAllButton.setDisable(empty);
        selectNoneButton.setDisable(empty);
        invertButton.setDisable(empty);
    }

    private void deleteSelected() {
        List<Candidate> selected = candidates.stream().filter(candidate -> candidate.checkBox().isSelected()).toList();
        if (selected.isEmpty()) {
            FxKit.alert("请先勾选要删除的目录");
            return;
        }
        long buildCount = selected.stream().filter(c -> c.type() == CandidateType.BUILD).count();
        long emptyCount = selected.size() - buildCount;
        long bytes = selected.stream().mapToLong(Candidate::sizeBytes).sum();
        var confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("将删除 " + selected.size() + " 个目录（构建产物 " + buildCount + "，空目录 " + emptyCount
                + "），释放 " + formatSize(bytes));
        confirm.setContentText("构建产物重新构建即可恢复；空目录只含系统元数据。确认删除？");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        scanBuildButton.setDisable(true);
        scanEmptyButton.setDisable(true);
        deleteButton.setDisable(true);
        selectAllButton.setDisable(true);
        selectNoneButton.setDisable(true);
        invertButton.setDisable(true);
        statusLabel.setText("正在删除...");
        Thread thread = new Thread(() -> {
            //逐条日志不再展示，只汇总成功/跳过/失败；失败明细写日志并放到状态文字的悬停提示里
            List<Candidate> removed = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            int skipped = 0;
            for (Candidate candidate : selected) {
                File file = candidate.file();
                //以勾选快照为准，删前再确认存在，期间已被删掉的跳过
                if (!file.exists()) {
                    removed.add(candidate);
                    skipped++;
                    continue;
                }
                try {
                    boolean ok;
                    if (candidate.type() == CandidateType.BUILD) {
                        FileUtils.deleteDirectory(file);
                        ok = true;
                    } else {
                        ok = FileUtil.deleteEmptyDirIgnoreMetadata(file);
                    }
                    if (ok) {
                        removed.add(candidate);
                    } else {
                        failures.add("目录非空，未删除: " + file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "删除文件夹失败: " + file.getAbsolutePath(), e);
                    failures.add("删除失败: " + file.getAbsolutePath() + " - " + e.getMessage());
                }
            }
            int skippedFinal = skipped;
            List<Candidate> removedFinal = removed;
            List<String> failuresFinal = failures;
            Platform.runLater(() -> {
                candidates.removeAll(removedFinal);
                renderCandidates();
                setScanning(false);
                refreshSummary();
                statusLabel.setTooltip(failuresFinal.isEmpty() ? null
                        : new Tooltip(String.join("\n", failuresFinal)));
                StringBuilder message = new StringBuilder("删除完成：成功 " + removedFinal.size());
                if (skippedFinal > 0) {
                    message.append("，已不存在跳过 ").append(skippedFinal);
                }
                if (!failuresFinal.isEmpty()) {
                    message.append("，失败 ").append(failuresFinal.size()).append("（悬停查看明细）");
                }
                statusLabel.setText(message.toString());
            });
        }, "delete-candidates");
        thread.setDaemon(true);
        thread.start();
    }

    private static String formatSize(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.2f GB", bytes / 1024.0 / 1024 / 1024);
        }
        if (bytes >= 1024L * 1024) {
            return String.format("%.1f MB", bytes / 1024.0 / 1024);
        }
        return String.format("%.0f KB", bytes / 1024.0);
    }

}
