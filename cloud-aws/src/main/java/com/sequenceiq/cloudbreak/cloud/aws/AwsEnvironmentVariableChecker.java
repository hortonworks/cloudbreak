package com.sequenceiq.cloudbreak.cloud.aws;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.springframework.stereotype.Component;

@Component
public class AwsEnvironmentVariableChecker {

    public boolean isAwsSecretAccessKeyAvailable() {
        return !isEmpty(System.getenv("AWS_SECRET_ACCESS_KEY"));
    }

    public boolean isAwsAccessKeyAvailable() {
        return !isEmpty(System.getenv("AWS_ACCESS_KEY_ID"));
    }

}
