package com.sequenceiq.it.util;

public class HostNameUtil {

    private HostNameUtil() {

    }

    public static String generateHostNameByIp(String address) {
        return "host-" + address.replace(".", "-") + ".example.com";
    }
}
