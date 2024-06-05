package com.sequenceiq.cloudbreak.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

public class CidrUtil {

    private CidrUtil() {

    }

    public static String[] cidrs(String cidr) {
        return Strings.isNullOrEmpty(cidr) ? new String[]{} : cidr.split(",");
    }

    public static Set<String> cidrSet(String cidr) {
        return Arrays.asList(Strings.isNullOrEmpty(cidr) ? new String[]{} : cidr.split(",")).stream().collect(Collectors.toSet());
    }

    public static List<String> cidrList(String cidr) {
        return Arrays.asList(Strings.isNullOrEmpty(cidr) ? new String[]{} : cidr.split(",")).stream().toList();
    }
}
