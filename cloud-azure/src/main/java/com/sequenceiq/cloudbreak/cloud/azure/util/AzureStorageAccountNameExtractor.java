package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AzureStorageAccountNameExtractor {

    private static Pattern locationPattern = Pattern.compile("[^@]*@([^.]*)[.]dfs[.]core[.]windows[.]net");

    public String extractStorageAccountNameIfNecessary(String storageLocation) {
        String result = storageLocation;
        if (result != null) {
            Matcher m = locationPattern.matcher(storageLocation);
            if (m.matches()) {
                result = m.group(1);
            }
        }
        return result;
    }
}
