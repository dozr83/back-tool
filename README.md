# 文件备份小助手（back-tool）

基于 JavaFX 的本地文件备份与去重小工具，纯本地运行、无服务端依赖。

## 功能

- **删除重复文件**：清空空文件夹、删除文件夹中的重复文件、删除两个文件夹的重复文件
- **文件对比**：计算 MD5 资产清单、检查资产清单、对比两个文件夹差异
- **命令工具**：一键脚本执行器、翻译、英语听力格式转换
- **系统**：系统配置、查看系统版本

## 环境要求

- JDK 17+
- Maven 3.8+
- JavaFX 21（pom 中 classifier 固定为 `mac-aarch64`，其它平台需调整 `javafx.platform`）

## 构建与运行

```bash
# 编译打包，产物在 target/，macOS dmg 在 target/dist/
mvn clean package

# 直接运行
java -cp "target/classes:target/lib/*" backtool.ui.main.BackToolMain
```

## 配置

编辑 `src/main/resources/config.properties`：

- `python.script.dir`：外部 Python 脚本根目录
- `baidu.trans.appid` / `baidu.trans.secret`：百度翻译开放平台密钥，留空则翻译功能不可用

## 许可

[Apache License 2.0](LICENSE)
