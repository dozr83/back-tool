package backtool.ui.pane;

import backtool.service.PaneStateStore;
import backtool.ui.component.FxKit;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import org.apache.commons.io.FileUtils;
import backtool.util.OsUtil;
import backtool.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeleteBuildPane extends VBox {
    private static final Logger logger = Logger.getLogger(DeleteBuildPane.class.getName());
    //和 DeleteEmptyDirPane 共用，用户经常是同一个目录
    private static final String K_ROOT = "DeletePane.rootDir";
    private static final String K_BUILD_DIRS = "DeleteBuildPane.buildDirs";
    private static final String K_EXTRA_DIRS = "DeleteBuildPane.extraDirs";
    private static final String K_FILE_NAMES = "DeleteBuildPane.fileNames";
    private static final String DEFAULT_BUILD_DIRS = "target,build,.build,dist,node_modules,__pycache__";
    private static final String DEFAULT_EXTRA_DIRS = ".git,.idea,.vcs,.vscode";
    private static final String DEFAULT_FILE_NAMES = ".iml";

    private FxKit.DirInputFx rootDirInput = new FxKit.DirInputFx(FxKit.inputLength);
    private final CheckBox buildDirsCheckBox = new CheckBox();
    private final CheckBox extraDirsCheckBox = new CheckBox();
    private final CheckBox fileNamesCheckBox = new CheckBox();
    private final TextField buildDirsField = new TextField();
    private final TextField extraDirsField = new TextField();
    private final TextField fileNamesField = new TextField();
    private FxKit.FGridPane grid = new FxKit.FGridPane();
    private VBox resultPane = FxKit.getVBox();
    private final Label statusLabel = new Label();
    private final Label summaryLabel = new Label();

    private FxKit.FButton scanButton;
    private FxKit.FButton deleteButton;
    private FxKit.FButton selectAllButton;
    private FxKit.FButton selectNoneButton;
    private FxKit.FButton invertButton;

    private File scanRoot;
    private final List<Candidate> candidates = new ArrayList<>();
    private final VBox detailPane = new VBox();

    private record Candidate(File file, long sizeBytes, CheckBox checkBox) {
    }

    public DeleteBuildPane() {
        rootDirInput.setSelectDir(PaneStateStore.get(K_ROOT, ""));
        buildDirsCheckBox.setSelected(true);
        extraDirsCheckBox.setSelected(true);
        fileNamesCheckBox.setSelected(true);
        buildDirsField.setText(PaneStateStore.get(K_BUILD_DIRS, DEFAULT_BUILD_DIRS));
        extraDirsField.setText(PaneStateStore.get(K_EXTRA_DIRS, DEFAULT_EXTRA_DIRS));
        fileNamesField.setText(PaneStateStore.get(K_FILE_NAMES, DEFAULT_FILE_NAMES));
        rootDirInput.textField.textProperty().addListener((o, a, b) -> PaneStateStore.put(K_ROOT, b));

        scanButton = new FxKit.FButton("扫描构建产物", event -> scan());
        deleteButton = new FxKit.FButton("删除选中", event -> deleteSelected());
        selectAllButton = new FxKit.FButton("全选", event -> candidates.forEach(c -> c.checkBox().setSelected(true)));
        selectNoneButton = new FxKit.FButton("取消全选", event -> candidates.forEach(c -> c.checkBox().setSelected(false)));
        invertButton = new FxKit.FButton("反选", event -> candidates.forEach(c -> c.checkBox().setSelected(!c.checkBox().isSelected())));

        grid.addRow(new Label("选择文件夹"), rootDirInput);
        grid.addRow(FxKit.getHBox(buildDirsCheckBox, new Label("构建目录")), buildDirsField);
        grid.addRow(FxKit.getHBox(extraDirsCheckBox, new Label("附加目录")), extraDirsField);
        grid.addRow(FxKit.getHBox(fileNamesCheckBox, new Label("文件名（结尾匹配，如 .iml）")), fileNamesField);
        grid.addMergeRow(FxKit.getHBox(scanButton, deleteButton, selectAllButton, selectNoneButton, invertButton), 2);
        grid.addMergeRow(FxKit.getHBox(summaryLabel, statusLabel), 2);
        grid.setAlignment(Pos.TOP_LEFT);

        resultPane.setPadding(new javafx.geometry.Insets(10, 5, 5, 10));
        this.getChildren().add(grid);
        this.getChildren().add(resultPane);
        this.getChildren().add(detailPane);
        refreshSummary();
    }

    /** 逗号分隔解析，支持半角逗号 "," 和全角逗号 "，" */
    private Set<String> parseCommaSeparated(String raw) {
        Set<String> set = new HashSet<>();
        if (raw != null && !raw.isBlank()) {
            for (String name : raw.split("[,，]")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    set.add(trimmed);
                }
            }
        }
        return set;
    }

    //两个目录 TextField 合并后作为扫描目标，各自受复选框控制
    private Set<String> activeDirSet() {
        Set<String> result = new HashSet<>();
        if (buildDirsCheckBox.isSelected()) {
            result.addAll(parseCommaSeparated(buildDirsField.getText()));
        }
        if (extraDirsCheckBox.isSelected()) {
            result.addAll(parseCommaSeparated(extraDirsField.getText()));
        }
        return result;
    }

    private Set<String> activeFileSet() {
        if (!fileNamesCheckBox.isSelected()) {
            return Set.of();
        }
        return parseCommaSeparated(fileNamesField.getText());
    }

    /** 文件名结尾是否命中集合中任意一项，用户输入需包含 "."，如 ".iml" */
    private static boolean endsWithAny(String fileName, Set<String> suffixes) {
        if (suffixes.isEmpty()) return false;
        for (String suffix : suffixes) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private void scan() {
        File root = checkRoot();
        if (root == null) {
            return;
        }
        //点扫描时保存三个 TextField 到配置
        PaneStateStore.put(K_BUILD_DIRS, buildDirsField.getText());
        PaneStateStore.put(K_EXTRA_DIRS, extraDirsField.getText());
        PaneStateStore.put(K_FILE_NAMES, fileNamesField.getText());
        scanRoot = root;
        Set<String> targetDirs = activeDirSet();
        Set<String> targetFiles = activeFileSet();
        setBusy(true);
        statusLabel.setText("扫描...");
        Thread thread = new Thread(() -> {
            List<Candidate> found = new ArrayList<>();
            try {
                Files.walkFileTree(scanRoot.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        File file = dir.toFile();
                        if (targetDirs.contains(file.getName())) {
                            found.add(new Candidate(file, FileUtils.sizeOfDirectory(file), null));
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        File file = path.toFile();
                        if (endsWithAny(file.getName(), targetFiles)) {
                            found.add(new Candidate(file, file.length(), null));
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
                Platform.runLater(() -> {
                    detailPane.getChildren().clear();
                    replaceCandidates(found);
                    setBusy(false);
                    statusLabel.setText("扫描完成，共 " + found.size() + " 个（目录+指定文件），默认全选，取消勾选不删除");
                });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "扫描失败: " + scanRoot.getAbsolutePath(), e);
                Platform.runLater(() -> {
                    setBusy(false);
                    statusLabel.setText("扫描失败: " + e.getMessage());
                });
            }
        }, "scan-build-dirs");
        thread.setDaemon(true);
        thread.start();
    }

    private void deleteSelected() {
        List<Candidate> selected = candidates.stream().filter(c -> c.checkBox().isSelected()).toList();
        if (selected.isEmpty()) {
            FxKit.alert("请先勾选要删除的目录或文件");
            return;
        }
        long bytes = selected.stream().mapToLong(Candidate::sizeBytes).sum();
        //附加目录（通常是 VCS/IDE 类危险目录）被选中时警告
        boolean hasExtra = false;
        if (extraDirsCheckBox.isSelected()) {
            Set<String> extraDirNames = parseCommaSeparated(extraDirsField.getText());
            hasExtra = selected.stream().anyMatch(c -> extraDirNames.contains(c.file().getName()));
        }
        if (!hasExtra && fileNamesCheckBox.isSelected()) {
            Set<String> extraFileNames = parseCommaSeparated(fileNamesField.getText());
            hasExtra = selected.stream().anyMatch(c -> c.file().isFile() && endsWithAny(c.file().getName(), extraFileNames));
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("将删除 " + selected.size() + " 项，释放 " + formatSize(bytes));
        if (hasExtra) {
            confirm.setContentText("⚠ 选中项包含用户自定义目录/文件（如 .git / .iml 等），删除后不可恢复！\n确认真的要删吗？");
        } else {
            confirm.setContentText("构建产物重新构建即可恢复。确认删除？");
        }
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        setBusy(true);
        statusLabel.setText("正在删除...");
        Thread thread = new Thread(() -> {
            List<Candidate> removed = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            int skipped = 0;
            for (Candidate candidate : selected) {
                File file = candidate.file();
                if (!file.exists()) {
                    removed.add(candidate);
                    skipped++;
                    continue;
                }
                try {
                    if (file.isDirectory()) {
                        FileUtils.deleteDirectory(file);
                    } else {
                        boolean deleted = file.delete();
                        if (!deleted && OsUtil.isMac()) {
                            try {
                                new ProcessBuilder("chflags", "nouchg", file.getAbsolutePath()).start().waitFor();
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "清除锁定标志失败: " + file.getAbsolutePath(), e);
                            }
                            deleted = file.delete();
                        }
                        if (!deleted) {
                            throw new IOException("删除失败");
                        }
                    }
                    removed.add(candidate);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "删除失败: " + file.getAbsolutePath(), e);
                    failures.add("删除失败: " + file.getAbsolutePath() + " - " + e.getMessage());
                }
            }
            int skippedFinal = skipped;
            List<Candidate> removedFinal = removed;
            List<String> failuresFinal = failures;
            Platform.runLater(() -> {
                candidates.removeAll(removedFinal);
                renderCandidates();
                setBusy(false);
                refreshSummary();
                StringBuilder message = new StringBuilder("删除完成：成功 " + removedFinal.size());
                if (skippedFinal > 0) {
                    message.append("，已不存在跳过 ").append(skippedFinal);
                }
                if (!failuresFinal.isEmpty()) {
                    message.append("，失败 ").append(failuresFinal.size()).append(" 个");
                }
                statusLabel.setText(message.toString());
                detailPane.getChildren().clear();
                for (String f : failuresFinal) {
                    detailPane.getChildren().add(new Label("  ❌ " + f));
                }
            });
        }, "delete-candidates");
        thread.setDaemon(true);
        thread.start();
    }

    private void replaceCandidates(List<Candidate> found) {
        candidates.clear();
        for (Candidate c : found) {
            CheckBox checkBox = new CheckBox();
            checkBox.setSelected(true);
            checkBox.selectedProperty().addListener((obs, oldValue, newValue) -> refreshSummary());
            candidates.add(new Candidate(c.file(), c.sizeBytes(), checkBox));
        }
        candidates.sort(Comparator.comparingLong(Candidate::sizeBytes).reversed());
        renderCandidates();
        refreshSummary();
    }

    private void renderCandidates() {
        resultPane.getChildren().clear();
        for (Candidate candidate : candidates) {
            Label sizeLabel = new Label(formatSize(candidate.sizeBytes()));
            sizeLabel.setMinWidth(90);
            Label pathLabel = new Label(scanRoot.toPath().relativize(candidate.file().toPath()).toString());
            pathLabel.setTooltip(new Tooltip(candidate.file().getAbsolutePath()));
            resultPane.getChildren().add(FxKit.getHBox(
                    candidate.checkBox(),
                    sizeLabel,
                    new FxKit.FButton("打开", e -> FxKit.openFile(candidate.file())),
                    pathLabel
            ));
        }
    }

    private void refreshSummary() {
        long selectedCount = candidates.stream().filter(c -> c.checkBox().isSelected()).count();
        long bytes = candidates.stream().filter(c -> c.checkBox().isSelected())
                .mapToLong(Candidate::sizeBytes).sum();
        summaryLabel.setText("已选 " + selectedCount + " 个，将释放 " + formatSize(bytes));
        boolean listEmpty = candidates.isEmpty();
        deleteButton.setDisable(selectedCount == 0);
        selectAllButton.setDisable(listEmpty);
        selectNoneButton.setDisable(listEmpty);
        invertButton.setDisable(listEmpty);
    }

    private void setBusy(boolean busy) {
        scanButton.setDisable(busy);
        deleteButton.setDisable(busy);
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
