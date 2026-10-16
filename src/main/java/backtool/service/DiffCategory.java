package backtool.service;

/**
 * 差异分类
 */
public enum DiffCategory {
    SAME,           // 相同
    MODIFIED,       // 已修改
    LEFT_ONLY,      // 仅左侧存在
    RIGHT_ONLY,     // 仅右侧存在
    MISSING,        // 缺失 (用于按值反向比对)
    OK,             // 正常 (用于按值反向比对)
    NO_RECORD       // 无记录 (用于MD5校验等场景)
}
