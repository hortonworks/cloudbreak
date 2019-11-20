package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

public class AwsSessionCredentialProvider implements AWSCredentialsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSessionCredentialProvider.class);

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

    @Override
    public AWSCredentials getCredentials() {
        AwsSessionCredentials sessionCredentials = awsSessionCredentialClient.retrieveCachedSessionCredentials(awsCredentialView);
        sessionCredentials = checkExpirationTime(sessionCredentials);
        return sessionCredentials;
    }

    private AwsSessionCredentials checkExpirationTime(AwsSessionCredentials sessionCredentials) {
        final Date expirationTime = sessionCredentials.getExpiration();
        if (expirationTimeCheck && expirationTime != null) {
            DateTime expirationDateTime = new DateTime(expirationTime);
            DateTime nowDateTime = new DateTime(new Date());
            if (expirationDateTime.isAfter(nowDateTime.toDate().getTime()) ||
                    new Duration(expirationDateTime, nowDateTime).getStandardMinutes() < forceTokenRefreshTimeInMin) {
                LOGGER.debug("Force retrieving session credentials because of expiration time is too close.");
                sessionCredentials = awsSessionCredentialClient.retrieveSessionCredentials(awsCredentialView);
            }
        }
        return sessionCredentials;
    }

    @Override
    public void refresh() {
    }
}
