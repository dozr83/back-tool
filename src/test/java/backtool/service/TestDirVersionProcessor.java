package backtool.service;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

public class TestDirVersionProcessor {

    private File testTempDir;

    @Before
    public void setUp() throws Exception {
        testTempDir = new File(System.getProperty("java.io.tmpdir"), "backtool-version-test-" + System.currentTimeMillis());
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
    public void testDirVersionProcessor() {
    }
}