package com.sequenceiq.freeipa.service.freeipa.backup.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class AdlsGen2BackupConfigGenerator extends CloudBackupConfigGenerator<AdlsGen2BackupConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdlsGen2BackupConfigGenerator.class);

    private static final String[] ADLS_GEN2_SCHEME_PREFIXES = {"abfs://", "abfss://"};

    private static final String AZURE_DFS_DOMAIN_SUFFIX = ".dfs.core.windows.net";

    private static final String AZURE_BLOB_STORAGE_SUFFIX = "blob.core.windows.net";

    private static final String AZURE_BLOB_STORAGE_SCHEMA = "https://";

    @Override
    public String generateBackupLocation(String location, String clusterType,
            String clusterName, String clusterId) {
        AdlsGen2BackupConfig adlsGen2BackupConfig = generateBackupConfig(location);
        String logFolder = resolveBackupFolder(adlsGen2BackupConfig, clusterType, clusterName, clusterId);
        String hostPart = String.format("%s.%s", adlsGen2BackupConfig.getAccount(), AZURE_BLOB_STORAGE_SUFFIX);
        String generatedLocation = String.format("%s%s", AZURE_BLOB_STORAGE_SCHEMA,
                Paths.get(hostPart, adlsGen2BackupConfig.getFileSystem(), logFolder));
        LOGGER.info("The following ADLS Gen2 base folder location is generated: {} (from {})",
                generatedLocation, location);
        return generatedLocation;
    }

    private AdlsGen2BackupConfig generateBackupConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, ADLS_GEN2_SCHEME_PREFIXES);
            String[] locationSplit = locationWithoutScheme.split("@");
            String[] storageWithSuffix = locationSplit[0].split("/", 2);
            String folderPrefix = storageWithSuffix.length < 2 ? "" :  "/" + storageWithSuffix[1];
            if (locationSplit.length < 2) {
                return new AdlsGen2BackupConfig(folderPrefix, storageWithSuffix[0], null);
            } else {
                String[] splitByDomain = locationSplit[1].split(AZURE_DFS_DOMAIN_SUFFIX);
                String account = splitByDomain[0];
                if (splitByDomain.length > 1) {
                    String folderPrefixAfterDomain = splitByDomain[1];
                    if (StringUtils.isNoneEmpty(folderPrefix, folderPrefixAfterDomain)) {
                        throw new CloudbreakServiceException(String.format("Invalid ADLS Gen2 path: %s", location));
                    }
                    folderPrefix = StringUtils.isNotEmpty(folderPrefixAfterDomain) ? folderPrefixAfterDomain : folderPrefix;
                }
                return new AdlsGen2BackupConfig(folderPrefix, storageWithSuffix[0], account);
            }
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for ADLS Gen2");
    }
}
