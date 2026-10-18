package backtool.service.core;

import backtool.service.CompareQuery;
import backtool.service.DiffResult;
import backtool.service.DigestCalculator;
import backtool.util.FileUtil;
import backtool.util.LinkedHashMapUtil;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DirCompareUtil {


    //仅处理【单个目录】，查找目录内部的重复文件
    public static OneDirResult compareOneDir(String leftDirStr) {
        OneDirResult result = new OneDirResult();

        /**第一步，使用Length初筛**/
        //取得文件长度的Map
        LinkedHashMap<File, String> fileLengthMapAll = DigestCalculator.getDirDigestFileMap(leftDirStr, DigestCalculator.DigestType.LEN);
        //转换Map：Key:文件长度，value：长度相等的文件列表
        LinkedHashMap<String, List<File>> lengthGroupFileMap_len = LinkedHashMapUtil.convert2ValueMap(fileLengthMapAll);
        //从ValueMap中查找重复文件
        LinkedHashMap<String, List<File>> duplicateFileMapByLen_len = LinkedHashMapUtil.filterLinkedHashMap(lengthGroupFileMap_len, e -> e.getValue().size() > 1);
        //简化结构，统一放入，不用按长度分堆 ，以前算法保持分堆，后续的二次检验在分堆中查找
        List<File> doubleFilesByLen = duplicateFileMapByLen_len.entrySet().stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toList());


        /**第二步，二次MD5检验**/
        //长度相等的一堆数据，生成新的反相Map
        LinkedHashMap<String, List<File>> md5RevertMap_md5 = DigestCalculator.groupFilesByMd5(doubleFilesByLen);
        //删选MD5相等的
        LinkedHashMapUtil.filterLinkedHashMap(md5RevertMap_md5, e -> e.getValue().size() > 1)
                .forEach((md5, files) -> result.addDuplicate(files));
        return result;
    }

    //    仅处理【两个目录】，不看路径，只查找两边内容相同的文件
    public static TwoDirSameResult compareTwoByContent(String leftDirStr, String rightDirStr) {
        TwoDirSameResult result = new TwoDirSameResult();
        LinkedHashMap<File, String> leftMd5Map_file = DigestCalculator.getDirDigestFileMap(leftDirStr, DigestCalculator.DigestType.MD5);
        LinkedHashMap<File, String> rightMd5Map_file = DigestCalculator.getDirDigestFileMap(rightDirStr, DigestCalculator.DigestType.MD5);
        LinkedHashMap<String, List<File>> leftGroupFileMap_md5 = LinkedHashMapUtil.convert2ValueMap(leftMd5Map_file);
        LinkedHashMap<String, List<File>> rightGroupFileMap_md5 = LinkedHashMapUtil.convert2ValueMap(rightMd5Map_file);
        leftGroupFileMap_md5.forEach((md5, leftFiles) -> {
            if (rightGroupFileMap_md5.containsKey(md5)) {
                result.addSame(leftFiles, rightGroupFileMap_md5.get(md5));
            }
        });
        return result;
    }


    /**
     * 【两个目录完整全量对比】，看路径，覆盖所有对比场景（相同 + 左边独有 + 右边独有 + 修改）
     *
     * @param leftDirStr             左边文件夹
     * @param rightDirStr            右边文件夹
     * @return 对比结果
     */
    public static TwoDirPathResult compareTwoByPath(String leftDirStr, String rightDirStr) {
        TwoDirPathResult compareResult = new TwoDirPathResult();

        //获取左右文件夹的文件MD5映射
        LinkedHashMap<File, String> leftMd5Map_file = DigestCalculator.getDirDigestFileMap(leftDirStr, DigestCalculator.DigestType.MD5);
        LinkedHashMap<File, String> rightMd5Map_file = DigestCalculator.getDirDigestFileMap(rightDirStr, DigestCalculator.DigestType.MD5);

        //构建路径到文件和MD5的映射，以相对路径为主键
        LinkedHashMap<String, File> leftFileMap_path = new LinkedHashMap<>();
        LinkedHashMap<String, String> leftMd5Map_path = new LinkedHashMap<>();
        leftMd5Map_file.forEach((file, md5) -> {
            String path = FileUtil.getSubPathUnix(file.getAbsolutePath(), leftDirStr);
            leftFileMap_path.put(path, file);
            leftMd5Map_path.put(path, md5);
        });

        LinkedHashMap<String, File> rightFileMap_path = new LinkedHashMap<>();
        LinkedHashMap<String, String> rightMd5Map_path = new LinkedHashMap<>();
        rightMd5Map_file.forEach((file, md5) -> {
            String path = FileUtil.getSubPathUnix(file.getAbsolutePath(), rightDirStr);
            rightFileMap_path.put(path, file);
            rightMd5Map_path.put(path, md5);
        });

        // 使用 CompareQuery 进行比对
        DiffResult diffResult = CompareQuery.of()
                .left(leftMd5Map_path)
                .right(rightMd5Map_path)
                .byPath();

        // 组装结果
        diffResult.getSamePaths().forEach(path -> compareResult.sameFiles.add(leftFileMap_path.get(path)));
        diffResult.getLeftOnlyPaths().forEach(path -> compareResult.leftUniqFiles.add(leftFileMap_path.get(path)));
        diffResult.getRightOnlyPaths().forEach(path -> compareResult.rightUniqFiles.add(rightFileMap_path.get(path)));
        diffResult.getModifiedPaths().forEach(path -> compareResult.modifiedFiles.add(
                new TwoDirPathResult.CrossDelta(leftFileMap_path.get(path), rightFileMap_path.get(path))));

        return compareResult;
    }

}
