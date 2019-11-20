package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.retry.PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.retry.PredefinedBackoffStrategies.EqualJitterBackoffStrategy;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.service.Retry;

@Component
public class AwsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsClient.class);

    @Inject
    private AwsSessionCredentialClient credentialClient;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private AwsPlatformParameters awsPlatformParameters;

    @Inject
    private AwsCredentialVerifier awsCredentialVerifier;

    @Inject
    private Retry retry;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        try {
            authenticatedContext.putParameter(AmazonEC2Client.class, createAccess(authenticatedContext.getCloudCredential()));
        } catch (AmazonServiceException e) {
            throw new CredentialVerificationException(e.getErrorMessage(), e);
        }
        return authenticatedContext;
    }

    public AmazonEC2Client createAccess(CloudCredential credential) {
        return createAccess(new AwsCredentialView(credential), awsDefaultZoneProvider.getDefaultZone(credential));
    }

    public AmazonEC2Client createAccess(AwsCredentialView awsCredential, String regionName) {
        AmazonEC2Client client = isRoleAssumeRequired(awsCredential)
                ? createAwsClientWithCredentialProvider(awsCredential, AmazonEC2ClientBuilder.standard(), AmazonEC2Client.class)
                : new AmazonEC2Client(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AWSSecurityTokenService createAwsSecurityTokenService(AwsCredentialView awsCredential) {
        return isRoleAssumeRequired(awsCredential)
                ? createAwsClientWithCredentialProvider(awsCredential,
                AWSSecurityTokenServiceClientBuilder.standard(), AWSSecurityTokenServiceClient.class)
                : new AWSSecurityTokenServiceClient(createAwsCredentials(awsCredential));
    }

    public AmazonIdentityManagement createAmazonIdentityManagement(AwsCredentialView awsCredential) {
        return isRoleAssumeRequired(awsCredential)
                ? createAwsClientWithCredentialProvider(awsCredential,
                AmazonIdentityManagementClientBuilder.standard(), AmazonIdentityManagementClient.class)
                : new AmazonIdentityManagementClient(createAwsCredentials(awsCredential));
    }

    public AWSKMS createAWSKMS(AwsCredentialView awsCredential, String regionName) {
        return AWSKMSClientBuilder.standard()
                .withCredentials(getAwsStaticCredentialsProvider(awsCredential))
                .withRegion(regionName)
                .build();
    }

    public AWSStaticCredentialsProvider getAwsStaticCredentialsProvider(AwsCredentialView awsCredential) {
        return isRoleAssumeRequired(awsCredential)
                ? new AWSStaticCredentialsProvider(credentialClient.retrieveCachedSessionCredentials(awsCredential))
                : new AWSStaticCredentialsProvider(createAwsCredentials(awsCredential));
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudFormationClient client = isRoleAssumeRequired(awsCredential)
                ? createAwsClientWithCredentialProvider(awsCredential,
                AmazonCloudFormationClientBuilder.standard(), AmazonCloudFormationClient.class)
                : new AmazonCloudFormationClient(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AmazonCloudFormationRetryClient createCloudFormationRetryClient(AwsCredentialView awsCredential, String regionName) {
        return new AmazonCloudFormationRetryClient(createCloudFormationClient(awsCredential, regionName), retry);
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonAutoScalingClient client = isRoleAssumeRequired(awsCredential)
                ? createAwsClientWithCredentialProvider(awsCredential,
                AmazonAutoScalingClientBuilder.standard(), AmazonAutoScalingClient.class)
                : new AmazonAutoScalingClient(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AmazonAutoScalingRetryClient createAutoScalingRetryClient(AwsCredentialView awsCredential, String regionName) {
        return new AmazonAutoScalingRetryClient(createAutoScalingClient(awsCredential, regionName), retry);
    }

    public AmazonS3 createS3Client(AwsCredentialView awsCredential) {
        return AmazonS3ClientBuilder.standard()
                .withCredentials(getAwsStaticCredentialsProvider(awsCredential))
                .withRegion(awsDefaultZoneProvider.getDefaultZone(awsCredential))
                .build();
    }

    public AmazonDynamoDB createDynamoDbClient(AwsCredentialView awsCredential, String region) {
        final int baseDelay = 100;
        final int maxDelay = 2000;
        final int maxRetries = 30;
        EqualJitterBackoffStrategy backoffStrategy = new EqualJitterBackoffStrategy(baseDelay, maxDelay);
        ClientConfiguration clientConfig = new ClientConfiguration()
                .withRetryPolicy(new RetryPolicy(DEFAULT_RETRY_CONDITION, backoffStrategy, maxRetries, true))
                .withMaxErrorRetry(maxRetries);
        return AmazonDynamoDBClientBuilder.standard()
                .withCredentials(getAwsStaticCredentialsProvider(awsCredential))
                .withRegion(region)
                .withClientConfiguration(clientConfig)
                .build();
    }

    public String getCbName(String groupName, Long number) {
        return String.format("%s%s", groupName, number);
    }

    public String getKeyPairName(AuthenticatedContext ac) {
        return String.format("%s%s%s%s", ac.getCloudCredential().getName(), ac.getCloudCredential().getId(),
                ac.getCloudContext().getName(), ac.getCloudContext().getId());
    }

    public boolean existingKeyPairNameSpecified(InstanceAuthentication instanceAuthentication) {
        return isNotEmpty(getExistingKeyPairName(instanceAuthentication));
    }

    public String getExistingKeyPairName(InstanceAuthentication instanceAuthentication) {
        return instanceAuthentication.getPublicKeyId();
    }

    public void checkAwsEnvironmentVariables(CloudCredential credential) {
        AwsCredentialView awsCredential = new AwsCredentialView(credential);
        if (isRoleAssumeRequired(awsCredential)) {
            validateEnvironmentForRoleAssuming(
                    awsCredential,
                    awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredential),
                    awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredential));
        }
    }

    public void validateEnvironmentForRoleAssuming(AwsCredentialView awsCredential, boolean awsAccessKeyAvailable, boolean awsSecretAccessKeyAvailable) {
        String accesKeyString = awsEnvironmentVariableChecker.getAwsAccessKeyString(awsCredential);
        String secretAccesKeyString = awsEnvironmentVariableChecker.getAwsSecretAccessKey(awsCredential);

        if (awsAccessKeyAvailable && !awsSecretAccessKeyAvailable) {
            throw new CredentialVerificationException(String.format("If '%s' available then '%s' must be set!", accesKeyString, secretAccesKeyString));
        } else if (awsSecretAccessKeyAvailable && !awsAccessKeyAvailable) {
            throw new CredentialVerificationException(String.format("If '%s' available then '%s' must be set!", accesKeyString, secretAccesKeyString));
        } else if (!awsAccessKeyAvailable) {
            try {
                try (InstanceProfileCredentialsProvider provider = new InstanceProfileCredentialsProvider()) {
                    provider.getCredentials();
                } catch (IOException e) {
                    LOGGER.error("Unable to create AWS provider", e);
                }
            } catch (AmazonClientException ignored) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("The '%s' and '%s' environment variables must be set ", accesKeyString, secretAccesKeyString));
                sb.append("or an instance profile role should be available.");
                LOGGER.info(sb.toString());
                throw new CredentialVerificationException(sb.toString());
            }
        }
    }

    public boolean roleBasedCredential(AwsCredentialView awsCredential) {
        return isNotEmpty(awsCredential.getRoleArn());
    }

    private boolean isRoleAssumeRequired(AwsCredentialView awsCredential) {
        return isNotEmpty(awsCredential.getRoleArn()) && isEmpty(awsCredential.getAccessKey()) && isEmpty(awsCredential.getSecretKey());
    }

    private BasicAWSCredentials createAwsCredentials(AwsCredentialView credentialView) {
        String accessKey = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            throw new CredentialVerificationException("Missing access or secret key from the credential.");
        }
        return new BasicAWSCredentials(accessKey, secretKey);
    }

    private <R, B extends AwsSyncClientBuilder> R createAwsClientWithCredentialProvider(AwsCredentialView awsCredential, B builder, Class<R> clazz) {
        return clazz.cast(builder.withCredentials(
                credentialClient.createSTSAssumeRoleSessionCredentialsProvider(awsCredential))
                .build());
    }
}
