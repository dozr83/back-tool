package backtool.util;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

public class TestFileUtil {

    private File testTempDir;

    @Before
    public void setUp() throws Exception {
        testTempDir = new File(System.getProperty("java.io.tmpdir"), "backtool-fileutil-test-" + System.currentTimeMillis());
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
    public void testFileSub() {
        String pathStr = "/../a.txt";
        Assert.assertEquals(pathStr, Path.of(pathStr).toString());
        Assert.assertEquals(pathStr, Path.of(pathStr).toAbsolutePath().toString());
        Assert.assertEquals("/a.txt", Path.of(pathStr).normalize().toString());
        Assert.assertEquals(pathStr, (new File(pathStr).getAbsolutePath().toString()));
    }

    @Test
    public void getSubPathUnix() {
        if (OsUtil.isMac()) {
            var key = FileUtil.getSubPathUnix("/export/资料/文件/a.txt", "/export/资料/");
            Assert.assertEquals("文件/a.txt", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件/a.txt", "/export/资料");
            Assert.assertEquals("文件/a.txt", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件", "/export/资料/文件");
            Assert.assertEquals("", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件", "/export/资料/文件/");
            Assert.assertEquals("", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件/", "/export/资料/文件/");
            Assert.assertEquals("", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件/a", "/export/资料/文件/");
            Assert.assertEquals("a", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件/a", "/export/资料/文件");
            Assert.assertEquals("a", key);
            key = FileUtil.getSubPathUnix("/export/资料/文件/a/", "/export/资料/文件/");
            Assert.assertEquals("a", key);
        }
        if (OsUtil.isWindows()) {
            var key = FileUtil.getSubPathUnix("D:\\资料\\文件\\a.txt", "D:\\资料");
            Assert.assertEquals("文件/a.txt", key);
            key = FileUtil.getSubPathUnix("D:\\文件\\a.txt", "D:\\");
            Assert.assertEquals("文件/a.txt", key);
            key = FileUtil.getSubPathUnix("D:\\资料\\文件", "D:\\资料\\文件");
            Assert.assertEquals("", key);
            key = FileUtil.getSubPathUnix("D:\\资料\\文件", "D:\\资料\\文件\\");
            Assert.assertEquals("", key);
            key = FileUtil.getSubPathUnix("D:\\资料\\文件\\", "D:\\资料\\文件\\");
            Assert.assertEquals("", key);
            key = FileUtil.getSubPathUnix("D:\\资料\\文件\\a", "D:\\资料\\文件\\");
            Assert.assertEquals("a", key);
            key = FileUtil.getSubPathUnix("D:\\资料\\文件\\a", "D:\\资料\\文件");
            Assert.assertEquals("a", key);
            key = FileUtil.getSubPathUnix("D:\\资料\\文件\\a\\", "D:\\资料\\文件\\");
            Assert.assertEquals("a", key);
        }
    }

    @Test
    public void deleteOneRepeatFile() throws Exception {
        File fileA = new File(testTempDir, "readme.md");
        File fileACopy = new File(testTempDir, "readmeCopy.md");
        Assert.assertTrue(fileA.exists());

        FileUtil.deleteRepeatFile(fileA, fileA);
        Assert.assertTrue(fileA.exists());
        FileUtil.deleteRepeatFile(fileA, new File(fileA.getAbsolutePath()));
        Assert.assertTrue(fileA.exists());

        File fileB = new File(testTempDir, "b.txt");
        FileUtil.deleteRepeatFile(fileA, fileB);
        Assert.assertTrue(fileA.exists());

        Assert.assertFalse(fileACopy.exists());
        FileUtils.copyFile(fileA, fileACopy);
        Assert.assertTrue(fileACopy.exists());
        FileUtil.deleteRepeatFile(fileA, fileACopy);
        Assert.assertFalse(fileA.exists());
    }

    @Test
    public void testFileName() {
        File file = new File("/testFly/abc.txt");
        Assert.assertEquals("txt", FileUtil.getSuffix(file));
        Assert.assertEquals("abc", FileUtil.getNameWithoutSuffix(file));
    }
}