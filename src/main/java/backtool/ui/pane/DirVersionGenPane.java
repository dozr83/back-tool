package backtool.ui.pane;

import backtool.service.FilteredFileWalker;
import backtool.ui.component.FxKit;
import backtool.util.Md5Util;
import backtool.util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirVersionGenPane extends FxKit.FGridPane {

    private FxKit.FileInputFx fileInputFx = new FxKit.FileInputFx(FxKit.inputLength);
    private FxKit.DirInputFx dirInputFx = new FxKit.DirInputFx(FxKit.inputLength);
    private final FxKit.LogTextArea outputArea = new FxKit.LogTextArea("执行结果");

    public DirVersionGenPane() {
        outputArea.append("执行结果");
        this.addRow(fileInputFx);
        this.addRow(dirInputFx);
        this.addMergeRow(new FxKit.FButton("计算MD5资产清单", event -> calMd5()), 2);
        this.addMergeRow(outputArea, 2);
    }

    private void calMd5() {
        outputArea.clear();
        String fileStr = fileInputFx.getSelectFile();
        String dirStr = dirInputFx.getSelectDir();
        if (StringUtil.isBlank(fileStr) && StringUtil.isBlank(dirStr)) {
            outputArea.append("未选择");
            return;
        }
        //构造结果
        var resultBuilder = new StringBuilder();

        //文件
        if (StringUtil.isNotBlank(fileStr)) {
            resultBuilder.append(toLine(new File(fileStr))).append(StringUtil.LF);
        }
        //文件夹
        if (StringUtil.isNotBlank(dirStr)) {
            List<String> list = new ArrayList<>();
            FilteredFileWalker.walk(new File(dirStr),
                    file -> list.add(toLine(file)));
            resultBuilder.append("//执行清单:").append(list.size()).append(StringUtil.LF);
            //文件版本信息
            list.forEach(line -> {
                //生成md5
                resultBuilder.append(line).append(StringUtil.LF);
            });
        }
        outputArea.append(resultBuilder.toString());
    }

    /** 行格式：文件名,md5,长度（与原 FileDigestModel.of(file) 取值一致） */
    private static String toLine(File file) {
        return file.getName() + "," + Md5Util.getMD5(file) + "," + file.length();
    }
}
