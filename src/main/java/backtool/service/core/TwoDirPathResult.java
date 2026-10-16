package backtool.service.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TwoDirPathResult {

    public List<File> sameFiles = new ArrayList<>();
    public List<File> leftUniqFiles = new ArrayList<>();
    public List<File> rightUniqFiles = new ArrayList<>();
    public List<CrossDelta> modifiedFiles = new ArrayList<>();

    public static record CrossDelta(File left, File right) {
    }
}