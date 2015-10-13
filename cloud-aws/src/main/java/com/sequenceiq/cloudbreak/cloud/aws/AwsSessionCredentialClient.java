package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_AWS_EXTERNAL_ID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.sequenceiq.cloudbreak.cloud.aws.cache.AwsCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@Component
public class AwsSessionCredentialClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSessionCredentialClient.class);
    private static final int DEFAULT_SESSION_CREDENTIALS_DURATION = 3600;

    @Value("${cb.aws.external.id:" + CB_AWS_EXTERNAL_ID + "}")
    private String externalId;

    @Cacheable(AwsCachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE)
    public BasicSessionCredentials retrieveCachedSessionCredentials(AwsCredentialView awsCredential) {
        LOGGER.debug("retrieving session credential");
        AWSSecurityTokenServiceClient client = new AWSSecurityTokenServiceClient();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .withExternalId(externalId)
                .withRoleArn(awsCredential.getRoleArn())
                .withRoleSessionName("hadoop-provisioning");
        AssumeRoleResult result = client.assumeRole(assumeRoleRequest);

        return new BasicSessionCredentials(
                result.getCredentials().getAccessKeyId(),
                result.getCredentials().getSecretAccessKey(),
                result.getCredentials().getSessionToken());
    }

}
