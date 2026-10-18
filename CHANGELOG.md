# 变更记录

倒序排列，最新在上。每条 = 日期 + 标题 + 影响说明（哪些文件/调用方受影响）。

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
