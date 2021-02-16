package com.sequenceiq.cloudbreak.cloud.aws;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystem;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonClientExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.tracing.AwsTracingRequestHandler;
import com.sequenceiq.cloudbreak.cloud.aws.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.service.Retry;

import io.opentracing.Tracer;

@Component
public class AwsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsClient.class);

    // Default retries is 3. This allows more time for backoff during throttling
    private static final int MAX_CLIENT_RETRIES = 30;

    private static final int MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING = 200;

    private AwsSessionCredentialClient credentialClient;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private Retry retry;

    @Inject
    private Tracer tracer;

    @Inject
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        try {
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(authenticatedContext);
            String region = authenticatedContextView.getRegion();
            AmazonEc2Client amazonEC2Client = region != null
                    ? createEc2Client(authenticatedContextView.getAwsCredentialView(), region)
                    : createEc2Client(authenticatedContextView.getAwsCredentialView());
            authenticatedContext.putParameter(AmazonEc2Client.class,
                    amazonEC2Client);
        } catch (AmazonServiceException e) {
            throw new CredentialVerificationException(e.getErrorMessage(), e);
        }
        return authenticatedContext;
    }

    public AmazonEc2Client createAccessWithMinimalRetries(AwsCredentialView awsCredential, String regionName) {
        AmazonEC2 ec2Client = createAccessWithClientConfiguration(awsCredential, regionName, getClientConfigurationWithMinimalRetries());
        return new AmazonEc2Client(ec2Client, retry);
    }

    public AmazonEc2Client createEc2Client(AwsCredentialView awsCredential) {
        return createEc2Client(awsCredential, awsDefaultZoneProvider.getDefaultZone(awsCredential));
    }

    public AmazonEc2Client createEc2Client(AwsCredentialView awsCredential, String regionName) {
        return new AmazonEc2Client(createAccess(awsCredential, regionName), retry);
    }

    private AmazonEC2 createAccess(AwsCredentialView awsCredential, String regionName) {
        return createAccessWithClientConfiguration(awsCredential, regionName, getDefaultClientConfiguration());
    }

    @VisibleForTesting
    AmazonEC2 createAccessWithClientConfiguration(AwsCredentialView awsCredential, String regionName, ClientConfiguration clientConfiguration) {
        return proxy(AmazonEC2Client.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withClientConfiguration(clientConfiguration)
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withRegion(regionName)
                .build(), awsCredential, regionName);
    }

    public AmazonCloudWatchClient createCloudWatchClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudWatch client = proxy(com.amazonaws.services.cloudwatch.AmazonCloudWatchClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withRegion(regionName)
                .build(), awsCredential, regionName);
        return new AmazonCloudWatchClient(client);
    }

    public AmazonSecurityTokenServiceClient createSecurityTokenService(AwsCredentialView awsCredential) {
        String region = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        return createSecurityTokenService(awsCredential, region);
    }

    public AmazonSecurityTokenServiceClient createSecurityTokenService(AwsCredentialView awsCredential, String region) {
        AWSSecurityTokenService client = proxy(com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withRegion(region)
                .build(), awsCredential, region);
        return new AmazonSecurityTokenServiceClient(client);
    }

    public AmazonSecurityTokenServiceClient createCdpSecurityTokenServiceClient(AwsCredentialView awsCredential) {
        if (!awsEnvironmentVariableChecker.isAwsAccessKeyAvailable(awsCredential)
                || !awsEnvironmentVariableChecker.isAwsSecretAccessKeyAvailable(awsCredential)) {
            LOGGER.debug("AWSSecurityTokenServiceClient will use aws metadata because environment variables are undefined");
        } else {
            LOGGER.debug("AWSSecurityTokenServiceClient will use environment variables");
        }

        String region = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        AWSSecurityTokenService client = proxy(com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient.builder()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withRegion(region)
                .build(), awsCredential, region);
        return new AmazonSecurityTokenServiceClient(client);
    }

    public AmazonIdentityManagementClient createAmazonIdentityManagement(AwsCredentialView awsCredential) {
        String region = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        AmazonIdentityManagement client = proxy(AmazonIdentityManagementClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withRegion(region)
                .withClientConfiguration(getDefaultClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential))
                .build(), awsCredential, region);
        return new AmazonIdentityManagementClient(client);
    }

    public AmazonKmsClient createAWSKMS(AwsCredentialView awsCredential, String regionName) {
        AWSKMS client = proxy(AWSKMSClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .build(), awsCredential, regionName);
        return new AmazonKmsClient(client);
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudFormation cloudFormationClient = createCloudFormation(awsCredential, regionName);
        return new AmazonCloudFormationClient(proxy(cloudFormationClient, awsCredential, regionName), retry);
    }

    @VisibleForTesting
    AmazonCloudFormation createCloudFormation(AwsCredentialView awsCredential, String regionName) {
        return com.amazonaws.services.cloudformation.AmazonCloudFormationClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withClientConfiguration(getDefaultClientConfiguration())
                .build();
    }

    public AmazonElasticLoadBalancingClient createElasticLoadBalancingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonElasticLoadBalancing client = proxy(com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withClientConfiguration(getDefaultClientConfiguration())
                .build(), awsCredential, regionName);
        return new AmazonElasticLoadBalancingClient(client);
    }

    public AmazonEfsClient createElasticFileSystemClient(AwsCredentialView awsCredential, String regionName) {
        AmazonElasticFileSystem client = proxy(com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .build(), awsCredential, regionName);
        return new AmazonEfsClient(client, retry);
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonAutoScaling client = proxy(com.amazonaws.services.autoscaling.AmazonAutoScalingClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withClientConfiguration(getDefaultClientConfiguration())
                .build(), awsCredential, regionName);
        return new AmazonAutoScalingClient(client, retry);
    }

    public AmazonS3Client createS3Client(AwsCredentialView awsCredential) {
        String regionName = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        AmazonS3 client = proxy(AmazonS3ClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .withForceGlobalBucketAccessEnabled(Boolean.TRUE)
                .build(), awsCredential, regionName);
        return new AmazonS3Client(client);
    }

    public AmazonDynamoDBClient createDynamoDbClient(AwsCredentialView awsCredential, String region) {
        final AmazonDynamoDB client = proxy(AmazonDynamoDBClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withClientConfiguration(getDynamoDbClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(region)
                .build(), awsCredential, region);
        return new AmazonDynamoDBClient(client);
    }

    public AmazonRdsClient createRdsClient(AwsCredentialView awsCredentialView, String region) {
        final AmazonRDS client = proxy(AmazonRDSClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withCredentials(getCredentialProvider(awsCredentialView))
                .withClientConfiguration(getDefaultClientConfiguration())
                .withRegion(region)
                .build(), awsCredentialView, region);
        return new AmazonRdsClient(client);
    }

    private ClientConfiguration getDefaultClientConfiguration() {
        return new ClientConfiguration()
                .withThrottledRetries(true)
                .withMaxConsecutiveRetriesBeforeThrottling(MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING)
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(MAX_CLIENT_RETRIES));
    }

    private ClientConfiguration getClientConfigurationWithMinimalRetries() {
        return new ClientConfiguration()
                .withThrottledRetries(true)
                .withMaxConsecutiveRetriesBeforeThrottling(MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING)
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicy());
    }

    private ClientConfiguration getDynamoDbClientConfiguration() {
        return new ClientConfiguration()
                .withRetryPolicy(PredefinedRetryPolicies.getDynamoDBDefaultRetryPolicyWithCustomMaxRetries(MAX_CLIENT_RETRIES));
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
        String accessKeyString = awsEnvironmentVariableChecker.getAwsAccessKeyString(awsCredential);
        String secretAccessKeyString = awsEnvironmentVariableChecker.getAwsSecretAccessKey(awsCredential);

        if (awsAccessKeyAvailable && !awsSecretAccessKeyAvailable) {
            throw new CredentialVerificationException(String.format("If '%s' available then '%s' must be set!", accessKeyString, secretAccessKeyString));
        } else if (awsSecretAccessKeyAvailable && !awsAccessKeyAvailable) {
            throw new CredentialVerificationException(String.format("If '%s' available then '%s' must be set!", secretAccessKeyString, accessKeyString));
        } else if (!awsAccessKeyAvailable) {
            try {
                try (InstanceProfileCredentialsProvider provider = getInstanceProfileProvider()) {
                    provider.getCredentials();
                } catch (IOException e) {
                    LOGGER.error("Unable to create AWS provider", e);
                    throw new CredentialVerificationException("Unable to create AWS provider");
                }
            } catch (AmazonClientException ignored) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("The '%s' and '%s' environment variables must be set ", accessKeyString, secretAccessKeyString));
                sb.append("or an instance profile role should be available.");
                LOGGER.info(sb.toString());
                throw new CredentialVerificationException(sb.toString());
            }
        }
    }

    @VisibleForTesting
    InstanceProfileCredentialsProvider getInstanceProfileProvider() {
        return InstanceProfileCredentialsProvider.getInstance();
    }

    private boolean isRoleAssumeRequired(AwsCredentialView awsCredential) {
        return isNotEmpty(awsCredential.getRoleArn()) && isEmpty(awsCredential.getAccessKey()) && isEmpty(awsCredential.getSecretKey());
    }

    private AWSCredentialsProvider getCredentialProvider(AwsCredentialView awsCredential) {
        return isRoleAssumeRequired(awsCredential) ?
                createAwsSessionCredentialProvider(awsCredential)
                : new AWSStaticCredentialsProvider(createAwsCredentials(awsCredential));
    }

    private BasicAWSCredentials createAwsCredentials(AwsCredentialView credentialView) {
        String accessKey = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            throw new CredentialVerificationException("Missing access or secret key from the credential.");
        }
        return new BasicAWSCredentials(accessKey, secretKey);
    }

    private AwsSessionCredentialProvider createAwsSessionCredentialProvider(AwsCredentialView awsCredential) {
        return new AwsSessionCredentialProvider(awsCredential, Objects.requireNonNull(credentialClient));
    }

    private <T> T proxy(T client, AwsCredentialView awsCredentialView, String region) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(client);
        proxyFactory.addAspect(new AmazonClientExceptionHandler(awsCredentialView, region, sdkClientExceptionMapper));
        return proxyFactory.getProxy();
    }

    public void setAwsSessionCredentialClient(AwsSessionCredentialClient awsSessionCredentialClient) {
        this.credentialClient = awsSessionCredentialClient;
    }
}
