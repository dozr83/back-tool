package backtool.ui.pane;

import backtool.ui.component.FxKit;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class EnglishListeningPane extends FxKit.FGridPane {
    private FxKit.FileInputFx fileInputFx = new FxKit.FileInputFx(FxKit.inputLength);
    private TextField maxLenInput = FxKit.getNumberInputTextField(500);
    private FxKit.LogTextArea outputArea = new FxKit.LogTextArea("执行结果");

    public EnglishListeningPane() {
        outputArea.append("执行结果");
        this.addRow(fileInputFx);
        this.addRow(FxKit.getHBox(new Label("单文件最大行数"), maxLenInput));
        this.addMergeRow(FxKit.getHBox(
                new FxKit.FButton("英语听力格式转换", event -> process())
        ), 2);
        this.addMergeRow(outputArea, 2);
        this.addMergeRow(FxKit.getWrapLabel(
                "使用说明：支持 .docx、.md、.lrc 三种格式。.docx 和 .md 直接按段落提取；"
                        + ".lrc 会先去除时间轴标签提取字幕内容；随后生成 _import_long.txt 完整长句版，"
                        + "并按上方行数拆分为 _1.txt、_2.txt... 多个文件（按整句累计不拆半句）；"
                        + ".lrc 额外生成 _clean.txt 纯文本字幕。输出均在所选文件同目录的 auto_export 文件夹下。",
                (int) FxKit.contentLength
        ), 2);
    }

    private void process() {
        outputArea.clear();
        outputArea.append("执行结果");
        String filePath = fileInputFx.getSelectFile();
        if (filePath == null || filePath.isEmpty()) {
            outputArea.append("未选择文件");
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            outputArea.append("文件不存在");
            return;
        }
        String lowerName = file.getName().toLowerCase();

        int maxLines;
        try {
            maxLines = Integer.parseInt(maxLenInput.getText().trim());
        } catch (NumberFormatException ex) {
            maxLines = 500;
        }
        if (maxLines <= 0) {
            maxLines = 500;
        }

        try {
            File autoExportDir = new File(file.getParent(), "auto_export");
            autoExportDir.mkdirs();

            List<String> lines;
            if (lowerName.endsWith(".lrc")) {
                lines = readLrc(file);
                writeCleanSubtitle(file, lines, autoExportDir);
            } else if (lowerName.endsWith(".md")) {
                lines = readMd(file);
            } else if (lowerName.endsWith(".docx")) {
                lines = readWord(file);
            } else {
                outputArea.append("请选择.docx、.md或.lrc文件");
                return;
            }
            String longText = convertToLongSentence(lines);
            String longFileName = file.getName().replaceAll("\\.[^.]+$", "_import_long.txt");
            File longFile = new File(autoExportDir, longFileName);
            try (OutputStream os = new FileOutputStream(longFile)) {
                os.write(longText.getBytes());
            }
            outputArea.append("长句导出: " + longFile.getAbsolutePath());

            if (lines.size() > maxLines) {
                splitToFiles(file, lines, maxLines, autoExportDir);
            } else {
                outputArea.append("文件行数未超过" + maxLines + "，跳过分文件");
            }
        } catch (Exception e) {
            outputArea.append("转换失败: " + e.getMessage());
        }
    }

    // 将单个短句内部的.!?替换为逗号，末尾标点保留，与每日英语听力长句格式一致
    private String toLongSingle(String line) {
        char lastChar = line.charAt(line.length() - 1);
        String converted = line.replaceAll("[.!?]", ",");
        return converted.substring(0, converted.length() - 1) + lastChar;
    }

    private String convertToLongSentence(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) {
                sb.append("\r\n\r\n");
            }
            sb.append(toLongSingle(line));
        }
        return sb.toString();
    }

    // 按单文件最大行数maxLines，将长句拆分为多个文件，按整句累计不拆半句
    private void splitToFiles(File file, List<String> lines, int maxLines, File autoExportDir) throws Exception {
        String baseName = file.getName().replaceAll("\\.[^.]+$", "");
        int fileIndex = 1;
        int count = 0;
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            if (current.length() > 0) {
                current.append("\r\n\r\n");
            }
            current.append(toLongSingle(line));
            count++;
            if (count >= maxLines) {
                writeSplitFile(autoExportDir, baseName, fileIndex, current.toString());
                fileIndex++;
                current.setLength(0);
                count = 0;
            }
        }
        if (current.length() > 0) {
            writeSplitFile(autoExportDir, baseName, fileIndex, current.toString());
        }
    }

    private void writeSplitFile(File autoExportDir, String baseName, int index, String content) throws Exception {
        File splitFile = new File(autoExportDir, baseName + "_" + index + ".txt");
        try (OutputStream os = new FileOutputStream(splitFile)) {
            os.write(content.getBytes());
        }
        outputArea.append("拆分文件" + index + ": " + splitFile.getAbsolutePath());
    }

    private List<String> readWord(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        try (InputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (var paragraph : doc.getParagraphs()) {
                String text = paragraph.getText().trim();
                if (!text.matches(".*[a-zA-Z].*")) {
                    continue;
                }
                lines.add(text);
            }
        }
        return lines;
    }

    // 读取Markdown文件，按行解析，过滤掉不含字母的行，与readWord逻辑保持一致
    private List<String> readMd(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String rawLine : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String text = rawLine.trim();
            if (text.isEmpty() || !text.matches(".*[a-zA-Z].*")) {
                continue;
            }
            lines.add(text);
        }
        return lines;
    }

    // 读取lrc字幕文件，去除时间轴标签后返回纯文本行，与readWord/readMd逻辑保持一致
    private List<String> readLrc(File file) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String rawLine : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String content = rawLine.replaceAll("\\[[^\\]]*\\]", "").trim();
            if (content.isEmpty()) {
                continue;
            }
            lines.add(content);
        }
        return lines;
    }

    // 将字幕纯文本行用换行符拼接写入_clean.txt，作为lrc提取的中间产物
    private void writeCleanSubtitle(File file, List<String> lines, File autoExportDir) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        String cleanFileName = file.getName().replaceAll("\\.[^.]+$", "_clean.txt");
        File cleanFile = new File(autoExportDir, cleanFileName);
        try (OutputStream os = new FileOutputStream(cleanFile)) {
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        outputArea.append("提取成功: " + cleanFile.getAbsolutePath());
    }
}