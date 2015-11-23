package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class AwsConnector implements CloudConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConnector.class);

    @Inject
    private AwsClient awsClient;
    @Inject
    private AwsResourceConnector awsResourceConnector;
    @Inject
    private AwsInstanceConnector awsInstanceConnector;
    @Inject
    private AwsMetadataCollector awsMetadataCollector;
    @Inject
    private AwsCredentialConnector awsCredentialConnector;
    @Inject
    private AwsPlatformParameters awsPlatformParameters;
    @Inject
    private AwsSetup awsSetup;
    @Inject
    private AwsAuthenticator awsAuthenticator;

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }

    @Override
    public Authenticator authentication() {
        return awsAuthenticator;
    }

    @Override
    public ResourceConnector resources() {
        return awsResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return awsInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return awsMetadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return awsPlatformParameters;
    }

    @Override
    public Setup setup() {
        return awsSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return awsCredentialConnector;
    }

}
