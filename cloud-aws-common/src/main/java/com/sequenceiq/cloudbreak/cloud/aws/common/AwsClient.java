package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Objects;

import javax.inject.Inject;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystem;
import com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClientExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.endpoint.AwsEndpointProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.service.Retry;

public abstract class AwsClient {

    // Default retries is 3. This allows more time for backoff during throttling
    public static final int MAX_CLIENT_RETRIES = 30;

    public static final int MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING = 200;

    @Inject
    private AwsSessionCredentialClient credentialClient;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private AwsPageCollector awsPageCollector;

    @Inject
    private Retry retry;

    @Inject
    private SdkClientExceptionMapper sdkClientExceptionMapper;

    @Inject
    private AwsEndpointProvider awsEndpointProvider;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        try {
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(authenticatedContext);
            String region = authenticatedContextView.getRegion();
            AwsCredentialView awsCredentialView = authenticatedContextView.getAwsCredentialView();
            AmazonEc2Client amazonEC2Client = null;
            if (region != null) {
                amazonEC2Client = createEc2Client(awsCredentialView, region);
                AmazonElasticLoadBalancingClient loadBalancingClient = createElasticLoadBalancingClient(awsCredentialView, region);
                authenticatedContext.putParameter(AmazonElasticLoadBalancingClient.class, loadBalancingClient);
            } else {
                amazonEC2Client = createEc2Client(awsCredentialView);
            }
            authenticatedContext.putParameter(AmazonEc2Client.class, amazonEC2Client);
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
        AmazonEC2ClientBuilder clientBuilder = AmazonEC2Client.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withClientConfiguration(clientConfiguration);
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonEC2.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return proxy(clientBuilder.build(), awsCredential, regionName);
    }

    public AmazonCloudWatchClient createCloudWatchClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudWatchClientBuilder clientBuilder = com.amazonaws.services.cloudwatch.AmazonCloudWatchClient.builder()
                .withClientConfiguration(getDefaultClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential));
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonCloudWatch.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonCloudWatchClient(proxy(clientBuilder.build(), awsCredential, regionName));
    }

    public AmazonSecurityTokenServiceClient createSecurityTokenService(AwsCredentialView awsCredential) {
        String region = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        return createSecurityTokenService(awsCredential, region);
    }

    public AmazonSecurityTokenServiceClient createSecurityTokenService(AwsCredentialView awsCredential, String regionName) {
        AWSSecurityTokenServiceClientBuilder clientBuilder = com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withClientConfiguration(getDefaultClientConfiguration());
        awsEndpointProvider.setupEndpoint(clientBuilder, AWSSecurityTokenService.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonSecurityTokenServiceClient(proxy(clientBuilder.build(), awsCredential, regionName));
    }

    public AmazonIdentityManagementClient createAmazonIdentityManagement(AwsCredentialView awsCredential) {
        String region = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        AmazonIdentityManagementClientBuilder clientBuilder = AmazonIdentityManagementClientBuilder.standard()
                .withClientConfiguration(getDefaultClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential));
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonIdentityManagement.ENDPOINT_PREFIX, region, awsCredential.isGovernmentCloudEnabled());
        return new AmazonIdentityManagementClient(proxy(clientBuilder.build(), awsCredential, region));
    }

    public AmazonKmsClient createAWSKMS(AwsCredentialView awsCredential, String regionName) {
        AWSKMSClientBuilder clientBuilder = AWSKMSClientBuilder.standard()
                .withCredentials(getCredentialProvider(awsCredential));
        awsEndpointProvider.setupEndpoint(clientBuilder, AWSKMS.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonKmsClient(proxy(clientBuilder.build(), awsCredential, regionName));
    }

    public AmazonElasticLoadBalancingClient createElasticLoadBalancingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonElasticLoadBalancingClientBuilder clientBuilder = com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient.builder()
                .withCredentials(getCredentialProvider(awsCredential))
                .withClientConfiguration(getDefaultClientConfiguration());
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonElasticLoadBalancing.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonElasticLoadBalancingClient(proxy(clientBuilder.build(), awsCredential, regionName));
    }

    public AmazonEfsClient createElasticFileSystemClient(AwsCredentialView awsCredential, String regionName) {
        AmazonElasticFileSystemClientBuilder clientBuilder = com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClient.builder()
                .withCredentials(getCredentialProvider(awsCredential));
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonElasticFileSystem.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonEfsClient(proxy(clientBuilder.build(), awsCredential, regionName), retry);
    }

    public AmazonS3Client createS3Client(AwsCredentialView awsCredential) {
        String regionName = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard()
                .withCredentials(getCredentialProvider(awsCredential))
                .withForceGlobalBucketAccessEnabled(Boolean.TRUE);
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonS3.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonS3Client(proxy(clientBuilder.build(), awsCredential, regionName));
    }

    public AmazonDynamoDBClient createDynamoDbClient(AwsCredentialView awsCredential, String regionName) {
        AmazonDynamoDBClientBuilder clientBuilder = AmazonDynamoDBClientBuilder.standard()
                .withClientConfiguration(getDynamoDbClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential));
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonDynamoDB.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonDynamoDBClient(proxy(clientBuilder.build(), awsCredential, regionName));
    }

    public AmazonRdsClient createRdsClient(AwsCredentialView awsCredential, String regionName) {
        AmazonRDSClientBuilder clientBuilder = AmazonRDSClientBuilder.standard()
                .withCredentials(getCredentialProvider(awsCredential))
                .withClientConfiguration(getDefaultClientConfiguration());
        awsEndpointProvider.setupEndpoint(clientBuilder, AmazonRDS.ENDPOINT_PREFIX, regionName, awsCredential.isGovernmentCloudEnabled());
        return new AmazonRdsClient(proxy(clientBuilder.build(), awsCredential, regionName), awsPageCollector);
    }

    protected ClientConfiguration getDefaultClientConfiguration() {
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
        String secretAccessKeyString = awsEnvironmentVariableChecker.getAwsSecretAccessKeyString(awsCredential);

        if (awsAccessKeyAvailable && !awsSecretAccessKeyAvailable) {
            throw new CredentialVerificationException(String.format("If '%s' available then '%s' must be set!", accessKeyString, secretAccessKeyString));
        } else if (awsSecretAccessKeyAvailable && !awsAccessKeyAvailable) {
            throw new CredentialVerificationException(String.format("If '%s' available then '%s' must be set!", secretAccessKeyString, accessKeyString));
        }
    }

    private boolean isRoleAssumeRequired(AwsCredentialView awsCredential) {
        return isNotEmpty(awsCredential.getRoleArn()) && isEmpty(awsCredential.getAccessKey()) && isEmpty(awsCredential.getSecretKey());
    }

    protected AWSCredentialsProvider getCredentialProvider(AwsCredentialView awsCredential) {
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

    protected <T> T proxy(T client, AwsCredentialView awsCredentialView, String region) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(client);
        proxyFactory.addAspect(new AmazonClientExceptionHandler(awsCredentialView, region, sdkClientExceptionMapper));
        return proxyFactory.getProxy();
    }

    protected AwsEndpointProvider getAwsEndpointProvider() {
        return awsEndpointProvider;
    }
}
