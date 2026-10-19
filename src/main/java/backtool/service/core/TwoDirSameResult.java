package backtool.service.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TwoDirSameResult {
    /** 按 MD5 或 文件名+MD5 匹配上的重复文件对 */
    public List<CrossIdentity> sames = new ArrayList<>();
    /** 仅按 文件名+MD5 对比时，左边独有的文件（路径不同导致的缺失） */
    public List<File> leftUniq = new ArrayList<>();
    /** 仅按 文件名+MD5 对比时，右边独有的文件 */
    public List<File> rightUniq = new ArrayList<>();

    public void addSame(List<File> lefts, List<File> rights) {
        sames.add(new CrossIdentity(lefts, rights));
    }

    public record CrossIdentity(List<File> lefts, List<File> rights) {
        @Override
        public String toString() {
            return "CrossIdentity{" +
                    "lefts=" + lefts +
                    ", rights=" + rights +
                    '}';
        }
    }
}
