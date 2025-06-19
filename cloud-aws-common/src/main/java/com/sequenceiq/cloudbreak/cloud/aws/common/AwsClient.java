package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;

import jakarta.inject.Inject;

import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClientExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonDynamoDBClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonPricingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonS3Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecretsManagerClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AwsApacheClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.mapper.SdkClientExceptionMapper;
import com.sequenceiq.cloudbreak.cloud.aws.common.metrics.AwsMetricPublisher;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.service.Retry;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.Ec2ClientBuilder;
import software.amazon.awssdk.services.ec2.endpoints.internal.DefaultEc2EndpointProvider;
import software.amazon.awssdk.services.efs.EfsClient;
import software.amazon.awssdk.services.efs.EfsClientBuilder;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2ClientBuilder;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.IamClientBuilder;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.PricingClientBuilder;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;

public abstract class AwsClient {

    // Default retries is 3. This allows more time for backoff during throttling
    public static final int MAX_CLIENT_RETRIES = 30;

    public static final int MAX_CLIENT_RETRIES_START_INSTANCES = 5;

    public static final int MAX_CLIENT_BACKOFF_MINS_START_INSTANCES = 5;

    public static final int BASE_CLIENT_BACKOFF_MINS_START_INSTANCES = 1;

    public static final int MAX_CONSECUTIVE_RETRIES_BEFORE_THROTTLING = 200;

    @Value("${cb.aws.metrics.enabled:true}")
    private boolean awsMetricsEnabled;

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
    private AwsMetricPublisher awsMetricPublisher;

