package backtool.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 目录摘要遍历引擎（包私有，仅供 DigestCalculator 门面使用）
 */
class DigestWalker {
    private static final Logger logger = Logger.getLogger(DigestWalker.class.getName());

    private DigestWalker() {
    }

    static LinkedHashMap<File, String> walkAndCalculate(File baseDir, Predicate<File> dirFilter,
                                                        Predicate<File> fileFilter, Function<File, String> calculator) {
        LinkedHashMap<File, String> digestFileMap = new LinkedHashMap<>();
        try {
            Files.walkFileTree(baseDir.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    // 目录过滤：命中则跳过整棵子树（如 .Trashes / .fseventsd / .idea / .git）
                    if (dirFilter != null && dirFilter.test(dir.toFile())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    // 文件过滤：命中则跳过该文件（如 .DS_Store / ._ 前缀 / iml 后缀）
                    if (fileFilter != null && fileFilter.test(path.toFile())) {
                        return FileVisitResult.CONTINUE;
                    }
                    File file = path.toFile();
                    digestFileMap.put(file, calculator.apply(file));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "遍历目录计算文件摘要失败: baseDir=" + baseDir.getAbsolutePath(), e);
            throw new RuntimeException("遍历目录计算文件摘要失败: " + baseDir.getAbsolutePath(), e);
        }
        return digestFileMap;
    }
}
