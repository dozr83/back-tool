package backtool.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 按路径比对两个摘要 Map（比对算法本体，调用方直接使用）
 */
public final class DirDigestComparer {
    private DirDigestComparer() {
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
}
