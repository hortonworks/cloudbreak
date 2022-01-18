package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_CLIENT_RETRIES;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING;

import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.tracing.AwsTracingRequestHandler;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import io.opentracing.Tracer;

@Component
public class AwsSessionCredentialClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSessionCredentialClient.class);

    private static final int DEFAULT_SESSION_CREDENTIALS_DURATION = 3600;

    @Value("${cb.aws.external.id:}")
    private String deprecatedExternalId;

    @Value("${cb.aws.role.session.name:}")
    private String roleSessionName;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private Tracer tracer;

    @Cacheable(value = AwsCachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE, unless = "#awsCredential.getId() == null")
    public AwsSessionCredentials retrieveCachedSessionCredentials(AwsCredentialView awsCredential) {
        return retrieveSessionCredentials(awsCredential);
    }

    public AwsSessionCredentials retrieveSessionCredentials(AwsCredentialView awsCredential) {
        String externalId = awsCredential.getExternalId();
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .withExternalId(StringUtils.isEmpty(externalId) ? deprecatedExternalId : externalId)
                .withRoleArn(awsCredential.getRoleArn())
                .withRoleSessionName(roleSessionName);
        LOGGER.debug("Trying to assume role with role arn {}", awsCredential.getRoleArn());
        return getAwsSessionCredentialsAndAssumeRole(awsCredential, assumeRoleRequest);
    }

    public AwsSessionCredentials retrieveSessionCredentialsWithoutExternalId(AwsCredentialView awsCredential) {
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest()
                .withDurationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .withRoleArn(awsCredential.getRoleArn())
                .withRoleSessionName(roleSessionName);
        LOGGER.debug("Trying to assume role with role arn {} and without external ID", awsCredential.getRoleArn());
        return getAwsSessionCredentialsAndAssumeRole(awsCredential, assumeRoleRequest);
    }

    private AwsSessionCredentials getAwsSessionCredentialsAndAssumeRole(AwsCredentialView awsCredential, AssumeRoleRequest assumeRoleRequest) {
        try {
            AssumeRoleResult result = awsSecurityTokenServiceClient(awsCredential).assumeRole(assumeRoleRequest);
            Credentials credentialsResponse = result.getCredentials();

            String formattedExpirationDate = "";
            Date expirationTime = credentialsResponse.getExpiration();
            if (expirationTime != null) {
                formattedExpirationDate = new StdDateFormat().format(expirationTime);
            }
            LOGGER.debug("Assume role result credential: role arn: {}, expiration date: {}",
                    awsCredential.getRoleArn(), formattedExpirationDate);

            return new AwsSessionCredentials(
                    credentialsResponse.getAccessKeyId(),
                    credentialsResponse.getSecretAccessKey(),
                    credentialsResponse.getSessionToken(),
                    credentialsResponse.getExpiration());
        } catch (SdkClientException e) {
            LOGGER.error("Unable to assume role. Check exception for details.", e);
            throw e;
        }
    }

    private AWSSecurityTokenService awsSecurityTokenServiceClient(AwsCredentialView awsCredential) {
        String defaultZone = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        return AWSSecurityTokenServiceClientBuilder.standard()
                .withEndpointConfiguration(getEndpointConfiguration(defaultZone))
                .withClientConfiguration(getDefaultClientConfiguration())
                .withCredentials(getCredential(awsCredential))
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .build();
    }

    private AWSCredentialsProvider getCredential(AwsCredentialView awsCredential) {
        if (isLocalDev(awsCredential)) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(
                    awsEnvironmentVariableChecker.getAwsAccessKey(awsCredential),
                    awsEnvironmentVariableChecker.getAwsSecretAccessKey(awsCredential));
            return new AWSStaticCredentialsProvider(awsCredentials);
        } else {
            return DefaultAWSCredentialsProviderChain.getInstance();
        }
    }

    private boolean isLocalDev(AwsCredentialView awsCredential) {
        return awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredential)
                && awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredential);
    }

    private ClientConfiguration getDefaultClientConfiguration() {
        return new ClientConfiguration()
                .withThrottledRetries(true)
                .withMaxConsecutiveRetriesBeforeThrottling(MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING)
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(MAX_CLIENT_RETRIES));
    }

    private AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(String defaultZone) {
        return new AwsClientBuilder
                .EndpointConfiguration(String.format("https://sts.%s.amazonaws.com", defaultZone), defaultZone);
    }

    @Override
    public String toString() {
        return "AwsSessionCredentialClient{" +
                "deprecatedExternalId='" + deprecatedExternalId + '\'' +
                ", roleSessionName='" + roleSessionName + '\'' +
                ", awsDefaultZoneProvider=" + awsDefaultZoneProvider.toString() +
                '}';
    }

}
