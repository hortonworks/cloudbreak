package com.sequenceiq.cloudbreak.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3ExpressBucketNameValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ExpressBucketNameValidator.class);

    @Value("${aws.s3express-name-pattern:--x-s3}")
    private String s3expressBucketSuffixPattern;

    public boolean isS3ExpressBucket(String bucketName) {
        LOGGER.info("Validating if s3 bucket name ends with {} for bucket: {}", s3expressBucketSuffixPattern, bucketName);
        return bucketName.endsWith(s3expressBucketSuffixPattern);
    }
}
