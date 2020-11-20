package com.sequenceiq.cloudbreak.cloud.aws;

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
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonElbV2RetryClient;
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

    @Inject
    private AwsSessionCredentialClient credentialClient;

    @Inject
    private AwsDefaultZoneProvider awsDefaultZoneProvider;

    @Inject
    private AwsEnvironmentVariableChecker awsEnvironmentVariableChecker;

    @Inject
    private Retry retry;

    @Inject
    private Tracer tracer;

    public AuthenticatedContext createAuthenticatedContext(CloudContext cloudContext, CloudCredential cloudCredential) {
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        try {
            AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(authenticatedContext);
            String region = authenticatedContextView.getRegion();
            AmazonEC2Client amazonEC2Client = region != null ?
                    createAccess(authenticatedContextView.getAwsCredentialView(), region) : createAccess(cloudCredential);
            authenticatedContext.putParameter(AmazonEC2Client.class,
                    amazonEC2Client);
        } catch (AmazonServiceException e) {
            throw new CredentialVerificationException(e.getErrorMessage(), e);
        }
        return authenticatedContext;
    }

    public AmazonEC2Client createAccess(CloudCredential credential) {
        return createAccess(new AwsCredentialView(credential), awsDefaultZoneProvider.getDefaultZone(credential));
    }

    public AmazonEC2Client createAccess(AwsCredentialView awsCredential, String regionName) {
        return createAccessWithClientConfiguration(awsCredential, regionName, getDefaultClientConfiguration());
    }

    public AmazonEC2Client createAccessWithMinimalRetries(AwsCredentialView awsCredential, String regionName) {
        return createAccessWithClientConfiguration(awsCredential, regionName, getClientConfigurationWithMinimalRetries());
    }

    public AmazonEC2Client createAccessWithClientConfiguration(AwsCredentialView awsCredential, String regionName, ClientConfiguration clientConfiguration) {
        AmazonEC2Client client = isRoleAssumeRequired(awsCredential) ?
                getAmazonEC2Client(createAwsSessionCredentialProvider(awsCredential), clientConfiguration) :
                getAmazonEC2Client(createAwsCredentials(awsCredential), clientConfiguration);
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AmazonEC2Client getAmazonEC2Client(AwsSessionCredentialProvider awsSessionCredentialProvider, ClientConfiguration clientConfiguration) {
        AmazonEC2Client client = new AmazonEC2Client(awsSessionCredentialProvider, clientConfiguration);
        client.addRequestHandler(new AwsTracingRequestHandler(tracer));
        return client;
    }

    public AmazonEC2Client getAmazonEC2Client(BasicAWSCredentials basicAWSCredentials, ClientConfiguration clientConfiguration) {
        AmazonEC2Client client = new AmazonEC2Client(basicAWSCredentials, clientConfiguration);
        client.addRequestHandler(new AwsTracingRequestHandler(tracer));
        return client;
    }

    public AmazonCloudWatchClient createCloudWatchClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudWatchClient client = isRoleAssumeRequired(awsCredential) ?
                new AmazonCloudWatchClient(createAwsSessionCredentialProvider(awsCredential)) :
                new AmazonCloudWatchClient(createAwsCredentials(awsCredential));
        client.setRegion(RegionUtils.getRegion(regionName));
        return client;
    }

    public AWSSecurityTokenService createAwsSecurityTokenService(AwsCredentialView awsCredential) {
        return isRoleAssumeRequired(awsCredential)
                ? new AWSSecurityTokenServiceClient(createAwsSessionCredentialProvider(awsCredential))
                : new AWSSecurityTokenServiceClient(createAwsCredentials(awsCredential));
    }

    public AmazonIdentityManagement createAmazonIdentityManagement(AwsCredentialView awsCredential) {
        return AmazonIdentityManagementClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withRegion(awsDefaultZoneProvider.getDefaultZone(awsCredential))
                .withClientConfiguration(getDefaultClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential))
                .build();
    }

    public AWSKMS createAWSKMS(AwsCredentialView awsCredential, String regionName) {
        return AWSKMSClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(regionName)
                .build();
    }

    public AmazonCloudFormationClient createCloudFormationClient(AwsCredentialView awsCredential, String regionName) {
        AmazonCloudFormationClient client = isRoleAssumeRequired(awsCredential) ?
                new AmazonCloudFormationClient(createAwsSessionCredentialProvider(awsCredential), getDefaultClientConfiguration()) :
                new AmazonCloudFormationClient(createAwsCredentials(awsCredential), getDefaultClientConfiguration());
        client.setRegion(RegionUtils.getRegion(regionName));
        client.addRequestHandler(new AwsTracingRequestHandler(tracer));
        return client;
    }

    public AmazonCloudFormationRetryClient createCloudFormationRetryClient(AwsCredentialView awsCredential, String regionName) {
        return new AmazonCloudFormationRetryClient(createCloudFormationClient(awsCredential, regionName), retry);
    }

    public AmazonCloudFormationRetryClient createCloudFormationRetryClient(AmazonCloudFormationClient amazonCloudFormationClient) {
        return new AmazonCloudFormationRetryClient(amazonCloudFormationClient, retry);
    }

    public AmazonElasticLoadBalancingClient createElasticLoadBalancingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonElasticLoadBalancingClient client = isRoleAssumeRequired(awsCredential) ?
            new AmazonElasticLoadBalancingClient(createAwsSessionCredentialProvider(awsCredential), getDefaultClientConfiguration()) :
            new AmazonElasticLoadBalancingClient(createAwsCredentials(awsCredential), getDefaultClientConfiguration());
        client.setRegion(RegionUtils.getRegion(regionName));
        client.addRequestHandler(new AwsTracingRequestHandler(tracer));
        return client;
    }

    public AmazonElbV2RetryClient createElbV2RetryClient(AwsCredentialView awsCredential, String regionName) {
        return new AmazonElbV2RetryClient(createElasticLoadBalancingClient(awsCredential, regionName), retry);
    }

    public AmazonElbV2RetryClient createElbV2RetryClient(AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient) {
        return new AmazonElbV2RetryClient(amazonElasticLoadBalancingClient, retry);
    }

    public AmazonAutoScalingClient createAutoScalingClient(AwsCredentialView awsCredential, String regionName) {
        AmazonAutoScalingClient client = isRoleAssumeRequired(awsCredential) ?
                new AmazonAutoScalingClient(createAwsSessionCredentialProvider(awsCredential), getDefaultClientConfiguration()) :
                new AmazonAutoScalingClient(createAwsCredentials(awsCredential), getDefaultClientConfiguration());
        client.setRegion(RegionUtils.getRegion(regionName));
        client.addRequestHandler(new AwsTracingRequestHandler(tracer));
        return client;
    }

    public AmazonAutoScalingRetryClient createAutoScalingRetryClient(AwsCredentialView awsCredential, String regionName) {
        return new AmazonAutoScalingRetryClient(createAutoScalingClient(awsCredential, regionName), retry);
    }

    public AmazonS3 createS3Client(AwsCredentialView awsCredential) {
        return AmazonS3ClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(awsDefaultZoneProvider.getDefaultZone(awsCredential))
                .withForceGlobalBucketAccessEnabled(Boolean.TRUE)
                .build();
    }

    public AmazonDynamoDB createDynamoDbClient(AwsCredentialView awsCredential, String region) {
        return AmazonDynamoDBClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withClientConfiguration(getDynamoDbClientConfiguration())
                .withCredentials(getCredentialProvider(awsCredential))
                .withRegion(region)
                .build();
    }

    public AmazonRDS createRdsClient(AwsCredentialView awsCredentialView, String region) {
        return AmazonRDSClientBuilder.standard()
                .withRequestHandlers(new AwsTracingRequestHandler(tracer))
                .withCredentials(getCredentialProvider(awsCredentialView))
                .withClientConfiguration(getDefaultClientConfiguration())
                .withRegion(region)
                .build();
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

    public InstanceProfileCredentialsProvider getInstanceProfileProvider() {
        return new InstanceProfileCredentialsProvider();
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
        return new AwsSessionCredentialProvider(awsCredential, credentialClient);
    }
}
