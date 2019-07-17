package com.sequenceiq.cloudbreak.cloud.azure.util;

public class CustomVMImageNameProvider {
    private static final int NAME_MAXIMUM_LENGTH = 80;

    private static final char DELIMITER = '-';

    private CustomVMImageNameProvider() {
    }

    public static String get(String region, String vhdName) {
        String name = vhdName + DELIMITER + region.toLowerCase().replaceAll("\\s", "");
        if (name.length() > NAME_MAXIMUM_LENGTH) {
            int diff = name.length() - NAME_MAXIMUM_LENGTH;
            int calculatedEndIndexOfVhdName = vhdName.length() - diff;
            name = vhdName.substring(0, calculatedEndIndexOfVhdName) + '-' + region.toLowerCase().replaceAll("\\s", "");
        }
        return name;
    }
}
