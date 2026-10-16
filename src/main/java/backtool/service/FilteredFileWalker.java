package backtool.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 目录文件遍历：内置常见系统/构建文件过滤（规则见 FilterRules.DEFAULT） */
public class FilteredFileWalker {

    private static final Logger logger = Logger.getLogger(FilteredFileWalker.class.getName());

    public static void walk(File baseDir, Consumer<File> fileConsumer) {
        try {
            Files.walkFileTree(baseDir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    //命中目录过滤则跳过整棵子树（.Trashes / .idea / target / .git 等）
                    if (FilterRules.DEFAULT.dirPredicate().test(dir.toFile())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    File file = path.toFile();
                    //命中文件过滤则跳过（.DS_Store / ._ 前缀 / iml 后缀）
                    if (!FilterRules.DEFAULT.filePredicate().test(file)) {
                        fileConsumer.accept(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "遍历目录失败: baseDir=" + baseDir.getAbsolutePath(), e);
            throw new RuntimeException("遍历目录失败: " + baseDir.getAbsolutePath(), e);
        }
    }
}
