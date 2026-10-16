package backtool.util;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Set;

public class TestFileAndDirFilter {

    private File testTempDir;

    @Before
    public void setUp() throws Exception {
        testTempDir = new File(System.getProperty("java.io.tmpdir"), "backtool-filefilter-test-" + System.currentTimeMillis());
        URL testDataUrl = getClass().getResource("/test-data/filefilter");
        FileUtils.copyDirectory(new File(testDataUrl.toURI()), testTempDir);
    }

    @After
    public void tearDown() throws Exception {
        if (testTempDir != null && testTempDir.exists()) {
            FileUtils.deleteDirectory(testTempDir);
        }
    }

    @Test
    public void testFileAndDirPredicate() {
        FileUtil.FileAndDirPredicate fileAndDirPredicate = new FileUtil.FileAndDirPredicate(
                Set.of(".idea", "target", ".DS_Store", ".git"),
                Set.of("iml", "md5")
        );
        Assert.assertTrue(fileAndDirPredicate.test(new File(testTempDir, ".idea")));
        Assert.assertTrue(fileAndDirPredicate.test(new File(testTempDir, ".idea/")));
        Assert.assertTrue(fileAndDirPredicate.test(new File(testTempDir, ".ideaA")));
        Assert.assertFalse(fileAndDirPredicate.test(new File(testTempDir, ".idea/a.txt")));
    }
}