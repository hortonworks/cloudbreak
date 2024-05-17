package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper.validateRequestCloudResource;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static com.sequenceiq.common.api.type.ResourceType.AWS_SECRETSMANAGER_SECRET;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.SecretConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecretsManagerClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.cloud.model.secret.CloudSecret;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.CreateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.DeleteCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.GetCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretRequest;
import com.sequenceiq.cloudbreak.cloud.model.secret.request.UpdateCloudSecretResourceAccessRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CommonStatus;

import software.amazon.awssdk.policybuilder.iam.IamAction;
import software.amazon.awssdk.policybuilder.iam.IamConditionKey;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.policybuilder.iam.IamResource;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.Tag;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;

@Service
public class AwsSecretsManagerConnector implements SecretConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSecretsManagerConnector.class);

    private static final EncryptionKeySource DEFAULT_AWS_MANAGED_KEY_SOURCE = EncryptionKeySource.builder()
            .withKeyType(EncryptionKeyType.AWS_MANAGED_KEY)
            .build();

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private AwsIamService awsIamService;

    @Inject
    private ArnService arnService;

    @Override
    public CloudSecret createCloudSecret(CreateCloudSecretRequest request) {
        AmazonSecretsManagerClient secretsManagerClient = awsClient.createSecretsManagerClient(new AwsCredentialView(request.cloudCredential()),
                request.cloudContext().getLocation().getRegion().getRegionName());
        CloudSecret cloudSecret;
        Optional<CloudResource> existingCloudResource = getExistingCloudResource(request.cloudResources(), request.secretName());
        if (existingCloudResource.isPresent()) {
            LOGGER.info("Secret CloudResource with name [{}] found.", request.secretName());
            cloudSecret = getCloudSecret(existingCloudResource.get().getReference(), secretsManagerClient);
        } else {
            Optional<DescribeSecretResponse> describeSecretResponse = describeSecretIfItExistsOnProvider(request.secretName(), secretsManagerClient);
            if (describeSecretResponse.isPresent()) {
                LOGGER.info("Secret CloudResource with name [{}] not found, but it exists on provider-side with ARN [{}].",
                        request.secretName(), describeSecretResponse.get().arn());
                GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                        .secretId(describeSecretResponse.get().arn())
                        .build();
                CloudResource cloudResource = getCloudResource(describeSecretResponse.get().name(), describeSecretResponse.get().arn());
                persistenceNotifier.notifyAllocation(cloudResource, request.cloudContext());
                GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
                cloudSecret = getCloudSecret(describeSecretResponse.get(), getSecretValueResponse);
            } else {
                LOGGER.info("Secret with name [{}] not found on provider. Creating secret...", request.secretName());
                CreateSecretRequest createSecretRequest = getCreateSecretRequest(request);
                CreateSecretResponse createSecretResponse = secretsManagerClient.createSecret(createSecretRequest);
                CloudResource cloudResource = getCloudResource(createSecretRequest.name(), createSecretResponse.arn());
                persistenceNotifier.notifyAllocation(cloudResource, request.cloudContext());
                cloudSecret = getCloudSecret(createSecretResponse.arn(), secretsManagerClient);
                LOGGER.debug("Secret with secretId [{}] created.", cloudSecret.secretName());
            }
        }
        return cloudSecret;
    }

    @Override
    public CloudSecret getCloudSecret(GetCloudSecretRequest request) {
        validateRequestCloudResource(request.cloudResource(), AWS_SECRETSMANAGER_SECRET);
        AmazonSecretsManagerClient secretsManagerClient = awsClient.createSecretsManagerClient(new AwsCredentialView(request.cloudCredential()),
                request.cloudContext().getLocation().getRegion().getRegionName());
        return getCloudSecret(request.cloudResource().getReference(), secretsManagerClient);
    }

    @Override
    public CloudSecret updateCloudSecret(UpdateCloudSecretRequest request) {
        if (!Optionals.isAnyPresent(request.newSecretValue(), request.newEncryptionKeySource())) {
            throw new IllegalArgumentException("Either a newSecretValue or a newEncryptionKeySource needs to be specified!");
        }
        validateRequestCloudResource(request.cloudResource(), AWS_SECRETSMANAGER_SECRET);
        AmazonSecretsManagerClient secretsManagerClient = awsClient.createSecretsManagerClient(new AwsCredentialView(request.cloudCredential()),
                request.cloudContext().getLocation().getRegion().getRegionName());
        CloudSecret cloudSecret = getCloudSecret(request.cloudResource().getReference(), secretsManagerClient);

        UpdateSecretRequest.Builder requestBuilder = UpdateSecretRequest.builder()
                .secretId(request.cloudResource().getReference());
        request.newSecretValue().ifPresent(requestBuilder::secretString);
        request.newEncryptionKeySource().ifPresent(value -> requestBuilder.kmsKeyId(value.keyValue()));
        UpdateSecretRequest updateSecretRequest = requestBuilder.build();
        if (updateSecretRequest.secretString().equals(cloudSecret.secretValue()) &&
                updateSecretRequest.kmsKeyId().equals(cloudSecret.keySource().keyValue())) {
            LOGGER.info("The secret with secretId [{}] doesn't need to be updated based on the provided parameters, therefore skipping the update call...",
                    cloudSecret.secretId());
        } else {
            secretsManagerClient.updateSecret(updateSecretRequest);
            LOGGER.debug("Updated secret with secretId [{}].", cloudSecret.secretId());
            CloudSecret.Builder cloudSecretBuilder = cloudSecret.toBuilder();
            request.newSecretValue().ifPresent(cloudSecretBuilder::withSecretValue);
            request.newEncryptionKeySource().ifPresent(cloudSecretBuilder::withKeySource);
            cloudSecret = cloudSecretBuilder.build();
        }
        return cloudSecret;
    }

    @Override
    public void deleteCloudSecret(DeleteCloudSecretRequest request) {
        AmazonSecretsManagerClient secretsManagerClient = awsClient.createSecretsManagerClient(new AwsCredentialView(request.cloudCredential()),
                request.cloudContext().getLocation().getRegion().getRegionName());
        Optional<CloudResource> existingCloudResource = getExistingCloudResource(request.cloudResources(), request.secretName());
        if (existingCloudResource.isPresent()) {
            DeleteSecretRequest deleteSecretRequest = DeleteSecretRequest.builder()
                    .secretId(existingCloudResource.get().getReference())
                    // Makes the delete idempotent, as AWS won't throw ResourceNotFoundException with this being true
                    .forceDeleteWithoutRecovery(true)
                    .build();
            secretsManagerClient.deleteSecret(deleteSecretRequest);
            persistenceNotifier.notifyDeletion(existingCloudResource.get(), request.cloudContext());
            LOGGER.debug("Deleted secret with name [{}]...", request.secretName());
        } else {
            LOGGER.info("Secret CloudResource with name [{}] already deleted.", request.secretName());
        }
    }

    @Override
    public EncryptionKeySource getDefaultEncryptionKeySource() {
        return DEFAULT_AWS_MANAGED_KEY_SOURCE;
    }

    @Override
    public CloudSecret updateCloudSecretResourceAccess(UpdateCloudSecretResourceAccessRequest request) {
        validateUpdateCloudSecretResourceAccessRequest(request);
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.cloudCredential());
        AmazonSecretsManagerClient secretsManagerClient = awsClient.createSecretsManagerClient(awsCredentialView,
                request.cloudContext().getLocation().getRegion().getRegionName());
        CloudSecret cloudSecret = getCloudSecret(request.cloudResource().getReference(), secretsManagerClient);
        String iamPolicyJson = getIamPolicyJson(awsCredentialView, request.cryptographicPrincipals(), request.cryptographicAuthorizedClients());
        LOGGER.info("Updating the resource policy for secret [{}] to the following: {}", request.cloudResource().getName(), iamPolicyJson);
        PutResourcePolicyRequest putResourcePolicyRequest = PutResourcePolicyRequest.builder()
                .secretId(request.cloudResource().getReference())
                .resourcePolicy(iamPolicyJson)
                .build();
        secretsManagerClient.putResourcePolicy(putResourcePolicyRequest);
        return cloudSecret.toBuilder()
                .withCryptographicPrincipals(request.cryptographicPrincipals())
                .withCryptographicAuthorizedClients(request.cryptographicAuthorizedClients())
                .build();
    }

    private void validateUpdateCloudSecretResourceAccessRequest(UpdateCloudSecretResourceAccessRequest request) {
        throwIfNull(request, () -> new IllegalArgumentException("request must not be null!"));
        throwIfNull(request.cloudContext(), () -> new IllegalArgumentException("request.cloudContext must not be null!"));
        throwIfNull(request.cloudCredential(), () -> new IllegalArgumentException("request.cloudCredential must not be null!"));
        validateRequestCloudResource(request.cloudResource(), AWS_SECRETSMANAGER_SECRET);
        validatePrincipals(request.cryptographicPrincipals(), "request.cryptographicPrincipals");
        validateAuthorizedClients(request.cryptographicAuthorizedClients(), "request.cryptographicAuthorizedClients");
    }

    private void validatePrincipals(List<String> principals, String propertyName) {
        List<String> badPrincipals = Optional.ofNullable(principals).orElse(List.of()).stream()
                .filter(iamResourceArn -> !isInstanceProfileOrRoleArn(iamResourceArn))
                .toList();
        if (!badPrincipals.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("The following elements of %s are malformed. Only IAM instance-profile and role resource ARNs are supported. %s",
                            propertyName, badPrincipals));
        }
    }

    private boolean isInstanceProfileOrRoleArn(String iamResourceArn) {
        return arnService.isInstanceProfileArn(iamResourceArn) || arnService.isRoleArn(iamResourceArn);
    }

    private void validateAuthorizedClients(List<String> authorizedClients, String propertyName) {
        List<String> badAuthorizedClients = Optional.ofNullable(authorizedClients).orElse(List.of()).stream()
                .filter(resourceArn -> !arnService.isEc2InstanceArn(resourceArn))
                .toList();
        if (!badAuthorizedClients.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("The following elements of %s are malformed. Only EC2 instance resource ARNs are supported. %s",
                            propertyName, badAuthorizedClients));
        }
    }

    private static CloudResource getCloudResource(String name, String arn) {
        CloudResource cloudResource = CloudResource.builder()
                .withName(name)
                .withReference(arn)
                .withType(AWS_SECRETSMANAGER_SECRET)
                .withStatus(CommonStatus.CREATED)
                .build();
        return cloudResource;
    }

    private String getIamPolicyJson(AwsCredentialView awsCredentialView, List<String> cryptographicPrincipals,
            List<String> cryptographicAuthorizedClients) {
        IamPolicy iamPolicy = IamPolicy.builder()
                .id("Policy generated by CDP")
                .addStatement(IamStatement.builder()
                        .sid("RestrictAccessToSpecificInstances")
                        .effect(IamEffect.ALLOW)
                        .addAction(IamAction.create("secretsmanager:DeleteSecret"))
                        .addAction(IamAction.create("secretsmanager:GetSecretValue"))
                        .addResource(IamResource.ALL)
                        .addPrincipals(IamPrincipalType.AWS,
                                awsIamService.getEffectivePrincipals(awsClient.createAmazonIdentityManagement(awsCredentialView), cryptographicPrincipals))
                        .addConditions(IamConditionOperator.ARN_EQUALS, IamConditionKey.create("ec2:SourceInstanceArn"), cryptographicAuthorizedClients)
                        .build())
                .build();
        return iamPolicy.toJson();
    }

    private static Optional<CloudResource> getExistingCloudResource(List<CloudResource> cloudResources, String secretName) {
        return cloudResources.stream()
                .filter(cr -> secretName.equals(cr.getName()) && AWS_SECRETSMANAGER_SECRET.equals(cr.getType()))
                .findFirst();
    }

    private static CloudSecret getCloudSecret(String secretArn, AmazonSecretsManagerClient secretsManagerClient) {
        try {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretArn)
                    .build();
            GetSecretValueResponse getSecretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            DescribeSecretRequest describeSecretRequest = DescribeSecretRequest.builder()
                    .secretId(secretArn)
                    .build();
            DescribeSecretResponse describeSecretResponse = secretsManagerClient.describeSecret(describeSecretRequest);
            return getCloudSecret(describeSecretResponse, getSecretValueResponse);
        } catch (ResourceNotFoundException e) {
            LOGGER.info("CloudSecret with ARN [{}] not found on provider!", secretArn, e);
            throw new NotFoundException(String.format("The secret with ARN [%s] does not exist on the provider!", secretArn));
        }
    }

    private static Optional<DescribeSecretResponse> describeSecretIfItExistsOnProvider(String secretName, AmazonSecretsManagerClient secretsManagerClient) {
        DescribeSecretRequest describeSecretRequest = DescribeSecretRequest.builder()
                .secretId(secretName)
                .build();
        try {
            return Optional.of(secretsManagerClient.describeSecret(describeSecretRequest));
        } catch (ResourceNotFoundException e) {
            LOGGER.debug("Secret with name [{}] does not exist on the provider.", secretName);
        }
        return Optional.empty();
    }

    private static CloudSecret getCloudSecret(DescribeSecretResponse describeSecretResponse, GetSecretValueResponse getSecretValueResponse) {
        EncryptionKeySource.Builder encryptionKeySourceBuilder = EncryptionKeySource.builder();
        if (describeSecretResponse.kmsKeyId() == null) {
            encryptionKeySourceBuilder
                    .withKeyType(EncryptionKeyType.AWS_MANAGED_KEY);
        } else {
            encryptionKeySourceBuilder
                    .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                    .withKeyValue(describeSecretResponse.kmsKeyId());
        }
        return CloudSecret.builder()
                .withSecretId(describeSecretResponse.arn())
                .withSecretName(describeSecretResponse.name())
                .withSecretValue(getSecretValueResponse.secretString())
                .withDescription(describeSecretResponse.description())
                .withKeySource(encryptionKeySourceBuilder.build())
                .withDeletionDate(describeSecretResponse.deletedDate())
                .withTags(describeSecretResponse.tags().stream()
                        .collect(Collectors.toMap(Tag::key, Tag::value)))
                .build();
    }

    private CreateSecretRequest getCreateSecretRequest(CreateCloudSecretRequest request) {
        CreateSecretRequest.Builder requestBuilder = CreateSecretRequest.builder()
                .name(request.secretName())
                .secretString(request.secretValue())
                .description(request.description())
                .tags(awsTaggingService.prepareSecretsManagerTags(request.tags()));
        request.encryptionKeySource().ifPresent(value -> {
            if (value.keyType().equals(EncryptionKeyType.AWS_KMS_KEY_ARN)) {
                requestBuilder.kmsKeyId(value.keyValue());
            } else {
                LOGGER.warn("Only EncryptionKeyType of {} is allowed when creating AWS Secrets Manager secrets! " +
                                "Using the default AWS managed encryption key instead...", EncryptionKeyType.AWS_KMS_KEY_ARN);
            }
        });
        return requestBuilder.build();
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }
}
