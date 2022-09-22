package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Date;

import software.amazon.awssdk.auth.credentials.AwsCredentials;

public class AwsSessionCredentials implements AwsCredentials {

    private final software.amazon.awssdk.auth.credentials.AwsSessionCredentials awsSessionCredentials;

    private final Date expiration;

    public AwsSessionCredentials(String awsAccessKey, String awsSecretKey, String sessionToken, Date expiration) {
        this.awsSessionCredentials = software.amazon.awssdk.auth.credentials.AwsSessionCredentials.create(awsAccessKey, awsSecretKey, sessionToken);
        this.expiration = expiration;
    }

    @Override
    public String accessKeyId() {
        return awsSessionCredentials.accessKeyId();
    }

    @Override
    public String secretAccessKey() {
        return awsSessionCredentials.secretAccessKey();
    }

    public Date getExpiration() {
        return expiration;
    }
}
