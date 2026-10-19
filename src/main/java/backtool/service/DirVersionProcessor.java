package backtool.service;

import backtool.util.LinkedHashMapUtil;
import backtool.util.Md5Util;

import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dozr83 on 2021/9/6
 */
public class DirVersionProcessor {
    private static final Logger logger = Logger.getLogger(DirVersionProcessor.class.getName());

    public static record FileDigestModel(String name, String md5, String length) {
        @Override
        public String toString() {
            return new StringBuilder().append(name)
                    .append(",").append(md5)
                    .append(",").append(length).toString();
        }

        public static FileDigestModel of(File file) {
            return new FileDigestModel(file.getName(), Md5Util.getMD5(file), "" + file.length());
        }

        public static FileDigestModel of(String str) {
            String[] strs = str.trim().split(",");
            return new FileDigestModel(strs[0], strs[1], strs[2]);
        }
    }

    public static record VersionCheckResult(List<FileDigestModel> okFiles, List<FileDigestModel> deleteFiles) {

    }



    public static VersionCheckResult checkDirChangeByMap(String checkDirStr, LinkedHashMap<String, String> historyMap) throws IOException {

        // 构建版本校验需要的反向 Map (key: md5, value: list of paths)
        // 1. 历史版本
        LinkedHashMap<String, List<String>> historyRevertMap = new LinkedHashMap<>();
        historyMap.forEach((name, value) -> {
            String md5 = value.split(",")[0];
            historyRevertMap.computeIfAbsent(md5, k -> new ArrayList<>()).add(name);
        });

        // 2. 当前版本
        Map<String, String> currentMd5Map = DigestCalculator.getDirDigestRelativePathMap(checkDirStr, DigestType.MD5);
        LinkedHashMap<String, List<String>> currentRevertMap = LinkedHashMapUtil.convert2ValueMap(currentMd5Map);

        //按值比对：历史版本md5在当前版本中仍存在为OK，不存在为已删除
        List<String> okPaths = new ArrayList<>();
        List<String> missingPaths = new ArrayList<>();
        historyRevertMap.forEach((md5, paths) -> {
            if (currentRevertMap.containsKey(md5)) {
                okPaths.addAll(paths);
            } else {
                missingPaths.addAll(paths);
            }
        });

        // 转换为旧的结果类型以保持兼容
        List<FileDigestModel> okFiles = new ArrayList<>();
        List<FileDigestModel> deleteFiles = new ArrayList<>();

        // 从 historyMap 中查找文件的完整信息
        Map<String, FileDigestModel> historyModelMap = new LinkedHashMap<>();
        historyMap.forEach((name, value) -> {
            String[] parts = value.split(",");
            String md5 = parts[0];
            String length = parts.length > 1 ? parts[1] : "";
            historyModelMap.put(name, new FileDigestModel(name, md5, length));
        });

        okPaths.forEach(path -> {
            FileDigestModel model = historyModelMap.get(path);
            if (model != null) okFiles.add(model);
        });
        missingPaths.forEach(path -> {
            FileDigestModel model = historyModelMap.get(path);
            if (model != null) deleteFiles.add(model);
        });

        return new VersionCheckResult(okFiles, deleteFiles);

    }

}
