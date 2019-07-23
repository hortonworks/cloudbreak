package com.sequenceiq.cloudbreak.fluent.cloud;

public abstract class CloudStorageConfigGenerator<T extends CloudStorageConfig> {

    public abstract T generateStorageConfig(String location);

    String getLocationWithoutSchemePrefixes(String input, String... schemePrefixes) {
        for (String schemePrefix : schemePrefixes) {
            if (input.startsWith(schemePrefix)) {
                String[] splitted = input.split(schemePrefix);
                if (splitted.length > 1) {
                    return splitted[1];
                }
            }
        }
        return input;
    }
}