    @Inject
    private AwsApacheClient awsApacheClient;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        try {
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(authenticatedContext);
            String region = authenticatedContextView.getRegion();
            AwsCredentialView awsCredentialView = authenticatedContextView.getAwsCredentialView();
            AmazonEc2Client amazonEC2Client;
            AmazonEc2Client startInstancesAmazonEc2Client;
            if (region != null) {
                amazonEC2Client = createEc2Client(awsCredentialView, region);
                startInstancesAmazonEc2Client = createEc2ClientForStartInstancesOperation(awsCredentialView, region);
                AmazonElasticLoadBalancingClient loadBalancingClient = createElasticLoadBalancingClient(awsCredentialView, region);
                authenticatedContext.putParameter(AmazonElasticLoadBalancingClient.class, loadBalancingClient);
            } else {
                amazonEC2Client = createEc2Client(awsCredentialView);
                startInstancesAmazonEc2Client = createEc2ClientForStartInstancesOperation(awsCredentialView);
            }
            authenticatedContext.putParameter(AmazonEc2Client.class, amazonEC2Client);
            authenticatedContext.putParameter("StartInstances" + AmazonEc2Client.class.getName(), startInstancesAmazonEc2Client);
        } catch (AwsServiceException e) {
            throw new CredentialVerificationException(e.getMessage(), e);
        }
        return authenticatedContext;
    }

    public AmazonEc2Client createEc2ClientForStartInstancesOperation(AwsCredentialView awsCredential, String regionName) {
        Ec2Client ec2Client = createAccessWithClientConfiguration(awsCredential, regionName, getClientConfigurationForStartInstances());
        return new AmazonEc2Client(ec2Client, retry);
    }

    public AmazonEc2Client createEc2ClientForStartInstancesOperation(AwsCredentialView awsCredential) {
        Ec2Client ec2Client = createAccessWithClientConfiguration(awsCredential, awsDefaultZoneProvider.getDefaultZone(awsCredential),
                getClientConfigurationForStartInstances());
        return new AmazonEc2Client(ec2Client, retry);
    }

    public AmazonEc2Client createAccessWithMinimalRetries(AwsCredentialView awsCredential, String regionName) {
        Ec2Client ec2Client = createAccessWithClientConfiguration(awsCredential, regionName, getClientConfigurationWithMinimalRetries());
        return new AmazonEc2Client(ec2Client, retry);
    }

    public AmazonEc2Client createEc2Client(AuthenticatedContext authenticatedContext) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().value();
        return createEc2Client(credentialView, regionName);
    }

    public AmazonEc2Client createEc2Client(AwsCredentialView awsCredential) {
        return createEc2Client(awsCredential, awsDefaultZoneProvider.getDefaultZone(awsCredential));
    }

    public AmazonEc2Client createEc2Client(AwsCredentialView awsCredential, String regionName) {
        return new AmazonEc2Client(createAccess(awsCredential, regionName), retry);
    }

    private Ec2Client createAccess(AwsCredentialView awsCredential, String regionName) {
        return createAccessWithClientConfiguration(awsCredential, regionName, getDefaultClientConfiguration());
    }

    private Ec2Client createAccessWithClientConfiguration(AwsCredentialView awsCredential, String regionName, ClientOverrideConfiguration clientConfiguration) {
        Ec2ClientBuilder ec2ClientBuilder = Ec2Client.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .overrideConfiguration(clientConfiguration)
                .endpointProvider(new DefaultEc2EndpointProvider())
                .region(Region.of(regionName));
        return proxy(ec2ClientBuilder.build(), awsCredential, regionName);
    }

    public AmazonCloudWatchClient createCloudWatchClient(AwsCredentialView awsCredential, String regionName) {
        CloudWatchClientBuilder cloudWatchClientBuilder = CloudWatchClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .overrideConfiguration(getDefaultClientConfiguration())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName));
        return new AmazonCloudWatchClient(proxy(cloudWatchClientBuilder.build(), awsCredential, regionName));
    }

    public AmazonSecurityTokenServiceClient createSecurityTokenService(AwsCredentialView awsCredential) {
        String region = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        return createSecurityTokenService(awsCredential, region);
    }

    public AmazonSecurityTokenServiceClient createSecurityTokenService(AwsCredentialView awsCredential, String regionName) {
        StsClientBuilder stsClientBuilder = StsClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .overrideConfiguration(getDefaultClientConfiguration())
                .region(Region.of(regionName));
        return new AmazonSecurityTokenServiceClient(proxy(stsClientBuilder.build(), awsCredential, regionName));
    }

    public AmazonIdentityManagementClient createAmazonIdentityManagement(AwsCredentialView awsCredential) {
        String regionName = awsDefaultZoneProvider.getDefaultZone(awsCredential);
        IamClientBuilder iamClientBuilder = IamClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .region(Region.of(regionName))
                .overrideConfiguration(getDefaultClientConfiguration())
                .credentialsProvider(getCredentialProvider(awsCredential));
        return new AmazonIdentityManagementClient(proxy(iamClientBuilder.build(), awsCredential, regionName));
    }

    public AmazonKmsClient createAWSKMS(AwsCredentialView awsCredential, String regionName) {
        KmsClient kmsClient = getAwsKmsClient(awsCredential, regionName);
        return new AmazonKmsClient(proxy(kmsClient, awsCredential, regionName));
    }

    public AmazonKmsClient createAWSKMS(AuthenticatedContext authenticatedContext) {
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().value();
        return createAWSKMS(credentialView, regionName);
    }

    public KmsClient createKmsClient(AwsCredentialView awsCredential, String regionName) {
        KmsClient kmsClient = getAwsKmsClient(awsCredential, regionName);
        return proxy(kmsClient, awsCredential, regionName);
    }

    private KmsClient getAwsKmsClient(AwsCredentialView awsCredential, String regionName) {
        return KmsClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName))
                .build();
    }

    public AmazonElasticLoadBalancingClient createElasticLoadBalancingClient(AwsCredentialView awsCredential, String regionName) {
        ElasticLoadBalancingV2ClientBuilder loadBalancingClientBuilder = ElasticLoadBalancingV2Client.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName))
                .overrideConfiguration(getDefaultClientConfiguration());
        return new AmazonElasticLoadBalancingClient(proxy(loadBalancingClientBuilder.build(), awsCredential, regionName));
    }

    public AmazonEfsClient createElasticFileSystemClient(AwsCredentialView awsCredential, String regionName) {
        EfsClientBuilder efsClientBuilder = EfsClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName));
        return new AmazonEfsClient(proxy(efsClientBuilder.build(), awsCredential, regionName), retry);
    }

    public AmazonS3Client createS3Client(AwsCredentialView awsCredential, String regionName) {
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .region(Region.of(regionName))
                .credentialsProvider(getCredentialProvider(awsCredential));
        return new AmazonS3Client(proxy(s3ClientBuilder.build(), awsCredential, regionName));
    }

    public AmazonDynamoDBClient createDynamoDbClient(AwsCredentialView awsCredential, String regionName) {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .overrideConfiguration(getDefaultClientConfiguration())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .region(Region.of(regionName));
        return new AmazonDynamoDBClient(proxy(dynamoDbClientBuilder.build(), awsCredential, regionName));
    }

    public AmazonRdsClient createRdsClient(AwsCredentialView awsCredential, String regionName) {
        RdsClientBuilder rdsClientBuilder = RdsClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .overrideConfiguration(getDefaultClientConfiguration())
                .region(Region.of(regionName));
        return new AmazonRdsClient(proxy(rdsClientBuilder.build(), awsCredential, regionName), awsPageCollector);
    }

    public AmazonRdsClient createRdsClient(AuthenticatedContext ac) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        return createRdsClient(credentialView, regionName);
    }

    public AmazonPricingClient createPricingClient(AwsCredentialView awsCredential, String regionName) {
        PricingClientBuilder clientBuilder = PricingClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .overrideConfiguration(getDefaultClientConfiguration())
                .region(Region.of(regionName));
        return new AmazonPricingClient(proxy(clientBuilder.build(), awsCredential, regionName), retry);
    }

    public AmazonSecretsManagerClient createSecretsManagerClient(AwsCredentialView awsCredential, String regionName) {
        SecretsManagerClientBuilder clientBuilder = SecretsManagerClient.builder()
                .httpClient(awsApacheClient.getApacheHttpClient())
                .credentialsProvider(getCredentialProvider(awsCredential))
                .overrideConfiguration(getDefaultClientConfiguration())
                .region(Region.of(regionName));
        return new AmazonSecretsManagerClient(proxy(clientBuilder.build(), awsCredential, regionName), retry);
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

    protected AwsCredentialsProvider getCredentialProvider(AwsCredentialView awsCredential) {
        return isRoleAssumeRequired(awsCredential) ?
                createAwsSessionCredentialProvider(awsCredential)
                : StaticCredentialsProvider.create(createAwsCredentials(awsCredential));
    }

    private AwsBasicCredentials createAwsCredentials(AwsCredentialView credentialView) {
        String accessKey = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        if (isEmpty(accessKey) || isEmpty(secretKey)) {
            throw new CredentialVerificationException("Missing access or secret key from the credential.");
        }
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    private AwsCredentialsProvider createAwsSessionCredentialProvider(AwsCredentialView awsCredential) {
        return credentialClient.createStsAssumeRoleCredentialsProvider(awsCredential);
    }

    protected <T> T proxy(T client, AwsCredentialView awsCredentialView, String regionName) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(client);
        proxyFactory.addAspect(new AmazonClientExceptionHandler(awsCredentialView, regionName, sdkClientExceptionMapper));
        return proxyFactory.getProxy();
    }

    protected ClientOverrideConfiguration getDefaultClientConfiguration() {
        ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder = ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(MAX_CLIENT_RETRIES)
                        .build());
        if (awsMetricsEnabled) {
            clientOverrideConfigurationBuilder.addMetricPublisher(awsMetricPublisher);
        }
        return clientOverrideConfigurationBuilder.build();
    }

    private ClientOverrideConfiguration getClientConfigurationWithMinimalRetries() {
        ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder = ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.defaultRetryPolicy());
        if (awsMetricsEnabled) {
            clientOverrideConfigurationBuilder.addMetricPublisher(awsMetricPublisher);
        }
        return clientOverrideConfigurationBuilder.build();
    }

    private ClientOverrideConfiguration getClientConfigurationForStartInstances() {
        ClientOverrideConfiguration.Builder clientOverrideConfigurationBuilder = ClientOverrideConfiguration.builder()
                .retryPolicy(RetryPolicy.builder()
                        .numRetries(MAX_CLIENT_RETRIES_START_INSTANCES)
                        .backoffStrategy(FullJitterBackoffStrategy.builder()
                                .maxBackoffTime(Duration.ofMinutes(MAX_CLIENT_BACKOFF_MINS_START_INSTANCES))
                                .baseDelay(Duration.ofMinutes(BASE_CLIENT_BACKOFF_MINS_START_INSTANCES))
                                .build())
                        .build());
        if (awsMetricsEnabled) {
            clientOverrideConfigurationBuilder.addMetricPublisher(awsMetricPublisher);
        }
        return clientOverrideConfigurationBuilder.build();
    }
}
