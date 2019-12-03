package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class AdlsGen2ConfigGenerator extends CloudStorageConfigGenerator<AdlsGen2Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdlsGen2ConfigGenerator.class);

    private static final String[] ADLS_GEN2_SCHEME_PREFIXES = {"abfs://", "abfss://"};

    private static final String AZURE_DFS_DOMAIN_SUFFIX = ".dfs.core.windows.net";

    @Override
    public String generateStoredLocation(String location, String clusterType,
            String clusterName, String clusterId) {
        AdlsGen2Config adlsGen2Config = generateStorageConfig(location);
        String scheme = adlsGen2Config.isSecure() ? ADLS_GEN2_SCHEME_PREFIXES[1] : ADLS_GEN2_SCHEME_PREFIXES[0];
        String logFolder = resolveLogFolder(adlsGen2Config, clusterType, clusterName, clusterId);
        String generatedLocation = String.format("%s%s@%s", scheme, adlsGen2Config.getFileSystem(),
                Paths.get(String.format("%s%s", adlsGen2Config.getAccount(), AZURE_DFS_DOMAIN_SUFFIX), logFolder));
        LOGGER.info("The following ADLS Gen2 base folder location is generated: {} (from {})",
                generatedLocation, location);
        return generatedLocation;
    }

    @Override
    public AdlsGen2Config generateStorageConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            boolean secure = location.startsWith(ADLS_GEN2_SCHEME_PREFIXES[1]);
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, ADLS_GEN2_SCHEME_PREFIXES);
            String[] split = locationWithoutScheme.split("@");
            String[] storageWithSuffix = split[0].split("/", 2);
            String folderPrefix = storageWithSuffix.length < 2 ? "" :  "/" + storageWithSuffix[1];
            if (split.length < 2) {
                return new AdlsGen2Config(folderPrefix, storageWithSuffix[0], null, secure);
            } else {
                String[] splitByDomain = split[1].split(AZURE_DFS_DOMAIN_SUFFIX);
                String account = splitByDomain[0];
                if (splitByDomain.length > 1) {
                    String folderPrefixAfterDomain = splitByDomain[1];
                    if (StringUtils.isNoneEmpty(folderPrefix, folderPrefixAfterDomain)) {
                        throw new CloudbreakServiceException(String.format("Invalid ADLS Gen2 path: %s", location));
                    }
                    folderPrefix = StringUtils.isNotEmpty(folderPrefixAfterDomain) ? folderPrefixAfterDomain : folderPrefix;
                }
                return new AdlsGen2Config(folderPrefix, storageWithSuffix[0], account, secure);
            }
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for ADLS Gen2");
    }
}
