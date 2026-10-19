package backtool.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import backtool.util.OsUtil;

/**
 * 统一封装文件访问
 *
 * @author dozr83
 */
public class FileUtil {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());
    private static Charset fileCharset = Charset.forName("utf-8");

    /**
     * 展开路径中的 ~ 为用户目录
     * 类似 Python 的 Path.expanduser()
     */
    public static Path expandUser(String pathStr) {
        if (pathStr.startsWith("~" + File.separator) || pathStr.equals("~")) {
            pathStr = System.getProperty("user.home") + pathStr.substring(1);
        }
        return Path.of(pathStr);
    }
    /**
     * 操作系统生成的元数据文件，这些文件不包含用户实际内容，可视为空文件
     * 包括：
     * - .DS_Store: macOS 文件夹自定义属性（图标位置、背景色等）
     * - Thumbs.db: Windows 缩略图缓存
     */
    public static final Set<String> systemMetadataFiles = new HashSet<>();

    static {
        systemMetadataFiles.add(".DS_Store");
        systemMetadataFiles.add("Thumbs.db");
    }

    public static void walkAllFileAndDir(File walkDir, Consumer<File> consumer) {
        File[] files = walkDir.listFiles();
        if (null == files) {
            return;
        }
        for (File cfile : files) {
            if (cfile.isDirectory()) {
                consumer.accept(cfile);
                //递归
                walkAllFileAndDir(cfile, consumer);
            } else {
                consumer.accept(cfile);
            }
        }
    }

    /**
     * 遍历文件，不返回文件列表，节省内存。
     *
     * @param walkDir
     * @param refusePredicate
     * @param consumer        返回是否需要继续执行
     */
    public static void walkAllFile(File walkDir, Predicate<File> refusePredicate, Consumer<File> consumer) {
        File[] files = walkDir.listFiles();
        if (null == files) {
            return;
        }
        for (File cfile : files) {
            if (null != refusePredicate && refusePredicate.test(cfile)) {
                continue;
            }
            if (cfile.isDirectory()) {
                //递归
                walkAllFile(cfile, refusePredicate, consumer);
            } else {
                consumer.accept(cfile);
            }
        }
    }

    public static void walkAllFileIgnoreHidden(File walkDir, Predicate<File> refusePredicate, Consumer<File> consumer) {
        File[] files = walkDir.listFiles();
        if (null == files) {
            return;
        }
        for (File cfile : files) {
            if (null != refusePredicate && refusePredicate.test(cfile)) {
                continue;
            }
            if (cfile.isDirectory()) {
                //递归
                if (!cfile.getName().startsWith(".")) {
                    walkAllFileIgnoreHidden(cfile, refusePredicate, consumer);
                }
            } else {
                if (!cfile.getName().startsWith(".")) {
                    consumer.accept(cfile);
                }
            }
        }
    }


    public static void walkAllDir(File walkDir, Predicate<File> refusePredicate, Consumer<File> consumer) {
        File[] files = walkDir.listFiles();
        if (null == files) {
            return;
        }
        for (File cfile : files) {
            if (null != refusePredicate && refusePredicate.test(cfile)) {
                continue;
            }
            if (cfile.isDirectory()) {
                consumer.accept(cfile);
                //递归
                walkAllDir(cfile, refusePredicate, consumer);
            } else {

            }
        }
    }

    public static void walkAllFileIgnoreMacShadowFile(File walkDir, Predicate<File> refusePredicate, Consumer<File> consumer) {
        walkAllFile(walkDir, refusePredicate, file -> {
            if (!file.getAbsolutePath().contains("._")) {
                consumer.accept(file);
            }
        });
    }

    public static void walkAllDirIgnoreHiddenDir(File walkDir, Consumer<File> consumer) {
        walkAllDir(walkDir, file -> {
            if (file.isDirectory() && file.getName().startsWith(".")) {
                return true;
            }
            return false;
        }, consumer);
    }

    public static void walkFiles(File walkDir, Consumer<File> consumer) throws IOException {
        File[] files = walkDir.listFiles();
        if (null == files) {
            return;
        }
        for (File file : walkDir.listFiles()) {
            consumer.accept(file);
        }
    }

    public static void deleteRepeatFiles(Map<File, File> deleteFileMaps) {
        deleteFileMaps.forEach((deleteFile, repeatFile) -> {
            deleteRepeatFile(deleteFile, repeatFile);
        });
    }

    public static boolean deleteEmptyDirIgnoreMetadata(File file) {
        for (File f : file.listFiles()) {
            if (f.isFile() && isSystemMetadataFile(f)) {
                deleteFile(f);
            }
        }
        if (isEmptyDirIgnoreMetadata(file)) {
            return deleteDir(file);
        } else {
            return false;
        }
    }

    /**
     * 判断目录是否为空（忽略系统元数据文件）
     * 忽略的文件类型：
     * - macOS 资源分叉文件（文件名包含 "._"，非 HFS+ 磁盘复制时生成）
     * - 系统元数据文件（.DS_Store, Thumbs.db 等）
     *
     * @param file 目录文件
     * @return 如果目录不存在或不是目录返回 false；如果目录为空（忽略系统元数据文件）返回 true
     */
    public static boolean isEmptyDirIgnoreMetadata(File file) {
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return true;
        }
        for (File f : files) {
            String name = f.getName();
            // 忽略 macOS 资源分叉文件（非 HFS+ 磁盘复制时生成）
            boolean isResourceFork = name.contains("._");
            // 忽略系统元数据文件（.DS_Store, Thumbs.db 等）
            boolean isMetadata = systemMetadataFiles.contains(name);
            if (!isResourceFork && !isMetadata) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否为操作系统元数据文件（.DS_Store, Thumbs.db 等）
     *
     * @param file 文件
     * @return 如果是元数据文件返回 true
     */
    private static boolean isSystemMetadataFile(File file) {
        return systemMetadataFiles.contains(file.getName());
    }

    public static int deleteEmptyDirsIgnoreMetadata(Path checkPath) throws IOException {
        AtomicInteger count = new AtomicInteger();
        Files.walk(checkPath).forEach(path -> {
            File file = path.toFile();
            if (isEmptyDirIgnoreMetadata(file)) {
                deleteEmptyDirIgnoreMetadata(file);
                count.addAndGet(1);
            }
        });
        return count.get();
    }

    public static void deleteRepeatFile(File delFile, File retainFile) {
        if (delFile.getAbsolutePath().equals(retainFile.getAbsolutePath())) {
            return;
        }
        if (delFile.length() == retainFile.length() && Md5Util.getMD5(delFile).equals(Md5Util.getMD5(retainFile))) {
            boolean deleted = delFile.delete();
            if (!deleted) {
                logger.severe("重复文件删除失败: " + delFile.getAbsolutePath());
                throw new RuntimeException("重复文件删除失败：" + delFile.getAbsolutePath());
            }
        }
    }

    public static boolean isImg(File file) {
        if (file.getName().endsWith(".svg")) {
            return true;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            return image != null;
        } catch (IOException ex) {
            return false;
        }
    }


    public static void writeFileToResponse(OutputStream servletOutputStream, File file) {

        /***
         * 把文件流写入客户端
         */
        try (InputStream inputStream = new FileInputStream(file)) {
            // 创建输入流，读取文件到内存
            // 创建输出流，输出内存到客户端
//            int readLength;
//            byte[] buf = new byte[4096];
//            while (((readLength = inputStream.read(buf)) != -1)) {
//                servletOutputStream.write(buf, 0, readLength);
//            }
            inputStream.transferTo(servletOutputStream);
//            IOUtils.copy(inputStream, servletOutputStream);
            servletOutputStream.flush();

        } catch (IOException e) {
            logger.log(Level.SEVERE, "文件输出到客户端异常: file=" + file.getAbsolutePath(), e);
            throw new RuntimeException("文件输出到客户端异常: " + file.getAbsolutePath(), e);
        } finally {
            if (null != servletOutputStream) {
                try {
                    servletOutputStream.close();
                } catch (IOException e1) {
//                    LogUtil.error(LogUtil.FILE_EXCEPTION, "servletOutputStream.close exception", e1);
                }
            }
        }
    }


    public static File getClassPathFile(String path) {
        try {
            String fullPath = FileUtil.class.getResource(path).getFile();
            return new File(fullPath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "获取类路径文件失败: path=" + path, e);
            throw new RuntimeException("获取类路径文件失败: " + path, e);
        }
    }

    public static List<File> getDirSortFiles(File md5Dir, String endWith) {
        File[] filesArray = md5Dir.listFiles(((dir, name) -> name.endsWith(endWith)));
        if (null == filesArray) {
            return List.of();
        }
        List<File> md5Files = Arrays.asList(filesArray);
        Collections.sort(md5Files, (f1, f2) -> {
            long t1 = f1.lastModified();
            long t2 = f2.lastModified();
            Long c = t2 - t1;
            return c.intValue();
        });
        return md5Files;
    }

    public static File getDirLastModifyFile(File md5Dir, String endWith) {
        List<File> md5Files = getDirSortFiles(md5Dir, endWith);
        if (null == md5Files || md5Files.size() == 0) {
            return null;
        }

        File md5File = md5Files.get(0);
        return md5File;
    }

    public static void deleteMoreBeforeFiles(File beZipSourceDir, int maxCount, String endWith) {
        //删除多余的Md5文件 deleteMore md5
        List<File> md5Files = getDirSortFiles(beZipSourceDir, endWith);
        if (md5Files.size() > maxCount) {
            for (int i = maxCount - 1; i < md5Files.size(); i++) {
                md5Files.get(i).delete();
            }
        }
    }

    public static String getSuffix(File file) {
        int lastDotIndex = file.getName().lastIndexOf(".");
        if (-1 == lastDotIndex) {
            return "";
        } else {
            return file.getName().substring(lastDotIndex + 1);
        }
    }

    public static String getNameWithoutSuffix(File file) {
        int lastDotIndex = file.getName().lastIndexOf(".");
        if (-1 == lastDotIndex) {
            return file.getName();
        } else {
            return file.getName().substring(0, lastDotIndex);
        }
    }

    /**
     * 获取文件相对于基础目录的 Unix 格式相对路径
     * 基于 JDK {@link Path#relativize} 实现，统一使用 / 分隔符。
     *
     * 示例：
     *   getSubPathUnix("/data/test/123.txt",   "/data")      → "test/123.txt"
     *   getSubPathUnix("/data/test/readme.md", "/data/test") → "readme.md"
     *   getSubPathUnix("/data/demo",           "/data/demo") → ""
     *
     * 注意：本方法基于当前操作系统的 Path 语义解析路径，要求两个路径都属于当前系统。
     * 跨平台路径字符串（如在 macOS 上传入 "D:\\资料\\a.txt"）无法正确解析。
     *
     * 异常情况说明：
     * 1. baseDir 不是 file 的祖先路径时，relativize 会返回带 ".." 的向上回溯路径，
     *    例：getSubPathUnix("/data/other/a.txt", "/data/test") → "../other/a.txt"
     *    （本项目 file 均由 walkFileTree(baseDir) 遍历得到，必然在 baseDir 子树下，不会触发）
     * 2. 路径含正则元字符不再有影响（旧实现用 replaceFirst 把 baseDir 当正则，存在误匹配风险，
     *    例：baseDir="/x.2/y" 旧实现会误匹配 "/xX2/y"；新实现不涉及正则，无此问题）
     * 3. 路径含 "." 或 ".." 段会被 normalize 规范化后再计算
     * 4. 两路径必须同为绝对或同为相对路径，本方法内部用 toAbsolutePath 统一为绝对路径
     */
    public static String getSubPathUnix(String filePathStr, String baseDirPathStr) {
        Path baseDir = Path.of(baseDirPathStr).toAbsolutePath().normalize();
        Path file = Path.of(filePathStr).toAbsolutePath().normalize();
        return baseDir.relativize(file).toString().replace("\\", "/");
    }

    public static String trimStartVirgule(String key) {
        if (key.startsWith("/") || key.startsWith("\\")) {
            key = key.substring(1);
        }
        return key;
    }

    /**
     * 纯Java获取文件扩展名（无任何第三方依赖）
     * 效果和 FilenameUtils.getExtension 完全一致
     *
     * @param fileName 文件名
     * @return 扩展名（小写/原样），无扩展名返回空字符串
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        // 查找最后一个 . 的位置
        int lastDotIndex = fileName.lastIndexOf('.');
        // 没有. 或者 . 在第一个字符（隐藏文件），返回空
        if (lastDotIndex <= 0) {
            return "";
        }
        // 截取扩展名
        return fileName.substring(lastDotIndex + 1);
    }

    public static class FileAndDirPredicate implements Predicate<File> {
        //过滤的文件名称集合
        private Set<String> acceptDirOrFileNames;
        //过滤文件类型集合
        private Set<String> acceptExtensions;

        public FileAndDirPredicate(Set<String> acceptDirOrFileNames, Set<String> acceptExtensions) {
            this.acceptDirOrFileNames = acceptDirOrFileNames;
            this.acceptExtensions = acceptExtensions;
        }

        @Override
        public String toString() {
            return "文件/文件夹名=" + acceptDirOrFileNames +
                    ", 文件类型=" + acceptExtensions;

        }

        @Override
        public boolean test(File file) {
            if (null != acceptDirOrFileNames) {
                for (String filterDir : this.acceptDirOrFileNames) {
                    if (file.getName().contains(filterDir)) {
                        return true;
                    }
                }
            }
            if (this.acceptExtensions != null) {
                String extension = getFileExtension(file.getName());
                return this.acceptExtensions.contains(extension);
            }
            return false;
        }

    }

    /**
     * 删除单个文件，Mac uchg 锁定时先清标志再重试。只处理文件，目录调用会返回 false
     */
    public static boolean deleteFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return false;
        }
        boolean deleted = file.delete();
        if (!deleted && OsUtil.isMac()) {
            clearUchgUpward(file);
            deleted = file.delete();
        }
        return deleted;
    }

    /**
     * 删除一个空目录，Mac uchg 锁定时先清标志再重试。非空目录调用会返回 false
     */
    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return false;
        }
        boolean deleted = dir.delete();
        if (!deleted && OsUtil.isMac()) {
            clearUchgUpward(dir);
            deleted = dir.delete();
        }
        return deleted;
    }

    /**
     * 清除路径本身 + 所有父目录的 uchg 锁定标志，直到根。
     * 解决父目录被锁导致子文件/子目录删不掉的情况。
     */
    private static void clearUchgUpward(File path) {
        try {
            for (File cur = path; cur != null && cur.exists(); cur = cur.getParentFile()) {
                new ProcessBuilder("chflags", "nouchg", cur.getAbsolutePath()).start().waitFor();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "清除锁定标志失败: " + path.getAbsolutePath(), e);
        }
    }

    /**
     * 递归删除整个目录树（含子文件和子目录）。Mac uchg 锁定时子文件/子目录分别清标志重试
     */
    public static boolean deleteDirRecursive(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteDirRecursive(child);
                } else {
                    deleteFile(child);
                }
            }
        }
        return deleteDir(dir);
    }
}