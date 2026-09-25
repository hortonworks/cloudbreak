package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.compute.models.HyperVGenerationTypes;

@Component
public class CustomVMImageNameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomVMImageNameProvider.class);

    private static final int NAME_MAXIMUM_LENGTH = 80;

    private static final char DELIMITER = '-';

    private static final String GEN2_SUFFIX = "-g2";

    public String getImageNameWithRegion(String region, String vhdUri, HyperVGenerationTypes hyperVGeneration) {
        String vhdName = getImageNameFromConnectionString(vhdUri);
        String suffix = DELIMITER + region.toLowerCase(Locale.ROOT).replaceAll("\\s", "") + generationSuffix(hyperVGeneration);
        String name = vhdName + suffix;
        if (name.length() > NAME_MAXIMUM_LENGTH) {
            int diff = name.length() - NAME_MAXIMUM_LENGTH;
            name = vhdName.substring(0, vhdName.length() - diff) + suffix;
        }

        LOGGER.debug("The following Azure image name obtained from the region [{}], VHD URI [{}] and Hyper-V generation [{}]: {}",
                region, vhdUri, hyperVGeneration, name);
        return name;
    }

    private String generationSuffix(HyperVGenerationTypes hyperVGeneration) {
        return HyperVGenerationTypes.V2.equals(hyperVGeneration) ? GEN2_SUFFIX : "";
    }

    public String getImageNameFromConnectionString(String vhdUri) {
        int begin = vhdUri.lastIndexOf('/') + 1;
        int end = vhdUri.contains("?") ? vhdUri.indexOf('?') : vhdUri.length();
        return vhdUri.substring(begin, end);
    }

}