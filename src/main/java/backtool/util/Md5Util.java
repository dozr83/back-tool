package backtool.util;

import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MD5工具
 */
public class Md5Util {
    private static final Logger logger = Logger.getLogger(Md5Util.class.getName());

    /**
     * 计算文件MD5 - 使用macOS系统自带的 md5 -r 命令
     * 对标 Python 的 subprocess.run(["md5", "-r", file_path])
     */
    public static String calculateFileMd5ByMac(Path filePath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("md5", "-r", filePath.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.readLine();
        }
        process.waitFor();

        // 输出格式: "<md5_hash> <filepath>"，取第一个空格前的部分
        if (output != null && !output.isBlank()) {
            int spaceIndex = output.indexOf(' ');
            if (spaceIndex > 0) {
                return output.substring(0, spaceIndex);
            }
            return output.trim();
        }
        throw new RuntimeException("md5 命令执行失败，输出为空: " + filePath);
    }

    /**
     * 获取文件MD5值 - Java纯实现，可处理大文件
     *
     * 生成过程：
     * 1. 通过 FileInputStream 读取文件原始字节流，使用 try-with-resources 确保流自动关闭
     * 2. 获取 java.security.MessageDigest 的 MD5 实例，用于计算哈希
     * 3. 使用 8KB 缓冲区循环读取文件内容，每次读取后通过 update() 方法累积到 MD5 摘要中
     *    （分块读取避免将大文件全部加载到内存，支持任意大小文件）
     * 4. 调用 digest() 方法完成哈希计算，得到 16 字节的原始 MD5 值
     * 5. 使用 Apache Commons Codec 的 Hex.encodeHex() 将 16 字节转为 32 位十六进制字符串
     *    （每字节转为 2 位十六进制，如 0xA1 → "a1"）
     *
     * 最终输出：32 位小写十六进制字符串，如 "d41d8cd98f00b204e9800998ecf8427e"
     *          该值与 macOS `md5` 命令结果一致
     *
     * @param file 要计算MD5的文件
     * @return 32位小写十六进制MD5字符串
     * @throws RuntimeException 读取文件或计算失败时抛出
     */
    public static String getMD5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            // 第2步：获取MD5计算器
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            // 第3步：分块读取文件，累积哈希
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                md5.update(buffer, 0, length);
            }

            // 第4、5步：完成哈希计算并转为十六进制字符串
            return new String(Hex.encodeHex(md5.digest()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "计算文件MD5失败: " + file.getAbsolutePath(), e);
            throw new RuntimeException("计算文件MD5失败: " + file.getAbsolutePath(), e);
        }
    }
}
