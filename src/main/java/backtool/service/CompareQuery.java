package backtool.service;

import backtool.service.DiffResult;
import backtool.service.DiffCategory;
import java.util.*;

/**
 * 流式比对查询：of() 进，left/right 配置，byPath()/byValue() 终结执行返回 DiffResult
 */
public class CompareQuery {
    private Map<String, ?> leftMap;
    private Map<String, ?> rightMap;

    public static CompareQuery of() {
        return new CompareQuery();
    }

    public CompareQuery left(Map<String, ?> leftMap) {
        this.leftMap = leftMap;
        return this;
    }

    public CompareQuery right(Map<String, ?> rightMap) {
        this.rightMap = rightMap;
        return this;
    }

    /**
     * 按路径比对
     */
    public DiffResult byPath() {
        DirDigestComparer.PathDiffResult diff = DirDigestComparer.compareByPath(leftMap, rightMap);

        Map<DiffCategory, List<String>> changes = new EnumMap<>(DiffCategory.class);
        changes.put(DiffCategory.SAME, new ArrayList<>(diff.samePaths));
        changes.put(DiffCategory.MODIFIED, new ArrayList<>(diff.modifiedPaths));
        changes.put(DiffCategory.LEFT_ONLY, new ArrayList<>(diff.leftOnlyPaths));
        changes.put(DiffCategory.RIGHT_ONLY, new ArrayList<>(diff.rightOnlyPaths));

        int passCount = diff.samePaths.size();
        int failCount = diff.modifiedPaths.size() + diff.leftOnlyPaths.size() + diff.rightOnlyPaths.size();

        return new DiffResult(passCount, failCount, changes);
    }

    /**
     * 按值比对 (用于历史版本校验)
     */
    public DiffResult byValue() {
        // leftMap 是历史版本，rightMap 是当前版本
        // leftMap: md5 -> list of paths
        // rightMap: md5 -> list of paths
        Map<String, List<String>> leftRevertMap = toRevertMap((Map<String, List<String>>) leftMap);
        Map<String, List<String>> rightRevertMap = toRevertMap((Map<String, List<String>>) rightMap);

        List<String> okPaths = new ArrayList<>();
        List<String> missingPaths = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : leftRevertMap.entrySet()) {
            String md5 = entry.getKey();
            List<String> paths = entry.getValue();
            if (rightRevertMap.containsKey(md5)) {
                okPaths.addAll(paths);
            } else {
                missingPaths.addAll(paths);
            }
        }

        Map<DiffCategory, List<String>> changes = new EnumMap<>(DiffCategory.class);
        changes.put(DiffCategory.OK, okPaths);
        changes.put(DiffCategory.MISSING, missingPaths);

        return new DiffResult(okPaths.size(), missingPaths.size(), changes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> toRevertMap(Map<String, ?> map) {
        Map<String, List<String>> revertMap = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            String key = entry.getKey();
            List<String> values = (List<String>) entry.getValue();
            revertMap.put(key, values);
        }
        return revertMap;
    }
}
