package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class AwsSessionCredentialProvider implements AwsCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSessionCredentialProvider.class);

    private static final int MINUTES_TO_MILLIS = 60 * 1000;

    private static final int FORCE_TOKEN_REFRESH_TIME_IN_MIN_DEFAULT = 5;

    private final AwsCredentialView awsCredentialView;

    private final AwsSessionCredentialClient awsSessionCredentialClient;

    private final boolean expirationTimeCheck;

    private final Integer forceTokenRefreshTimeInMin;

    public AwsSessionCredentialProvider(AwsCredentialView awsCredentialView,
            AwsSessionCredentialClient awsSessionCredentialClient) {
        this(awsCredentialView, awsSessionCredentialClient, false, FORCE_TOKEN_REFRESH_TIME_IN_MIN_DEFAULT);
    }

    public AwsSessionCredentialProvider(AwsCredentialView awsCredentialView,
            AwsSessionCredentialClient awsSessionCredentialClient,
            boolean expirationTimeCheck, Integer forceTokenRefreshTimeInMin) {
        this.awsCredentialView = awsCredentialView;
        this.awsSessionCredentialClient = awsSessionCredentialClient;
        this.expirationTimeCheck = expirationTimeCheck;
        this.forceTokenRefreshTimeInMin = forceTokenRefreshTimeInMin;
    }

    private AwsSessionCredentials checkExpirationTime(AwsSessionCredentials sessionCredentials) {
        Date expirationTime = sessionCredentials.getExpiration();
        if (expirationTimeCheck && expirationTime != null) {
            Date now = new Date();
            if (expirationTime.after(now) ||
                    expirationTime.getTime() - now.getTime() < forceTokenRefreshTimeInMin * MINUTES_TO_MILLIS) {
                LOGGER.debug("Force retrieving session credentials because of expiration time is too close.");
                sessionCredentials = awsSessionCredentialClient.retrieveSessionCredentials(awsCredentialView);
            }
        }
        return sessionCredentials;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        AwsSessionCredentials sessionCredentials = awsSessionCredentialClient.retrieveCachedSessionCredentials(awsCredentialView);
        sessionCredentials = checkExpirationTime(sessionCredentials);
        return sessionCredentials;
    }
}
