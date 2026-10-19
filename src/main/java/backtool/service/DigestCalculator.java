package backtool.service;

import backtool.util.FileUtil;
import backtool.util.LinkedHashMapUtil;
import backtool.util.Md5Util;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 摘要计算门面：DigestType 类型目录 + 统一入口（遍历引擎见 DigestWalker，比对见 DirDigestComparer，清单 IO 见 DigestManifest）
 */
public class DigestCalculator {

    public static LinkedHashMap<String, List<File>> groupFilesByMd5(List<File> files) {
        LinkedHashMap<String, List<File>> md5RevertMap_md5 = new LinkedHashMap<>();
        files.forEach(file -> {
            String md5 = Md5Util.getMD5(file);
            List<File> md5Files = md5RevertMap_md5.computeIfAbsent(md5, key -> new ArrayList<>());
            md5Files.add(file);
        });
        return md5RevertMap_md5;
    }

    public static LinkedHashMap<String, String> getDirDigestRelativePathMap(String baseDirStr, DigestType digestType) {
        LinkedHashMap<File, String> digestFileMap = getDirDigestFileMap(baseDirStr, digestType);
        return LinkedHashMapUtil.alterKey(digestFileMap,
                file -> FileUtil.getSubPathUnix(file.getAbsolutePath(), new File(baseDirStr).getAbsolutePath()));
    }

    public static LinkedHashMap<File, String> getDirDigestFileMap(String baseDirStr, DigestType digestType) {
        return DigestWalker.walkAndCalculate(new File(baseDirStr),
                FilterRules.DEFAULT.dirPredicate(), FilterRules.DEFAULT.filePredicate(),
                digestType::calculate);
    }
}
