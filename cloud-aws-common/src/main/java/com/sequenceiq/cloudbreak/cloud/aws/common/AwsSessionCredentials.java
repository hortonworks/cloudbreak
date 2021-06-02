package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Date;

import com.amazonaws.auth.BasicSessionCredentials;

public class AwsSessionCredentials extends BasicSessionCredentials {

    private final Date expiration;

    public AwsSessionCredentials(String awsAccessKey, String awsSecretKey, String sessionToken, Date expiration) {
        super(awsAccessKey, awsSecretKey, sessionToken);
        this.expiration = expiration;
    }

    public Date getExpiration() {
        return expiration;
    }
}
