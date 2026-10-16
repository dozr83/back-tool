package backtool.service.core;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class TestDirCompareUtil {

    private File testTempDir;

    @Before
    public void setUp() throws Exception {
        testTempDir = new File(System.getProperty("java.io.tmpdir"), "backtool-test-" + System.currentTimeMillis());
        URL testDataUrl = getClass().getResource("/test-data");
        FileUtils.copyDirectory(new File(testDataUrl.toURI()), testTempDir);
    }

    @After
    public void tearDown() throws Exception {
        if (testTempDir != null && testTempDir.exists()) {
            FileUtils.deleteDirectory(testTempDir);
        }
    }

    @Test
    public void testCompareOneDir() {
        File testDir = new File(testTempDir, "one-dir/compre");
        OneDirResult result = DirCompareUtil.compareOneDir(testDir.getAbsolutePath());

        Assert.assertEquals(1, result.duplicates.size());
        Assert.assertEquals(2, result.duplicates.get(0).sames().size());
        Assert.assertTrue(result.duplicates.get(0).sames().contains(new File(testDir, "file1.txt")));
        Assert.assertTrue(result.duplicates.get(0).sames().contains(new File(testDir, "sub/file1.txt")));
    }

    @Test
    public void testCompareTwoByContent() {
        File leftDir = new File(testTempDir, "two-dir/left");
        File rightDir = new File(testTempDir, "two-dir/right");
        TwoDirSameResult result = DirCompareUtil.compareTwoByContent(leftDir.getAbsolutePath(), rightDir.getAbsolutePath());

        Assert.assertEquals(1, result.sames.size());
        Assert.assertTrue(result.sames.get(0).lefts().contains(new File(leftDir, "same.txt")));
        Assert.assertTrue(result.sames.get(0).rights().contains(new File(rightDir, "same.txt")));
    }

    @Test
    public void testCompareTwoByPath() {
        File leftDir = new File(testTempDir, "two-dir/left");
        File rightDir = new File(testTempDir, "two-dir/right");
        TwoDirPathResult result = DirCompareUtil.compareTwoByPath(leftDir.getAbsolutePath(), rightDir.getAbsolutePath());

        Assert.assertEquals(1, result.sameFiles.size());
        Assert.assertTrue(result.sameFiles.contains(new File(leftDir, "same.txt")));

        Assert.assertEquals(1, result.modifiedFiles.size());
        Assert.assertEquals(new File(leftDir, "modified.txt"), result.modifiedFiles.get(0).left());
        Assert.assertEquals(new File(rightDir, "modified.txt"), result.modifiedFiles.get(0).right());

        Assert.assertEquals(2, result.leftUniqFiles.size());
        Assert.assertTrue(result.leftUniqFiles.contains(new File(leftDir, "left-only.txt")));
        Assert.assertTrue(result.leftUniqFiles.contains(new File(leftDir, "sub/left-only-sub.txt")));

        Assert.assertEquals(2, result.rightUniqFiles.size());
        Assert.assertTrue(result.rightUniqFiles.contains(new File(rightDir, "right-only.txt")));
        Assert.assertTrue(result.rightUniqFiles.contains(new File(rightDir, "sub/right-only-sub.txt")));
    }
}