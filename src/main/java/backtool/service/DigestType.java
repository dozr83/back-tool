package backtool.service;

import backtool.util.Md5Util;

import java.io.File;

public enum DigestType {
    MD5 {
        @Override
        public String calculate(File file) {
            return Md5Util.getMD5(file);
        }
    }, LEN {
        @Override
        public String calculate(File file) {
            return String.valueOf(file.length());
        }
    }, LEN_MD5 {
        @Override
        public String calculate(File file) {
            return Md5Util.getMD5(file) + "," + file.length();
        }
    };

    public abstract String calculate(File file);
}
