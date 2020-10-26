package com.sequenceiq.cloudbreak.cloud.azure.util;

import org.springframework.stereotype.Component;

@Component
public class CustomVMImageNameProvider {
    private static final int NAME_MAXIMUM_LENGTH = 80;

    private static final char DELIMITER = '-';

    public String getImageNameWithRegion(String region, String vhdUri) {
        String vhdName = getImageNameFromConnectionString(vhdUri);
        String name = vhdName + DELIMITER + region.toLowerCase().replaceAll("\\s", "");
        if (name.length() > NAME_MAXIMUM_LENGTH) {
            int diff = name.length() - NAME_MAXIMUM_LENGTH;
            int calculatedEndIndexOfVhdName = vhdName.length() - diff;
            name = vhdName.substring(0, calculatedEndIndexOfVhdName) + '-' + region.toLowerCase().replaceAll("\\s", "");
        }
        return name;
    }

    public String getImageNameFromConnectionString(String vhdUri) {
        int begin = vhdUri.lastIndexOf('/') + 1;
        int end = vhdUri.contains("?") ? vhdUri.indexOf('?') : vhdUri.length();
        return vhdUri.substring(begin, end);
    }

}
