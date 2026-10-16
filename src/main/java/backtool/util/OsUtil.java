package backtool.util;

/**
 * 操作系统工具类（标准、高效、安全）
 */
public class OsUtil {

    // 只加载一次，全局复用，避免反复读系统属性
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    // 静态判断，只计算一次（业界标准命名）
    public static final boolean IS_WINDOWS = OS_NAME.startsWith("windows");
    public static final boolean IS_MAC     = OS_NAME.startsWith("mac");
    public static final boolean IS_LINUX   = OS_NAME.startsWith("linux");
    public static final boolean IS_UNIX_LIKE = !IS_WINDOWS;

    // 所有方法直接返回静态常量，0 消耗
    public static boolean isUnixLike() {
        return IS_UNIX_LIKE;
    }

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static boolean isLinux() {
        return IS_LINUX;
    }

    public static boolean isMac() {
        return IS_MAC;
    }

    public static String getOsName() {
        return OS_NAME;
    }

    public static String getOsSimpleName() {
        if (IS_WINDOWS) return "windows";
        if (IS_MAC)     return "mac";
        if (IS_LINUX)   return "linux";
        return "unixLike";
    }
}