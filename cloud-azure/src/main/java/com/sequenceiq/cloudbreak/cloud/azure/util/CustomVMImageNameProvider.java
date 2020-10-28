package com.sequenceiq.cloudbreak.cloud.azure.util;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;

@Component
public class CustomVMImageNameProvider {
    private static final int NAME_MAXIMUM_LENGTH = 80;

    private static final char DELIMITER = '-';

    @Inject
    private AzureUtils azureUtils;

    public String get(String region, String vhdUri) {
        String vhdName = azureUtils.getImageNameFromConnectionString(vhdUri);
        String name = vhdName + DELIMITER + region.toLowerCase().replaceAll("\\s", "");
        if (name.length() > NAME_MAXIMUM_LENGTH) {
            int diff = name.length() - NAME_MAXIMUM_LENGTH;
            int calculatedEndIndexOfVhdName = vhdName.length() - diff;
            name = vhdName.substring(0, calculatedEndIndexOfVhdName) + '-' + region.toLowerCase().replaceAll("\\s", "");
        }
        return name;
    }
}
