package backtool.service;

import backtool.util.OsUtil;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 面板状态持久化（与 ConfigStore 独立）。
 * 存储路径：
 *   Mac: ~/Library/Application Support/FlyBackTool/pane_state.properties
 *   Win: %APPDATA%\FlyBackTool\pane_state.properties
 *
 * 每次 put / putBool 都直接写文件。
 */
public class PaneStateStore {

    private static final Logger logger = Logger.getLogger(PaneStateStore.class.getName());
    private static final Path FILE_PATH;
    private static final Properties PROPERTIES = new Properties();

    static {
        if (OsUtil.isMac()) {
            FILE_PATH = Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support", "FlyBackTool", "pane_state.properties");
        } else if (OsUtil.isWindows()) {
            FILE_PATH = Paths.get(System.getenv("APPDATA"), "FlyBackTool", "pane_state.properties");
        } else {
            FILE_PATH = Paths.get(System.getProperty("user.home"), ".config", "FlyBackTool", "pane_state.properties");
        }
        load();
    }

    private static void load() {
        if (!Files.exists(FILE_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
            PROPERTIES.load(reader);
        } catch (IOException e) {
            logger.log(Level.WARNING, "加载 pane_state.properties 失败: " + FILE_PATH, e);
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
                PROPERTIES.store(writer, null);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "保存 pane_state.properties 失败: " + FILE_PATH, e);
        }
    }

    public static String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static boolean getBool(String key, boolean defaultValue) {
        String v = PROPERTIES.getProperty(key);
        if (v == null) return defaultValue;
        return Boolean.parseBoolean(v);
    }

    public static void put(String key, String value) {
        if (value == null || value.isEmpty()) {
            PROPERTIES.remove(key);
        } else {
            PROPERTIES.setProperty(key, value);
        }
        save();
    }

    public static void putBool(String key, boolean value) {
        PROPERTIES.setProperty(key, String.valueOf(value));
        save();
    }
}
