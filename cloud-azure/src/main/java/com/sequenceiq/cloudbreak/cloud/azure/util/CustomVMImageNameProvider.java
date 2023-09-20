package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomVMImageNameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomVMImageNameProvider.class);

    private static final int NAME_MAXIMUM_LENGTH = 80;

    private static final char DELIMITER = '-';

    public String getImageNameWithRegion(String region, String vhdUri) {
        String vhdName = getImageNameFromConnectionString(vhdUri);
        String name = vhdName + DELIMITER + region.toLowerCase(Locale.ROOT)
                .replaceAll("\\s", "");
        if (name.length() > NAME_MAXIMUM_LENGTH) {
            int diff = name.length() - NAME_MAXIMUM_LENGTH;
            int calculatedEndIndexOfVhdName = vhdName.length() - diff;
            name = vhdName.substring(0, calculatedEndIndexOfVhdName) + '-' + region.toLowerCase(Locale.ROOT)
                    .replaceAll("\\s", "");
        }

        LOGGER.debug("The following azure Azure image name obtained from the region [{}] and VHD URI [{}]: {}", region, vhdUri, name);
        return name;
    }

    public String getImageNameFromConnectionString(String vhdUri) {
        int begin = vhdUri.lastIndexOf('/') + 1;
        int end = vhdUri.contains("?") ? vhdUri.indexOf('?') : vhdUri.length();
        return vhdUri.substring(begin, end);
    }

}
