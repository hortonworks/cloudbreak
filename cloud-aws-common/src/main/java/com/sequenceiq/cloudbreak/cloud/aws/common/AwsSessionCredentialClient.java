package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_CLIENT_RETRIES;

import java.net.URI;
import java.time.Instant;
import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsStsAssumeRoleCredentialsProviderCacheConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

@Component
public class AwsSessionCredentialClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSessionCredentialClient.class);

    private static final int DEFAULT_SESSION_CREDENTIALS_DURATION = 3600;

    @Value("${cb.aws.external.id:}")
    private String deprecatedExternalId;

    @Value("${cb.aws.role.session.name:}")
    private String roleSessionName;

    @Value("${aws.use.fips.endpoint:false}")
    private boolean fipsEnabled;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private AwsApacheClient awsApacheClient;

    @Cacheable(value = AwsCachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE, unless = "#awsCredential.getId() == null")
    public AwsSessionCredentials retrieveCachedSessionCredentials(AwsCredentialView awsCredential) {
        return retrieveSessionCredentials(awsCredential);
    }

    public AwsSessionCredentials retrieveSessionCredentials(AwsCredentialView awsCredential) {
        String externalId = awsCredential.getExternalId();
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .externalId(StringUtils.isEmpty(externalId) ? deprecatedExternalId : externalId)
                .roleArn(awsCredential.getRoleArn())
                .roleSessionName(roleSessionName)
                .build();
        LOGGER.debug("Trying to assume role with role arn {}", awsCredential.getRoleArn());
        return getAwsSessionCredentialsAndAssumeRole(awsCredential, assumeRoleRequest);
    }

    public AwsSessionCredentials retrieveSessionCredentialsWithoutExternalId(AwsCredentialView awsCredential) {
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                .roleArn(awsCredential.getRoleArn())
                .roleSessionName(roleSessionName)
                .build();
        LOGGER.debug("Trying to assume role with role arn {} and without external ID", awsCredential.getRoleArn());
        return getAwsSessionCredentialsAndAssumeRole(awsCredential, assumeRoleRequest);
    }

    @Cacheable(value = AwsStsAssumeRoleCredentialsProviderCacheConfig.TEMPORARY_AWS_STS_ASSUMEROLE_CREDENTIALS_PROVIDER_CACHE,
            unless = "#awsCredential.getId() == null")
    public StsAssumeRoleCredentialsProvider createStsAssumeRoleCredentialsProvider(AwsCredentialView awsCredential) {
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(awsSecurityTokenServiceClient(awsCredential))
                .refreshRequest(AssumeRoleRequest.builder()
                        .durationSeconds(DEFAULT_SESSION_CREDENTIALS_DURATION)
                        .externalId(awsCredential.getExternalId())
                        .roleArn(awsCredential.getRoleArn())
                        .roleSessionName(roleSessionName)
                        .build())
                .build();
    }

    private AwsSessionCredentials getAwsSessionCredentialsAndAssumeRole(AwsCredentialView awsCredential, AssumeRoleRequest assumeRoleRequest) {
        try {
            AssumeRoleResponse result = awsSecurityTokenServiceClient(awsCredential).assumeRole(assumeRoleRequest);
            Credentials credentialsResponse = result.credentials();

            String formattedExpirationDate = "";
            Date expirationTime = null;
            Instant expiration = credentialsResponse.expiration();
            if (expiration != null) {
                expirationTime = Date.from(expiration);
                formattedExpirationDate = new StdDateFormat().format(expirationTime);
            }
            LOGGER.debug("Assume role result credential: role arn: {}, expiration date: {}",
                    awsCredential.getRoleArn(), formattedExpirationDate);

            return new AwsSessionCredentials(
                    credentialsResponse.accessKeyId(),
                    credentialsResponse.secretAccessKey(),
                    credentialsResponse.sessionToken(),
                    expirationTime);
        } catch (SdkException e) {
            LOGGER.error("Unable to assume role. Check exception for details.", e);
            throw e;
        }
    }

    public StsClient awsSecurityTokenServiceClient(AwsCredentialView awsCredential) {
        String defaultZone = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        StsClientBuilder stsClientBuilder = StsClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .region(Region.of(defaultZone))
                .credentialsProvider(getCredential(awsCredential))
                .overrideConfiguration(getDefaultClientConfiguration());
        if (!fipsEnabled || !awsCredential.isGovernmentCloudEnabled()) {
            URI endpointConfiguration = getEndpointConfiguration(defaultZone);
            LOGGER.info("Configuring STS endpoint override to: '{}'", endpointConfiguration);
            stsClientBuilder.endpointOverride(endpointConfiguration);
        }
        return stsClientBuilder.build();
    }

    private AwsCredentialsProvider getCredential(AwsCredentialView awsCredential) {
        if (isLocalDev(awsCredential)) {
            AwsCredentials awsCredentials = AwsBasicCredentials.create(
                    awsEnvironmentVariableChecker.getAwsAccessKey(awsCredential),
                    awsEnvironmentVariableChecker.getAwsSecretAccessKey(awsCredential));
            return StaticCredentialsProvider.create(awsCredentials);
        } else {
            return DefaultCredentialsProvider.create();
        }
    }

    private boolean isLocalDev(AwsCredentialView awsCredential) {
        return awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredential)
                && awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredential);
    }

    private ClientOverrideConfiguration getDefaultClientConfiguration() {
        return ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(MAX_CLIENT_RETRIES)
                        .build())
                .build();
    }

    URI getEndpointConfiguration(String defaultZone) {
        return URI.create(String.format("https://sts.%s.amazonaws.com", defaultZone));
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
