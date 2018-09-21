package com.sequenceiq.cloudbreak.cloud.aws;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.sequenceiq.cloudbreak.cloud.aws.cache.AwsCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;

@Component
public class AwsSessionCredentialClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSessionCredentialClient.class);

    private static final int DEFAULT_SESSION_CREDENTIALS_DURATION = 3600;

    @Value("${cb.aws.external.id:}")
    private String deprecatedExternalId;

    @Value("${cb.aws.role.session.name:}")
    private String roleSessionName;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Cacheable(value = AwsCachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE, unless = "#awsCredential.getId() == null")
    public BasicSessionCredentials retrieveCachedSessionCredentials(AwsCredentialView awsCredential) {
        return retrieveSessionCredentials(awsCredential);
    }

    public BasicSessionCredentials retrieveSessionCredentials(AwsCredentialView awsCredential) {
        LOGGER.debug("retrieving session credential");

        String externalId = awsCredential.getExternalId();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .withExternalId(StringUtils.isEmpty(externalId) ? deprecatedExternalId : externalId)
                .withRoleArn(awsCredential.getRoleArn())
                .withRoleSessionName(roleSessionName);
        AssumeRoleResult result = awsSecurityTokenServiceClient(awsCredential).assumeRole(assumeRoleRequest);
        return new BasicSessionCredentials(
                result.getCredentials().getAccessKeyId(),
                result.getCredentials().getSecretAccessKey(),
                result.getCredentials().getSessionToken());
    }

    private AWSSecurityTokenService awsSecurityTokenServiceClient(AwsCredentialView awsCredential) {
        if (!awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredential)
                || !awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredential)) {
            LOGGER.info("AWSSecurityTokenServiceClient will use aws metadata because environment variables are undefined");
            return AWSSecurityTokenServiceClientBuilder.standard()
                    .withRegion(awsDefaultZoneProvider.getDefaultZone(awsCredential))
                    .withCredentials(new InstanceProfileCredentialsProvider())
                    .build();
        } else {
            LOGGER.info("AWSSecurityTokenServiceClient will use environment variables");
            return AWSSecurityTokenServiceClientBuilder.standard()
                    .withRegion(awsDefaultZoneProvider.getDefaultZone(awsCredential))
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();
        }
    }

}
