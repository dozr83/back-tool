package backtool.service.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TwoDirSameResult {
    public List<CrossIdentity> sames = new ArrayList<>();

    public void addSame(List<File> lefts, List<File> rights) {
        sames.add(new CrossIdentity(lefts, rights));
    }

    public record CrossIdentity(List<File> lefts, List<File> rights) {
        @Override
        public String toString() {
            return "CrossIdentity{" +
                    "lefts=" + lefts +
                    ", rights=" + rights +
                    '}';
        }
    }
}
