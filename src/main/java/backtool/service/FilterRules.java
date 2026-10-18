package backtool.service;

import backtool.util.FileUtil;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 文件过滤规则集合（目录过滤 + 文件过滤）
 *
 * 所有忽略对比的规则统一收口到此类。
 * describeRules() 从规则定义自动计算，不写死字符串。
 *
 * 用法：
 *   - DigestCalculator 直接取 DEFAULT 的 dirPredicate()/filePredicate()
 *   - FileUtil.walkAllFile 等单一 Predicate 接口走 combinedPredicate()
 *   - UI 提示走 describeRules()
 */
public class FilterRules {

    /** 规则类型：目录过滤 / 文件过滤 */
    public enum Type { DIR, FILE }

    /** 单条规则：类型 + 描述 + 断言 */
    public static final class Rule {
        final Type type;
        final String description;
        final Predicate<File> predicate;

        Rule(Type type, String description, Predicate<File> predicate) {
            this.type = type;
            this.description = description;
            this.predicate = predicate;
        }
    }

    private final List<Rule> rules;

    private FilterRules(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /** 目录过滤断言（命中则跳过整棵子树） */
    public Predicate<File> dirPredicate() {
        return file -> rules.stream()
                .filter(r -> r.type == Type.DIR)
                .anyMatch(r -> r.predicate.test(file));
    }

    /** 文件过滤断言（命中则跳过该文件） */
    public Predicate<File> filePredicate() {
        return file -> rules.stream()
                .filter(r -> r.type == Type.FILE)
                .anyMatch(r -> r.predicate.test(file));
    }

    /** 组合断言（目录 + 文件），用于单一 Predicate 接口的场景如 FileUtil.walkAllFile */
    public Predicate<File> combinedPredicate() {
        return dirPredicate().or(filePredicate());
    }

    /** 从规则定义计算描述列表，用于 UI 提示用户当前过滤了哪些内容 */
    public List<String> describeRules() {
        return rules.stream()
                .map(r -> "【" + (r.type == Type.DIR ? "目录" : "文件") + "】" + r.description)
                .toList();
    }

    // ================ 内置规则实例 ================

    private static FilterRules rules(Rule... rules) {
        return new FilterRules(List.of(rules));
    }

    /** 默认规则：项目目录/文件 + Mac 系统目录/文件 */
    public static final FilterRules DEFAULT = rules(
            new Rule(Type.DIR, ".Trashes / .fseventsd（Mac系统）",
                    f -> {
                        String n = f.getName();
                        return n.equals(".Trashes") || n.equals(".fseventsd");
                    }),
            new Rule(Type.DIR, ".idea / target / .git（项目构建）",
                    new FileUtil.FileAndDirPredicate(Set.of(".idea", "target", ".git"), Set.of())),
            new Rule(Type.FILE, ".DS_Store / ._ 前缀（Mac系统）",
                    f -> {
                        String n = f.getName();
                        return n.equals(".DS_Store") || n.startsWith("._");
                    }),
            new Rule(Type.FILE, "iml 后缀（IDE配置）",
                    new FileUtil.FileAndDirPredicate(Set.of(), Set.of("iml")))
    );
}
