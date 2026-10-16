package backtool.service;

import java.io.File;

/**
 * 文件摘要计算配置（不可变对象，线程安全）
 * Builder 模式。
 *
 * 过滤规则统一收口到 {@link FilterRules}，本类只持有配置（目录 + 摘要类型 + 过滤规则）。
 * 目录过滤（dirFilter）和文件过滤（fileFilter）分离，职责清晰：
 *   - dirFilter  在 preVisitDirectory 阶段生效，命中则跳过整棵子树
 *   - fileFilter 在 visitFile 阶段生效，命中则跳过该文件
 */
public class DigestConfig {
    // 核心配置
    private final File baseDir;
    private final DigestCalculator.DigestType digestType;
    private final FilterRules filterRules;

    // 私有构造，仅 Builder 可创建
    private DigestConfig(Builder builder) {
        this.baseDir = builder.baseDir;
        this.digestType = builder.digestType;
        this.filterRules = builder.filterRules;
    }

    // Getter
    public File getBaseDir() {
        return baseDir;
    }

    public DigestCalculator.DigestType getDigestType() {
        return digestType;
    }

    public FilterRules getFilterRules() {
        return filterRules;
    }

    /** 目录过滤断言（委托给 FilterRules） */
    public java.util.function.Predicate<File> getDirFilter() {
        return filterRules.dirPredicate();
    }

    /** 文件过滤断言（委托给 FilterRules） */
    public java.util.function.Predicate<File> getFileFilter() {
        return filterRules.filePredicate();
    }

    // ------------------- Guava 风格 Builder -------------------
    public static class Builder {
        private File baseDir;
        private DigestCalculator.DigestType digestType;
        private FilterRules filterRules = FilterRules.DEFAULT; // 默认规则

        // 设置目录（支持 String/File 两种入参）
        public Builder baseDir(String dirPath) {
            this.baseDir = new File(dirPath);
            return this;
        }

        public Builder baseDir(File baseDir) {
            this.baseDir = baseDir;
            return this;
        }

        // 设置摘要类型
        public Builder digestType(DigestCalculator.DigestType digestType) {
            this.digestType = digestType;
            return this;
        }

        // 设置过滤规则（默认 FilterRules.DEFAULT）
        public Builder filterRules(FilterRules filterRules) {
            if (filterRules != null) {
                this.filterRules = filterRules;
            }
            return this;
        }

        // 构建不可变配置
        public DigestConfig build() {
            // 必填参数校验
            if (null == baseDir) {
                throw new IllegalArgumentException("目录不能为空");
            }
            if (null == digestType) {
                throw new IllegalArgumentException("摘要类型不能为空");
            }
            return new DigestConfig(this);
        }
    }

    // 快速创建 Builder
    public static Builder builder() {
        return new Builder();
    }
}
