package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class S3ConfigGenerator extends CloudStorageConfigGenerator<S3Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ConfigGenerator.class);

    private static final String[] S3_SCHEME_PREFIXES = {"s3://", "s3a://", "s3n://"};

    @Override
    public String generateStoredLocation(String location, String clusterType, String clusterName, String clusterId) {
        S3Config s3Config = generateStorageConfig(location);
        String generatedS3Location = S3_SCHEME_PREFIXES[0] + Paths.get(s3Config.getBucket(),
                resolveLogFolder(s3Config, clusterType, clusterName, clusterId));
        LOGGER.debug("The following S3 base folder location is generated: {} (from {})",
                generatedS3Location, location);
        return generatedS3Location;
    }

    @Override
    public S3Config generateStorageConfig(String location) {
        if (StringUtils.isNotEmpty(location)) {
            String locationWithoutScheme = getLocationWithoutSchemePrefixes(location, S3_SCHEME_PREFIXES);
            String[] splitted = locationWithoutScheme.split("/", 2);
            String folderPrefix = splitted.length < 2 ? "" :  splitted[1];
            return new S3Config(folderPrefix, splitted[0]);
        }
        throw new CloudbreakServiceException("Storage location parameter is missing for S3");
    }
}
