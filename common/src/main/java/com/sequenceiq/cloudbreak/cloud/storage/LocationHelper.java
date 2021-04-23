package com.sequenceiq.cloudbreak.cloud.storage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class LocationHelper {

    private static final Pattern EXTRACT_BUCKET_PATTERN = Pattern.compile("^s3.?://([^/]+)");

    public String parseS3BucketName(String cloudStorageLocation) {
        String result = "";
        Matcher m = EXTRACT_BUCKET_PATTERN.matcher(cloudStorageLocation);
        if (m.find()) {
            result = m.group(1);
        }
        return result;
    }
}
