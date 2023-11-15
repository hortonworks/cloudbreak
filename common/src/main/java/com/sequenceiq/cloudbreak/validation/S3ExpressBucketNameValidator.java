package com.sequenceiq.cloudbreak.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class S3ExpressBucketNameValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ExpressBucketNameValidator.class);

    private static final String S3_EXPRESS_BUCKET_NAME_PATTERN = "--x-s3";

    public static boolean isS3ExpressBucket(String bucketName) {
        LOGGER.info("Validating if s3 bucket name ends with --x-s3 for bucket: {}", bucketName);
        return bucketName.endsWith(S3_EXPRESS_BUCKET_NAME_PATTERN);
    }
}
