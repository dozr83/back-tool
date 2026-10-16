package backtool.service;

import backtool.util.FileUtil;
import backtool.util.LinkedHashMapUtil;
import backtool.util.Md5Util;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class DigestCalculator {
    private static final Logger logger = Logger.getLogger(DigestCalculator.class.getName());
    public enum DigestType {
        MD5 {
            @Override
            public String calculate(File file) {
                return Md5Util.getMD5(file);
            }
        }, LEN {
            @Override
            public String calculate(File file) {
                return String.valueOf(file.length());
            }
        }, LEN_MD5 {
            @Override
            public String calculate(File file) {
                return Md5Util.getMD5(file) + "," + file.length();
            }
        };

        public abstract String calculate(File file);
    }

    public static LinkedHashMap<String, List<File>> groupFilesByMd5(List<File> files) {
        LinkedHashMap<String, List<File>> md5RevertMap_md5 = new LinkedHashMap<>();
        files.forEach(file -> {
            String md5 = Md5Util.getMD5(file);
            List<File> md5Files = md5RevertMap_md5.computeIfAbsent(md5, key -> new ArrayList<>());
            md5Files.add(file);
        });
        return md5RevertMap_md5;
    }

    //    @Deprecated
//    public static LinkedHashMap<String, String> getDirDigestMap(String checkBaseDirStr, DigestType digestType, Predicate<File> noNeedCalMd5FileFilter) {
//        LinkedHashMap<File, String> digestFileMap = getDirDigestFileMap(checkBaseDirStr, digestType, noNeedCalMd5FileFilter);
//        return LinkedHashMapUtil.alterKey(digestFileMap, file -> FilenameUtil.getSubPathUnix(file.getAbsolutePath(), checkBaseDirStr));
//    }
    public static LinkedHashMap<String, String> getDirDigestRelativePathMap(DigestConfig config) {
        LinkedHashMap<File, String> digestFileMap = getDirDigestFileMap(config);
        return LinkedHashMapUtil.alterKey(digestFileMap,
                file -> FileUtil.getSubPathUnix(file.getAbsolutePath(), config.getBaseDir().getAbsolutePath()));
    }


//    public static LinkedHashMap<File, String> getDirDigestFileMap(String checkBaseDirStr, DigestType digestType, Predicate<File> noNeedCalMd5FileFilter) {
//        return getDirDigestFileMap(new File(checkBaseDirStr), digestType, noNeedCalMd5FileFilter);
//    }


    public static LinkedHashMap<File, String> getDirDigestFileMap(DigestConfig config) {
        LinkedHashMap<File, String> md5FileMap = new LinkedHashMap<>();
        File checkBaseDir = config.getBaseDir();
        DigestType digestType = config.getDigestType();
        // 目录过滤和文件过滤分离，分别在不同阶段生效，职责清晰
        Predicate<File> dirFilter = config.getDirFilter();
        Predicate<File> fileFilter = config.getFileFilter();

        // 原有遍历逻辑 100% 保留
        try {
            Files.walkFileTree(checkBaseDir.toPath(), new java.nio.file.SimpleFileVisitor<Path>() {
                @Override
                public java.nio.file.FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
                    // 目录过滤：命中则跳过整棵子树（如 .Trashes / .fseventsd / .idea / .git）
                    if (dirFilter != null && dirFilter.test(dir.toFile())) {
                        return java.nio.file.FileVisitResult.SKIP_SUBTREE;
                    }
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFile(Path path, java.nio.file.attribute.BasicFileAttributes attrs) {
                    // 文件过滤：命中则跳过该文件（如 .DS_Store / ._ 前缀 / iml 后缀）
                    if (fileFilter != null && fileFilter.test(path.toFile())) {
                        return java.nio.file.FileVisitResult.CONTINUE;
                    }
                    File file = path.toFile();
                    md5FileMap.put(file, digestType.calculate(file));
                    return java.nio.file.FileVisitResult.CONTINUE;
                }

                @Override
                public java.nio.file.FileVisitResult visitFileFailed(Path path, IOException exc) {
                    return java.nio.file.FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            logger.log(Level.SEVERE, "遍历目录计算文件摘要失败: baseDir=" + checkBaseDir.getAbsolutePath(), e);
            throw new RuntimeException("遍历目录计算文件摘要失败: " + checkBaseDir.getAbsolutePath(), e);
        }

        return md5FileMap;
    }

    /**
     * 按路径比对两个 Map，分类为 4 类（保持插入顺序）
     */
    public static class PathDiffResult {
        public final LinkedHashSet<String> samePaths = new LinkedHashSet<>();
        public final LinkedHashSet<String> modifiedPaths = new LinkedHashSet<>();
        public final LinkedHashSet<String> leftOnlyPaths = new LinkedHashSet<>();
        public final LinkedHashSet<String> rightOnlyPaths = new LinkedHashSet<>();
    }

    /**
     * 核心比对算法：以路径为键，比对两个 Map<String, ?> 的值
     * 内部统一转为 String 进行比较
     */
    public static PathDiffResult compareByPath(Map<String, ?> leftMap, Map<String, ?> rightMap) {
        PathDiffResult result = new PathDiffResult();

        // 遍历左侧：找出 SAME / MODIFIED / LEFT_ONLY
        for (Map.Entry<String, ?> entry : leftMap.entrySet()) {
            String path = entry.getKey();
            String leftValue = String.valueOf(entry.getValue());
            if (rightMap.containsKey(path)) {
                String rightValue = String.valueOf(rightMap.get(path));
                if (leftValue.equals(rightValue)) {
                    result.samePaths.add(path);
                } else {
                    result.modifiedPaths.add(path);
                }
            } else {
                result.leftOnlyPaths.add(path);
            }
        }

        // 遍历右侧：找出 RIGHT_ONLY
        for (String path : rightMap.keySet()) {
            if (!leftMap.containsKey(path)) {
                result.rightOnlyPaths.add(path);
            }
        }

        return result;
    }

    /**
     * 读取清单文件，返回 LinkedHashMap (保持文件内顺序)
     * 支持格式：
     *   - .size:    path,size
     *   - .md5/.version: name,md5,length (跳过 // 开头的注释行)
     */
    public static LinkedHashMap<String, String> readManifest(Path file) throws IOException {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            boolean isSizeFile = file.toString().endsWith(".size");
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("//")) {
                    continue;
                }
                if (isSizeFile) {
                    // .size 格式: path,size
                    int lastSep = line.lastIndexOf(',');
                    if (lastSep > 0) {
                        String path = line.substring(0, lastSep);
                        String size = line.substring(lastSep + 1);
                        result.put(path, size);
                    }
                } else {
                    // .md5/.version 格式: name,md5,length
                    String[] parts = line.split(",", 2); // 只拆成 name 和 value 两部分
                    if (parts.length >= 2) {
                        result.put(parts[0], parts[1]); // value 为 "md5,length" 或 "md5"
                    }
                }
            }
        }
        return result;
    }

    /**
     * 将 Map 写入为清单文件
     * 格式: key,value
     */
    public static void writeVersionStr(Path file, Map<String, ?> map) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
        }
    }

    /**
     * 从字符串内容解析清单（格式同 readManifest 的非 .size 分支）
     */
    public static LinkedHashMap<String, String> parseVersionStr(String content) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("//")) {
                continue;
            }
            String[] parts = line.split(",", 2);
            if (parts.length >= 2) {
                result.put(parts[0], parts[1]);
            }
        }
        return result;
    }

}


