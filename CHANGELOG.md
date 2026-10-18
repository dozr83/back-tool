# 变更记录

倒序排列，最新在上。每条 = 日期 + 标题 + 影响说明（哪些文件/调用方受影响）。

## 2026-10-18 DigestCalculator 重载精简

删除两个 `DigestConfig` 参数版重载（业务零调用的转发链），`getDirDigestFileMap(String, DigestType)` 改为直连 `DigestWalker` 真实现，不再经 Config 组装转发。测试改走默认规则重载（已核实 `FilterRules.DEFAULT` 对测试数据与原 `NONE` 行为一致，期望值不变）。`DigestConfig` 类与 `FilterRules.NONE` 因此无任何引用，去留待定。现状：门面仅 2 个计算方法 + `groupFilesByMd5` + `DigestType` 枚举，无重载。

## 2026-10-18 CompareBuilder 更名 CompareQuery（正名）

`CompareBuilder` 实为流式 API（`of()` 进、`left/right` 配置、`byPath()/byValue()` 终结执行），并非 Builder 模式——Builder 一词在 Java 生态专指「配置载体 + `build()` 产出」的 EJ Item 2 模式（如 `DigestConfig.Builder`），流式 API 不应占用该命名。更名仅涉及类名与注释，无逻辑变化；受影响引用：`DirCompareUtil`、`DirVersionProcessor`、`DirDigestComparer`（Javadoc）。

## 2026-10-18 摘要模块职责分离重构

原 `DigestCalculator` 大类按功能拆分为职责单一的类，对外 API 与调用方代码零改动：

| 类 | 职责 |
|---|---|
| `DigestCalculator`（瘦身） | 门面：`DigestType` 枚举类型目录 + `groupFilesByMd5` + 2 个统一入口方法 |
| `DigestConfig`（不变） | Builder 参数载体（目录 + 摘要类型 + 过滤规则） |
| `DigestWalker`（新增，包私有） | 遍历引擎：目录/文件分离过滤 + 注入计算函数 |
| `DirDigestComparer`（新增） | 按路径比对算法（`compareByPath` + `PathDiffResult`），`CompareBuilder` 为其流式门面 |
| `DigestManifest`（新增） | 清单文件读写（.size / .md5 / .version） |

同步清理：移除 `DigestCalculator` 中两段注释掉的废弃方法；`CompareBuilder`、`DirVersionCheckPane` 改为引用新类。重构依据及方案对比见 [back-tool-ref-b](https://github.com/dozr83/back-tool-ref-b)。
