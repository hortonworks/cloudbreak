package com.sequenceiq.cloudbreak.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HostUtil {

    private HostUtil() {

    }

    public static boolean hasPort(String url) {
        Pattern compile = Pattern.compile(".*\\d{4,5}/?(.*)/?");
        Matcher matcher = compile.matcher(url);
        return matcher.find();
    }
}
