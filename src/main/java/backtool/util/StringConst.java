package backtool.util;


/**
 * 字符串常量
 * Created by dozr83 on 2017/5/17.
 */
public class StringConst {
    /**
     * 1.关于换行符号
     * \n是换行，英文是New line，表示使光标到行首
     * \r是回车，英文是Carriage return，表示使光标下移一格
     * \r\n表示回车换行
     * 2.文件中的换行
     * linux,unix: \r\n
     * windows : \n
     * Mac OS ： \r
     */
    //br textarea
    public static String BR_TEXTAREA_HTML = "&#xd;";
    public static String N_BR = "<br/>";
    public static final String LF = "\n";


    public static String getConsoleTitle(String title) {
        return "*******************************************" + title + "*******************************************";
    }
}
