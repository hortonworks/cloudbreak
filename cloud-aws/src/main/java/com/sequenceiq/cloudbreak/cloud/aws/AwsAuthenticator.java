package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class AwsAuthenticator implements Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsAuthenticator.class);

    @Inject
    private AwsClient awsClient;

    @Override
    public String platform() {
        return AwsConstants.AWS;
    }

    @Override
    public String variant() {
        return AwsConstants.AWS;
    }

    @Override
    public AuthenticatedContext authenticate(CloudContext cloudContext, CloudCredential cloudCredential) {
        LOGGER.info("Authenticating to aws ...");
        return awsClient.createAuthenticatedContext(cloudContext, cloudCredential);
    }
}
