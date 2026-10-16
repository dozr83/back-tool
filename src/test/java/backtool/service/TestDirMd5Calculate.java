package backtool.service;

import backtool.util.OsUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class TestDirMd5Calculate {

    private File testTempDir;

    @Before
    public void setUp() throws Exception {
        testTempDir = new File(System.getProperty("java.io.tmpdir"), "backtool-md5-test-" + System.currentTimeMillis());
        URL testDataUrl = getClass().getResource("/test-data/fileutil");
        FileUtils.copyDirectory(new File(testDataUrl.toURI()), testTempDir);
    }

    @After
    public void tearDown() throws Exception {
        if (testTempDir != null && testTempDir.exists()) {
            FileUtils.deleteDirectory(testTempDir);
        }
    }

    @Test
    public void genDirMd5VersionTagLen() {
        if (OsUtil.isWindows()) {
            return;
        }
        String md5 = """
                {Java/java语法.txt=25, Java/Spring框架.txt=33, readme.md=24, 李白/静夜思.txt=15}""";

        String md5FileStr =
                DigestCalculator.getDirDigestRelativePathMap(
                        DigestConfig.builder()
                                .baseDir(testTempDir.getAbsolutePath())
                                .digestType(DigestCalculator.DigestType.LEN)
                                .filterRules(FilterRules.NONE)
                                .build()
                ).toString();
        Assert.assertEquals(md5, md5FileStr);
    }

    @Test
    public void genDirMd5VersionTagMd5() {
        if (OsUtil.isWindows()) {
            return;
        }
        String md5 = """
                {Java/java语法.txt=f29a628831fe901bbdd3132bbdc2313e, Java/Spring框架.txt=5fb14d685c15f75c92d243da709cc28b, readme.md=a723b646fe33ab2f0c8e8ed4c4583f0b, 李白/静夜思.txt=7bae0c4e8ec3f7dededc3450137efa04}""";

        String md5FileStr =
                DigestCalculator.getDirDigestRelativePathMap(
                        DigestConfig.builder()
                                .baseDir(testTempDir.getAbsolutePath())
                                .digestType(DigestCalculator.DigestType.MD5)
                                .filterRules(FilterRules.NONE)
                                .build()
                ).toString();
        Assert.assertEquals(md5, md5FileStr);
    }
}