package com.sequenceiq.cloudbreak.telemetry.fluent.cloud;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class S3ConfigGenerator extends CloudStorageConfigGenerator<S3Config> {

    private static final String[] S3_SCHEME_PREFIXES = {"s3://", "s3a://", "s3n://"};

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
