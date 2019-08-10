package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class WasbConfigGenerator extends CloudStorageConfigGenerator<WasbConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WasbConfigGenerator.class);

    private static final String[] WASB_SCHEME_PREFIXES = {"wasb://", "wasbs://"};

    private static final String AZURE_BLOB_DOMAIN_SUFFIX = ".blob.core.windows.net";

    @Override
    public String generateStoredLocation(String location, String clusterType,
            String clusterName, String clusterId) {
        WasbConfig wasbConfig = generateStorageConfig(location);
        String scheme = wasbConfig.isSecure() ? WASB_SCHEME_PREFIXES[1] : WASB_SCHEME_PREFIXES[0];
        String logFolder = resolveLogFolder(wasbConfig, clusterType, clusterName, clusterId);
        String generatedS3Location = String.format("%s%s@%s", scheme, wasbConfig.getStorageContainer(),
                Paths.get(String.format("%s%s", wasbConfig.getAccount(), AZURE_BLOB_DOMAIN_SUFFIX), logFolder));
        LOGGER.debug("The following WASB base folder location is generated: {} (from {})",
                generatedS3Location, location);
        return generatedS3Location;
    }

    @Override
    public WasbConfig generateStorageConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            boolean secure = location.startsWith(WASB_SCHEME_PREFIXES[1]);
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, WASB_SCHEME_PREFIXES);
            String[] splitted = locationWithoutScheme.split("@");
            String[] storageWithSuffix = splitted[0].split("/", 2);
            String folderPrefix = storageWithSuffix.length < 2 ? "" :  "/" + storageWithSuffix[1];
            if (splitted.length < 2) {
                return new WasbConfig(folderPrefix, storageWithSuffix[0], null, secure);
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
                return new WasbConfig(folderPrefix, storageWithSuffix[0], account, secure);
            }
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for WASB");
    }
}
