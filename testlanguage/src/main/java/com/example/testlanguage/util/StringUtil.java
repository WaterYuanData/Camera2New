package com.example.testlanguage.util;

public class StringUtil {
    public static int string2int (String str) {
        return string2int(str,0);
    }
    public static int string2int (String str,int def) {
        try {
            return Integer.valueOf(str);
        } catch (Exception e) {
        }
        return def;
    }
}
