package backtool.service;

import backtool.util.OsUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 配置统一读取（只读取，不写入）
 * 标准路径：
 * Mac: ~/Library/Application Support/FlyBackTool/config.properties
 * Win: %APPDATA%\FlyBackTool/config.properties
 */
public class ConfigStore {

    public static final Path CONFIG_DIR_PATH;
    public static final Path CONFIG_FILE_PATH;
    public static final Properties PROPERTIES;

    static {
        // 仅支持 Windows + Mac，其他系统直接拒绝启动
        if (!OsUtil.isWindows() && !OsUtil.isMac()) {
            throw new UnsupportedOperationException("当前操作系统暂不支持，仅支持 Windows 和 Mac");
        }

        // 标准应用配置目录
        if (OsUtil.isMac()) {
            CONFIG_DIR_PATH = Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support", "FlyBackTool");
        } else {
            CONFIG_DIR_PATH = Paths.get(System.getenv("APPDATA"), "FlyBackTool");
        }

        CONFIG_FILE_PATH = CONFIG_DIR_PATH.resolve("config.properties");
        PROPERTIES = loadConfig();
    }

    private static Properties loadConfig() {
        Properties prop = new Properties();
        if (Files.exists(CONFIG_FILE_PATH)) {
            try (var reader = Files.newBufferedReader(CONFIG_FILE_PATH, StandardCharsets.UTF_8)) {
                prop.load(reader);
            } catch (IOException ignored) {
            }
        }
        return prop;
    }

    // -------------------- 对外统一接口 --------------------
    public static boolean isBetaOpen() {
        return Boolean.parseBoolean(PROPERTIES.getProperty("betaOpen", "false"));
    }

    public static String getDownRemoteUrl() {
        return PROPERTIES.getProperty("downConfig.remoteUrl");
    }

    public static String getDownLocalDir() {
        return PROPERTIES.getProperty("downConfig.localDir");
    }

    // 清空配置 = 删除文件
    public static void clear() {
        try {
            Files.deleteIfExists(CONFIG_FILE_PATH);
        } catch (IOException ignored) {
        }
    }

    // ==========================================
    // 🔥 你要的方法：创建目录 + 返回路径（给前台用）
    // ==========================================
    public static Path createDirAndEmptyConfigFile() throws IOException {
        Files.createDirectories(CONFIG_DIR_PATH);
        if (!Files.exists(CONFIG_FILE_PATH)) {
            Files.createFile(CONFIG_FILE_PATH);
        }
        return CONFIG_DIR_PATH;
    }
}