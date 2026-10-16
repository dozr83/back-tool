package backtool.service;

import java.util.*;

/**
 * 统一的比对结果
 */
public class DiffResult {
    private final int passCount;
    private final int failCount;
    private final Map<DiffCategory, List<String>> changes;

    public DiffResult(int passCount, int failCount, Map<DiffCategory, List<String>> changes) {
        this.passCount = passCount;
        this.failCount = failCount;
        this.changes = Collections.unmodifiableMap(changes);
    }

    public int getPassCount() {
        return passCount;
    }

    public int getFailCount() {
        return failCount;
    }
    
    public int getTotalCount() {
        return passCount + failCount;
    }

    public List<String> getPathsByCategory(DiffCategory category) {
        return changes.getOrDefault(category, Collections.emptyList());
    }

    public List<String> getSamePaths() {
        return getPathsByCategory(DiffCategory.SAME);
    }

    public List<String> getModifiedPaths() {
        return getPathsByCategory(DiffCategory.MODIFIED);
    }

    public List<String> getLeftOnlyPaths() {
        return getPathsByCategory(DiffCategory.LEFT_ONLY);
    }

    public List<String> getRightOnlyPaths() {
        return getPathsByCategory(DiffCategory.RIGHT_ONLY);
    }
    
    public List<String> getMissingPaths() {
        return getPathsByCategory(DiffCategory.MISSING);
    }
    
    public List<String> getOkPaths() {
        return getPathsByCategory(DiffCategory.OK);
    }
    
    public List<String> getNoRecordPaths() {
        return getPathsByCategory(DiffCategory.NO_RECORD);
    }

    public boolean isAllPass() {
        return failCount == 0;
    }
}
