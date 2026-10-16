package backtool.util;

/**
 * 字符串工具类（替代 org.apache.commons.lang3.StringUtils）
 */
public class StringUtil {

    /**
     * 换行符常量
     */
    public static final String LF = "\n";

    /**
     * 判断字符串是否为空白（null、空串或只包含空白字符）
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否非空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 判断字符串是否为数字
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
