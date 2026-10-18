package backtool.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 清单文件读写（.size / .md5 / .version）
 */
public final class DigestManifest {
    private DigestManifest() {
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
