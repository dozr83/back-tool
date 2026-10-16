package backtool.service.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OneDirResult {
    public List<Duplicate> duplicates = new ArrayList<>();

    public void addDuplicate(List<File> files) {
        duplicates.add(new Duplicate(files));
    }

    public static record Duplicate(List<File> sames) {
    }
}