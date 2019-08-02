package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class WasbConfigGenerator extends CloudStorageConfigGenerator<WasbConfig> {

    private static final String[] WASB_SCHEME_PREFIXES = {"wasb://", "wasbs://"};

    private static final String AZURE_BLOB_DOMAIN_SUFFIX = ".blob.core.windows.net";

    @Override
    public WasbConfig generateStorageConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, WASB_SCHEME_PREFIXES);
            String[] splitted = locationWithoutScheme.split("@");
            String[] storageWithSuffix = splitted[0].split("/", 2);
            String folderPrefix = storageWithSuffix.length < 2 ? "" :  "/" + storageWithSuffix[1];
            if (splitted.length < 2) {
                return new WasbConfig(folderPrefix, storageWithSuffix[0], null);
            } else {
                String[] splittedByDomain = splitted[1].split(AZURE_BLOB_DOMAIN_SUFFIX);
                String account = splittedByDomain[0];
                if (splittedByDomain.length > 1) {
                    String folderPrefixAfterDomain = splittedByDomain[1];
                    if (StringUtils.isNoneEmpty(folderPrefix, folderPrefixAfterDomain)) {
                        throw new CloudbreakServiceException(String.format("Invalid WASB path: %s", location));
                    }
                    folderPrefix = StringUtils.isNotEmpty(folderPrefixAfterDomain) ? folderPrefixAfterDomain : folderPrefix;
                }
                return new WasbConfig(folderPrefix, storageWithSuffix[0], account);
            }
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for WASB");
    }
}
