package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsClient.MAX_CLIENT_RETRIES;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsCachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.cache.AwsStsAssumeRoleCredentialsProviderCacheConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialAssumeRoleTypePersister;

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

    @Value("${cb.aws.session.credentials.duration:3600}")
    private int sessionCredentialsDuration;

    @Value("${cb.aws.external.id:}")
    private String deprecatedExternalId;

    @Value("${cb.aws.role.session.name:}")
    private String roleSessionName;

    @Value("${cb.aws.delegatorrole.session.name:}")
    private String delegatorRoleSessionName;

    @Value("${cb.aws.delegatorrole.arn:}")
    private String delegatorRoleArn;

    @Value("${aws.use.fips.endpoint:false}")
    private boolean fipsEnabled;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private AwsApacheClient awsApacheClient;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Optional<CredentialAssumeRoleTypePersister> credentialAssumeRoleTypePersister;

    @Cacheable(value = AwsCachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE, unless = "#awsCredential.getId() == null")
    public AwsSessionCredentials retrieveCachedSessionCredentials(AwsCredentialView awsCredential) {
        return retrieveSessionCredentials(awsCredential);
    }

    public AwsSessionCredentials retrieveSessionCredentials(AwsCredentialView awsCredential) {
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(sessionCredentialsDuration)
                .externalId(externalIdWithFallback(awsCredential))
                .roleArn(awsCredential.getRoleArn())
                .roleSessionName(roleSessionName)
                .build();
        LOGGER.debug("Trying to assume role with role arn {}", awsCredential.getRoleArn());
        return getAwsSessionCredentialsAndAssumeRole(awsCredential, assumeRoleRequest, true);
    }

    public AwsSessionCredentials retrieveSessionCredentialsWithoutExternalIdForValidationOnly(AwsCredentialView awsCredential) {
        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .durationSeconds(sessionCredentialsDuration)
                .roleArn(awsCredential.getRoleArn())
                .roleSessionName(roleSessionName)
                .build();
        LOGGER.debug("Trying to assume role with role arn {} and without external ID", awsCredential.getRoleArn());
        return getAwsSessionCredentialsAndAssumeRole(awsCredential, assumeRoleRequest, false);
    }

    @Cacheable(value = AwsStsAssumeRoleCredentialsProviderCacheConfig.TEMPORARY_AWS_STS_ASSUMEROLE_CREDENTIALS_PROVIDER_CACHE,
            unless = "#awsCredential.getId() == null")
    public StsAssumeRoleCredentialsProvider createStsAssumeRoleCredentialsProvider(AwsCredentialView awsCredential) {
        AssumeRoleRequest refreshRequest = AssumeRoleRequest.builder()
                .durationSeconds(sessionCredentialsDuration)
                .externalId(externalIdWithFallback(awsCredential))
                .roleArn(awsCredential.getRoleArn())
                .roleSessionName(roleSessionName)
                .build();
        return switch (determineAssumeRoleStrategy(awsCredential)) {
            case ACCESS_KEY -> {
                LOGGER.info("Using DEFAULT_CREDENTIAL_CHAIN assume type for provider, role [{}]", awsCredential.getRoleArn());
                updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, true);
                yield getDefaultRoleCredentialProvider(awsCredential, refreshRequest);
            }
            case STORED_DELEGATOR -> {
                LOGGER.info("Using stored DELEGATOR assume type for provider, role [{}]", awsCredential.getRoleArn());
                StsClient stsClient = buildDelegatorStsClient(awsCredential);
                StsAssumeRoleCredentialsProvider delegatorProvider = getDelegatorCredentialProvider(stsClient, refreshRequest);
                try {
                    doAssumeRole(stsClient, refreshRequest, awsCredential.getRoleArn());
                    delegatorProvider.resolveCredentials();
                    yield delegatorProvider;
                } catch (SdkException e) {
                    LOGGER.info("Stored DELEGATOR role cannot assume [{}], retrying with SA role", awsCredential.getRoleArn(), e);
                    delegatorProvider.close();
                    StsAssumeRoleCredentialsProvider saProvider = getDefaultRoleCredentialProvider(awsCredential, refreshRequest);
                    saProvider.resolveCredentials();
                    LOGGER.info("SA role can assume [{}], reverting assume type to DEFAULT_CREDENTIAL_CHAIN", awsCredential.getRoleArn());
                    updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, true);
                    yield saProvider;
                }
            }
            case SA_WITH_FALLBACK -> {
                boolean delegatorAvailable = isDelegatorConfiguredAndEntitled(awsCredential);
                StsAssumeRoleCredentialsProvider saProvider = getDefaultRoleCredentialProvider(awsCredential, refreshRequest);
                if (!delegatorAvailable) {
                    updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, true);
                    yield saProvider;
                }
                try {
                    saProvider.resolveCredentials();
                    LOGGER.info("SA role can assume [{}] directly, delegator not needed", awsCredential.getRoleArn());
                    updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, true);
                    yield saProvider;
                } catch (SdkException e) {
                    LOGGER.warn("SA role cannot assume [{}], falling back to delegator role [{}]",
                            awsCredential.getRoleArn(), delegatorRoleArn, e);
                    saProvider.close();
                    StsAssumeRoleCredentialsProvider delegatorCredentialProvider =
                            getDelegatorCredentialProvider(buildDelegatorStsClient(awsCredential), refreshRequest);
                    updateAssumeRoleType(awsCredential, AssumeRoleType.DELEGATOR, true);
                    yield delegatorCredentialProvider;
                }
            }
        };
    }

    private StsAssumeRoleCredentialsProvider getDefaultRoleCredentialProvider(AwsCredentialView awsCredential, AssumeRoleRequest refreshRequest) {
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(awsSecurityTokenServiceClient(awsCredential))
                .refreshRequest(refreshRequest)
                .build();
    }

    private StsAssumeRoleCredentialsProvider getDelegatorCredentialProvider(StsClient stsClient, AssumeRoleRequest refreshRequest) {
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(refreshRequest)
                .build();
    }

    private AwsSessionCredentials getAwsSessionCredentialsAndAssumeRole(AwsCredentialView awsCredential,
            AssumeRoleRequest assumeRoleRequest, boolean persistEnabled) {
        return switch (determineAssumeRoleStrategy(awsCredential)) {
            case ACCESS_KEY -> {
                LOGGER.info("Using DEFAULT_CREDENTIAL_CHAIN assume type for role [{}]", awsCredential.getRoleArn());
                AwsSessionCredentials result = doAssumeRole(
                        awsSecurityTokenServiceClient(awsCredential), assumeRoleRequest, awsCredential.getRoleArn());
                updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, persistEnabled);
                yield result;
            }
            case STORED_DELEGATOR -> {
                LOGGER.info("Using stored DELEGATOR assume type for role [{}]", awsCredential.getRoleArn());
                try {
                    yield doAssumeRole(
                            buildDelegatorStsClient(awsCredential),
                            assumeRoleRequest,
                            awsCredential.getRoleArn()
                    );
                } catch (SdkException e) {
                    LOGGER.warn("Stored DELEGATOR role cannot assume [{}], retrying with SA role", awsCredential.getRoleArn(), e);
                    AwsSessionCredentials result = doAssumeRole(
                            awsSecurityTokenServiceClient(awsCredential),
                            assumeRoleRequest,
                            awsCredential.getRoleArn()
                    );
                    LOGGER.info("SA role assumed [{}], reverting assume type to DEFAULT_CREDENTIAL_CHAIN", awsCredential.getRoleArn());
                    updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, persistEnabled);
                    yield result;
                }
            }
            case SA_WITH_FALLBACK -> {
                try {
                    AwsSessionCredentials result = doAssumeRole(
                            awsSecurityTokenServiceClient(awsCredential),
                            assumeRoleRequest,
                            awsCredential.getRoleArn()
                    );
                    LOGGER.info("SA role assumed [{}] successfully", awsCredential.getRoleArn());
                    updateAssumeRoleType(awsCredential, AssumeRoleType.DEFAULT_CREDENTIAL_CHAIN, persistEnabled);
                    yield result;
                } catch (SdkException e) {
                    if (isDelegatorConfiguredAndEntitled(awsCredential)) {
                        LOGGER.warn("SA role cannot assume [{}], falling back to delegator role [{}]",
                                awsCredential.getRoleArn(), delegatorRoleArn, e);
                        AwsSessionCredentials result = doAssumeRole(
                                buildDelegatorStsClient(awsCredential),
                                assumeRoleRequest,
                                awsCredential.getRoleArn()
                        );
                        LOGGER.info("Delegator assumed [{}] successfully", awsCredential.getRoleArn());
                        updateAssumeRoleType(awsCredential, AssumeRoleType.DELEGATOR, persistEnabled);
                        yield result;
                    }
                    LOGGER.error("Unable to assume role. Check exception for details.", e);
                    throw e;
                }
            }
        };
    }

    private AwsSessionCredentials doAssumeRole(StsClient stsClient, AssumeRoleRequest assumeRoleRequest, String roleArn) {
        AssumeRoleResponse result = stsClient.assumeRole(assumeRoleRequest);
        Credentials credentialsResponse = result.credentials();

        String formattedExpirationDate = "";
        Date expirationTime = null;
        Instant expiration = credentialsResponse.expiration();
        if (expiration != null) {
            expirationTime = Date.from(expiration);
            formattedExpirationDate = new StdDateFormat().format(expirationTime);
        }
        LOGGER.debug("Assume role result credential: role arn: {}, expiration date: {}",
                roleArn, formattedExpirationDate);

        return new AwsSessionCredentials(
                credentialsResponse.accessKeyId(),
                credentialsResponse.secretAccessKey(),
                credentialsResponse.sessionToken(),
                expirationTime);
    }

    StsClient buildDelegatorStsClient(AwsCredentialView awsCredential) {
        String defaultZone = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        StsClient delegatorStsClient = baseStsClientBuilder(defaultZone, awsCredential)
                .credentialsProvider(getCredential(awsCredential))
                .build();
        return baseStsClientBuilder(defaultZone, awsCredential)
                .credentialsProvider(
                        StsAssumeRoleCredentialsProvider.builder()
                                .stsClient(delegatorStsClient)
                                .refreshRequest(AssumeRoleRequest.builder()
                                        .roleArn(delegatorRoleArn)
                                        .roleSessionName(delegatorRoleSessionName)
                                        .externalId(awsCredential.getExternalId())
                                        .build())
                                .build())
                .build();
    }

    public StsClient awsSecurityTokenServiceClient(AwsCredentialView awsCredential) {
        String defaultZone = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        return baseStsClientBuilder(defaultZone, awsCredential)
                .credentialsProvider(getCredential(awsCredential))
                .build();
    }

    private StsClientBuilder baseStsClientBuilder(String defaultZone, AwsCredentialView awsCredential) {
        StsClientBuilder builder = StsClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .region(Region.of(defaultZone))
                .overrideConfiguration(getDefaultClientConfiguration());
        if (!fipsEnabled || !awsCredential.isGovernmentCloudEnabled()) {
            URI endpointConfiguration = getEndpointConfiguration(defaultZone);
            LOGGER.info("Configuring STS endpoint override to: '{}'", endpointConfiguration);
            builder.endpointOverride(endpointConfiguration);
        }
        return builder;
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

    private boolean isDelegatorConfigured() {
        return StringUtils.isNotEmpty(delegatorRoleArn);
    }

    private String externalIdWithFallback(AwsCredentialView awsCredential) {
        String externalId = awsCredential.getExternalId();
        return StringUtils.isEmpty(externalId) ? deprecatedExternalId : externalId;
    }

    private AssumeRoleStrategy determineAssumeRoleStrategy(AwsCredentialView awsCredential) {
        if (shouldUseAccessKeyDirectly(awsCredential)) {
            return AssumeRoleStrategy.ACCESS_KEY;
        }
        if (shouldUseDelegatorDirectly(awsCredential)) {
            return AssumeRoleStrategy.STORED_DELEGATOR;
        }
        return AssumeRoleStrategy.SA_WITH_FALLBACK;
    }

    private boolean shouldUseAccessKeyDirectly(AwsCredentialView awsCredential) {
        return isLocalDev(awsCredential);
    }

    private boolean shouldUseDelegatorDirectly(AwsCredentialView awsCredential) {
        return AssumeRoleType.DELEGATOR == awsCredential.getRoleAssumeType()
                && isDelegatorConfiguredAndEntitled(awsCredential);
    }

    private boolean isDelegatorConfiguredAndEntitled(AwsCredentialView awsCredential) {
        return isDelegatorConfigured()
                && entitlementService.isAwsDelegatorRoleBasedCredentialEnabled(awsCredential.getAccountId());
    }

    private void updateAssumeRoleType(AwsCredentialView awsCredential, AssumeRoleType type, boolean persistEnabled) {
        AssumeRoleType previousType = awsCredential.getRoleAssumeType();
        boolean typeChanged = type != previousType;
        awsCredential.setRoleAssumeType(type);
        if (persistEnabled && typeChanged) {
            LOGGER.info("Assume role type changed from {} to {}", previousType, type);
            credentialAssumeRoleTypePersister.ifPresent(persister -> persister.persistAssumeRoleType(
                    awsCredential.getCredentialCrn(), awsCredential.getAccountId(), type.name()));
        }
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

    private enum AssumeRoleStrategy {
        ACCESS_KEY,
        STORED_DELEGATOR,
        SA_WITH_FALLBACK
    }

}
