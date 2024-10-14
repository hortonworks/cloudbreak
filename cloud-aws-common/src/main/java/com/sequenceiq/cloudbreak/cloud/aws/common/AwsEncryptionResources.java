package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper.validateRequestCloudResource;
import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static com.sequenceiq.common.api.type.ResourceType.AWS_KMS_KEY;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AmazonKmsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyEnableAutoRotationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyRotationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.UpdateEncryptionKeyResourceAccessRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;

import software.amazon.awssdk.policybuilder.iam.IamCondition;
import software.amazon.awssdk.policybuilder.iam.IamConditionKey;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.policybuilder.iam.IamResource;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.EnableKeyRotationRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.OriginType;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.RotateKeyOnDemandRequest;
import software.amazon.awssdk.services.kms.model.Tag;

@Service
public class AwsEncryptionResources implements EncryptionResources {

    static final String TAG_KEY_CLOUDERA_KMS_KEY_TARGET = "Cloudera-KMS-Key-Target";

    static final String POLICY_NAME = "default";

    static final String POLICY_STATEMENT_ID_KEY_ADMINISTRATOR = "Allow access for the CDP credential IAM role as a key administrator";

    static final String POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER = "Allow use of the key by CDP clusters for cryptographic operations";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEncryptionResources.class);

    private static final IamConditionKey POLICY_CONDITION_KEY_EC2_SOURCE_INSTANCE_ARN = IamConditionKey.create("ec2:SourceInstanceArn");

    private static final IamConditionKey POLICY_CONDITION_KEY_KMS_ENCRYPTION_CONTEXT_SECRET_ARN = IamConditionKey.create("kms:EncryptionContext:SecretARN");

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AmazonKmsUtil amazonKmsUtil;

    @Inject
    private AwsIamService awsIamService;

    @Inject
    private ArnService arnService;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }

    @Override
    public CloudEncryptionKey createEncryptionKey(EncryptionKeyCreationRequest request) {
        validateEncryptionKeyCreationRequest(request);
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.cloudCredential());
        CloudContext cloudContext = request.cloudContext();
        AmazonKmsClient kmsClient = awsClient.createAWSKMS(awsCredentialView, cloudContext.getLocation().getRegion().value());

        String tagValueClouderaKMSKeyTarget = request.keyName();
        LOGGER.info("Checking if AWS_KMS_KEY CloudResource with name \"{}\" exists", tagValueClouderaKMSKeyTarget);
        Optional<CloudResource> keyCloudResourceOptional = getKeyCloudResourceByTarget(request.cloudResources(),
                tagValueClouderaKMSKeyTarget);
        KeyMetadata keyMetadata;
        if (keyCloudResourceOptional.isPresent()) {
            CloudResource keyCloudResource = keyCloudResourceOptional.get();
            LOGGER.info("Found existing CloudResource {}", keyCloudResource);
            keyMetadata = amazonKmsUtil.getKeyMetadataByKeyId(kmsClient, keyCloudResource.getReference());
        } else {
            LOGGER.info("No matching CloudResource found. Checking if KMS Key with tag \"{}\"=\"{}\" exists", TAG_KEY_CLOUDERA_KMS_KEY_TARGET,
                    tagValueClouderaKMSKeyTarget);
            Optional<KeyMetadata> keyMetadataOptional = findKeyByTargetTag(kmsClient, tagValueClouderaKMSKeyTarget);
            if (keyMetadataOptional.isPresent()) {
                keyMetadata = keyMetadataOptional.get();
                LOGGER.info("Found existing KMS Key {}", keyMetadata);
            } else {
                LOGGER.info("No matching KMS Key found. Creating a new one.");
                keyMetadata = createKey(request, awsCredentialView, kmsClient);
            }
            createKeyCloudResource(cloudContext, keyMetadata, tagValueClouderaKMSKeyTarget);
        }

        return toCloudEncryptionKey(keyMetadata, tagValueClouderaKMSKeyTarget);
    }

    private void validateEncryptionKeyCreationRequest(EncryptionKeyCreationRequest request) {
        throwIfNull(request, () -> new IllegalArgumentException("request must not be null!"));
        if (isBlank(request.keyName())) {
            throw new IllegalArgumentException("request.keyName must not be null, empty or blank!");
        }
        throwIfNull(request.cloudContext(), () -> new IllegalArgumentException("request.cloudContext must not be null!"));
        throwIfNull(request.cloudCredential(), () -> new IllegalArgumentException("request.cloudCredential must not be null!"));
        throwIfNull(request.tags(), () -> new IllegalArgumentException("request.tags must not be null!"));
        validatePrincipals(request.cryptographicPrincipals(), "request.cryptographicPrincipals", true);
    }

    private void validatePrincipals(List<String> principals, String propertyName, boolean mustNotBeEmpty) {
        if (mustNotBeEmpty && isEmpty(principals)) {
            throw new IllegalArgumentException(String.format("%s must not be null or empty!", propertyName));
        }
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

    private Optional<CloudResource> getKeyCloudResourceByTarget(List<CloudResource> cloudResources, String tagValueClouderaKMSKeyTarget) {
        return cloudResourceHelper.getResourceTypeInstancesFromList(AWS_KMS_KEY, Objects.requireNonNullElse(cloudResources, List.of()))
                .stream()
                .filter(r -> tagValueClouderaKMSKeyTarget.equals(r.getName()))
                .findFirst();
    }

    private Optional<KeyMetadata> findKeyByTargetTag(AmazonKmsClient kmsClient, String tagValueClouderaKMSKeyTarget) {
        List<KeyListEntry> listKeyResult = amazonKmsUtil.listKeysWithAllPages(kmsClient);
        List<String> keyArns = listKeyResult.stream()
                .map(KeyListEntry::keyArn)
                .toList();
        LOGGER.info("Existing KMS Key ARNs: {}", keyArns);
        Predicate<? super String> predicate = (Predicate<String>) keyArn -> {
            ListResourceTagsRequest listResourceTagsRequest = ListResourceTagsRequest.builder()
                    .keyId(keyArn)
                    .build();
            List<Tag> tags = new ArrayList<>();
            try {
                tags = amazonKmsUtil.listResourceTagsWithAllPages(kmsClient, listResourceTagsRequest);
            } catch (Exception e) {
                LOGGER.error("Unable to fetch tags for {}", keyArn, e);
            }
            return tags.stream()
                    .anyMatch(tag -> TAG_KEY_CLOUDERA_KMS_KEY_TARGET.equals(tag.tagKey()) && tagValueClouderaKMSKeyTarget.equals(tag.tagValue()));
        };
        return keyArns.stream()
                .filter(predicate)
                .findFirst()
                .map(keyArn -> amazonKmsUtil.getKeyMetadataByKeyId(kmsClient, keyArn));
    }

    private KeyMetadata createKey(EncryptionKeyCreationRequest request, AwsCredentialView awsCredentialView, AmazonKmsClient kmsClient) {
        CreateKeyRequest createKeyRequest = CreateKeyRequest.builder()
                .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .description(request.description())
                .origin(OriginType.AWS_KMS)
                .policy(buildKeyPolicyJsonForCreate(awsCredentialView, request.cryptographicPrincipals()))
                .tags(getTagsForCreate(request.tags(), request.keyName()))
                .build();
        return kmsClient.createKey(createKeyRequest).keyMetadata();
    }

    private String buildKeyPolicyJsonForCreate(AwsCredentialView awsCredentialView, List<String> cryptographicPrincipals) {
        String credentialRoleArn = awsCredentialView.getRoleArn();
        IamPolicy iamPolicy = IamPolicy.builder()
                .id("Policy generated by CDP")
                .addStatement(IamStatement.builder()
                        .sid("Enable IAM user permissions")
                        .effect(IamEffect.ALLOW)
                        .addPrincipal(IamPrincipalType.AWS, awsIamService.getAccountRootArn(credentialRoleArn))
                        .addAction("kms:*")
                        .addResource(IamResource.ALL)
                        .build())
                .addStatement(IamStatement.builder()
                        .sid(POLICY_STATEMENT_ID_KEY_ADMINISTRATOR)
                        .effect(IamEffect.ALLOW)
                        .addPrincipal(IamPrincipalType.AWS, credentialRoleArn)
                        .addAction("kms:EnableKeyRotation")
                        .addAction("kms:GetKeyPolicy")
                        .addAction("kms:ListResourceTags")
                        .addAction("kms:PutKeyPolicy")
                        .addAction("kms:RotateKeyOnDemand")
                        .addAction("kms:ScheduleKeyDeletion")
                        .addResource(IamResource.ALL)
                        .build())
                .addStatement(IamStatement.builder()
                        .sid(POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER)
                        .effect(IamEffect.ALLOW)
                        .addPrincipals(IamPrincipalType.AWS,
                                awsIamService.getEffectivePrincipals(awsClient.createAmazonIdentityManagement(awsCredentialView), cryptographicPrincipals))
                        .addAction("kms:Decrypt")
                        .addAction("kms:Encrypt")
                        .addAction("kms:GenerateDataKey")
                        .addResource(IamResource.ALL)
                        .build())
                .build();
        String policyJson = iamPolicy.toJson();
        LOGGER.debug("Generated initial key policy: \n{}", policyJson);
        return policyJson;
    }

    private Collection<Tag> getTagsForCreate(Map<String, String> requestTags, String tagValueClouderaKMSKeyTarget) {
        Map<String, String> effectiveTagsMap = new HashMap<>(requestTags);
        String oldValue = effectiveTagsMap.put(TAG_KEY_CLOUDERA_KMS_KEY_TARGET, tagValueClouderaKMSKeyTarget);
        if (oldValue == null) {
            LOGGER.info("Adding new tag \"{}\"=\"{}\"", TAG_KEY_CLOUDERA_KMS_KEY_TARGET, tagValueClouderaKMSKeyTarget);
        } else {
            LOGGER.warn("Overwriting tag \"{}\". Old value: \"{}\". New value: \"{}\"", TAG_KEY_CLOUDERA_KMS_KEY_TARGET, oldValue,
                    tagValueClouderaKMSKeyTarget);
        }
        return awsTaggingService.prepareKmsTags(effectiveTagsMap);
    }

    private void createKeyCloudResource(CloudContext cloudContext, KeyMetadata keyMetadata, String tagValueClouderaKMSKeyTarget) {
        CloudResource keyCloudResource = CloudResource.builder()
                .withName(tagValueClouderaKMSKeyTarget)
                .withType(AWS_KMS_KEY)
                .withReference(keyMetadata.arn())
                .withStatus(CommonStatus.CREATED)
                .build();
        persistenceNotifier.notifyAllocation(keyCloudResource, cloudContext);
    }

    private CloudEncryptionKey toCloudEncryptionKey(KeyMetadata keyMetadata, String tagValueClouderaKMSKeyTarget) {
        return new CloudEncryptionKey(
                keyMetadata.arn(),
                keyMetadata.keyId(),
                keyMetadata.description(),
                tagValueClouderaKMSKeyTarget,
                amazonKmsUtil.extractKeyMetadataMap(keyMetadata));
    }

    @Override
    public void updateEncryptionKeyResourceAccess(UpdateEncryptionKeyResourceAccessRequest request) {
        validateUpdateEncryptionKeyResourceAccessRequest(request);
        String keyArn = request.cloudResource().getReference();
        if (isPolicyUpdateNeeded(request)) {
            AwsCredentialView awsCredentialView = new AwsCredentialView(request.cloudCredential());
            AmazonKmsClient kmsClient = awsClient.createAWSKMS(awsCredentialView, request.cloudContext().getLocation().getRegion().value());
            String keyPolicyJson = getKeyPolicyJsonByKeyId(kmsClient, keyArn);
            LOGGER.debug("Current resource policy for KMS key [{}]: {}", keyArn, keyPolicyJson);
            keyPolicyJson = updateKeyPolicyJson(keyPolicyJson, keyArn, awsCredentialView, request);
            LOGGER.debug("Updating the resource policy for KMS key [{}] to the following: {}", keyArn, keyPolicyJson);
            putKeyPolicyJsonByKeyId(kmsClient, keyArn, keyPolicyJson);
        } else {
            LOGGER.warn("Received an empty UpdateEncryptionKeyResourceAccessRequest, no resource policy update is needed for KMS key [{}].", keyArn);
        }
    }

    private void validateUpdateEncryptionKeyResourceAccessRequest(UpdateEncryptionKeyResourceAccessRequest request) {
        throwIfNull(request, () -> new IllegalArgumentException("request must not be null!"));
        throwIfNull(request.cloudContext(), () -> new IllegalArgumentException("request.cloudContext must not be null!"));
        throwIfNull(request.cloudCredential(), () -> new IllegalArgumentException("request.cloudCredential must not be null!"));
        validateRequestCloudResource(request.cloudResource(), AWS_KMS_KEY);
        validatePrincipals(request.administratorPrincipalsToAdd(), "request.administratorPrincipalsToAdd", false);
        validatePrincipals(request.administratorPrincipalsToRemove(), "request.administratorPrincipalsToRemove", false);
        validatePrincipals(request.cryptographicPrincipalsToAdd(), "request.cryptographicPrincipalsToAdd", false);
        validatePrincipals(request.cryptographicPrincipalsToRemove(), "request.cryptographicPrincipalsToRemove", false);
        validateAuthorizedClients(request.cryptographicAuthorizedClientsToAdd(), "request.cryptographicAuthorizedClientsToAdd");
        validateAuthorizedClients(request.cryptographicAuthorizedClientsToRemove(), "request.cryptographicAuthorizedClientsToRemove");
    }

    private void validateAuthorizedClients(List<String> authorizedClients, String propertyName) {
        List<String> badAuthorizedClients = Optional.ofNullable(authorizedClients).orElse(List.of()).stream()
                .filter(resourceArn -> !isEc2InstanceOrSecretsManagerSecretArn(resourceArn))
                .toList();
        if (!badAuthorizedClients.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("The following elements of %s are malformed. Only EC2 instance and SecretsManager secret resource ARNs are supported. %s",
                            propertyName, badAuthorizedClients));
        }
    }

    private boolean isEc2InstanceOrSecretsManagerSecretArn(String resourceArn) {
        return arnService.isEc2InstanceArn(resourceArn) || arnService.isSecretsManagerSecretArn(resourceArn);
    }

    private boolean isPolicyUpdateNeeded(UpdateEncryptionKeyResourceAccessRequest request) {
        return !(isEmpty(request.administratorPrincipalsToAdd()) && isEmpty(request.administratorPrincipalsToRemove())
                && isEmpty(request.cryptographicPrincipalsToAdd()) && isEmpty(request.cryptographicPrincipalsToRemove())
                && isEmpty(request.cryptographicAuthorizedClientsToAdd()) && isEmpty(request.cryptographicAuthorizedClientsToRemove()));
    }

    private String getKeyPolicyJsonByKeyId(AmazonKmsClient kmsClient, String keyArn) {
        GetKeyPolicyRequest getKeyPolicyRequest = GetKeyPolicyRequest.builder()
                .keyId(keyArn)
                .policyName(POLICY_NAME)
                .build();
        return kmsClient.getKeyPolicy(getKeyPolicyRequest).policy();
    }

    private String updateKeyPolicyJson(String keyPolicyJsonCurrent, String keyArn, AwsCredentialView awsCredentialView,
            UpdateEncryptionKeyResourceAccessRequest request) {
        IamPolicy iamPolicyCurrent = IamPolicy.fromJson(keyPolicyJsonCurrent);
        List<IamStatement> statements = new ArrayList<>(iamPolicyCurrent.statements());
        IamPolicy.Builder iamPolicyBuilder = iamPolicyCurrent.toBuilder();
        AmazonIdentityManagementClient iamClient = awsClient.createAmazonIdentityManagement(awsCredentialView);
        updateKeyAdministratorStatement(keyArn, request.administratorPrincipalsToAdd(), request.administratorPrincipalsToRemove(), statements, iamClient);
        updateKeyCryptographicUserStatement(keyArn, request.cryptographicPrincipalsToAdd(), request.cryptographicPrincipalsToRemove(),
                request.cryptographicAuthorizedClientsToAdd(), request.cryptographicAuthorizedClientsToRemove(), statements, iamClient);
        iamPolicyBuilder.statements(statements);
        return iamPolicyBuilder.build().toJson();
    }

    private void updateKeyAdministratorStatement(String keyArn, List<String> administratorPrincipalsToAdd, List<String> administratorPrincipalsToRemove,
            List<IamStatement> statements, AmazonIdentityManagementClient iamClient) {
        updateStatementPrincipals(keyArn, administratorPrincipalsToAdd, administratorPrincipalsToRemove, statements, iamClient,
                POLICY_STATEMENT_ID_KEY_ADMINISTRATOR);
    }

    private void updateStatementPrincipals(String keyArn, List<String> principalsToAdd, List<String> principalsToRemove, List<IamStatement> statements,
            AmazonIdentityManagementClient iamClient, String policyStatementId) {
        if (!(isEmpty(principalsToAdd) && isEmpty(principalsToRemove))) {
            IamStatement iamStatementCurrent = findIamStatementByStatementId(keyArn, statements, policyStatementId);
            List<IamPrincipal> principals = new ArrayList<>(iamStatementCurrent.principals());
            IamStatement.Builder iamStatementBuilder = iamStatementCurrent.toBuilder();
            principalsToRemove.forEach(principal -> {
                String effectivePrincipal = awsIamService.getEffectivePrincipal(iamClient, principal);
                removePrincipalByPrincipalId(principals, effectivePrincipal);
            });
            iamStatementBuilder.principals(principals);
            principalsToAdd.forEach(principal -> {
                String effectivePrincipal = awsIamService.getEffectivePrincipal(iamClient, principal);
                iamStatementBuilder.addPrincipal(IamPrincipalType.AWS, effectivePrincipal);
            });
            replaceIamStatement(statements, iamStatementBuilder.build());
        }
    }

    private IamStatement findIamStatementByStatementId(String keyArn, List<IamStatement> statements, String policyStatementId) {
        return statements.stream()
                .filter(st -> policyStatementId.equals(st.sid()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Cannot find resource policy statement with ID [%s] for KMS key [%s]", policyStatementId, keyArn)));
    }

    private void removePrincipalByPrincipalId(List<IamPrincipal> principals, String principalId) {
        for (int i = 0; i < principals.size(); i++) {
            if (principalId.equals(principals.get(i).id())) {
                principals.remove(i);
                break;
            }
        }
    }

    private void replaceIamStatement(List<IamStatement> statements, IamStatement iamStatement) {
        String policyStatementId = iamStatement.sid();
        for (int i = 0; i < statements.size(); i++) {
            if (policyStatementId.equals(statements.get(i).sid())) {
                statements.set(i, iamStatement);
            }
        }
    }

    private void updateKeyCryptographicUserStatement(String keyArn, List<String> cryptographicPrincipalsToAdd, List<String> cryptographicPrincipalsToRemove,
            List<String> cryptographicAuthorizedClientsToAdd, List<String> cryptographicAuthorizedClientsToRemove, List<IamStatement> statements,
            AmazonIdentityManagementClient iamClient) {
        updateStatementPrincipals(keyArn, cryptographicPrincipalsToAdd, cryptographicPrincipalsToRemove, statements, iamClient,
                POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER);

        if (!(isEmpty(cryptographicAuthorizedClientsToAdd) && isEmpty(cryptographicAuthorizedClientsToRemove))) {
            String policyStatementId = POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER;
            IamStatement iamStatementCurrent = findIamStatementByStatementId(keyArn, statements, policyStatementId);
            List<IamCondition> conditions = new ArrayList<>(iamStatementCurrent.conditions());
            IamStatement.Builder iamStatementBuilder = iamStatementCurrent.toBuilder();
            cryptographicAuthorizedClientsToRemove.forEach(authorizedClient ->
                    conditions.remove(IamCondition.create(IamConditionOperator.ARN_EQUALS, authorizedClientToConditionKey(authorizedClient), authorizedClient)));
            iamStatementBuilder.conditions(conditions);
            cryptographicAuthorizedClientsToAdd.forEach(authorizedClient ->
                    iamStatementBuilder.addCondition(IamConditionOperator.ARN_EQUALS, authorizedClientToConditionKey(authorizedClient), authorizedClient));
            replaceIamStatement(statements, iamStatementBuilder.build());
        }
    }

    private IamConditionKey authorizedClientToConditionKey(String authorizedClient) {
        IamConditionKey key;
        if (arnService.isEc2InstanceArn(authorizedClient)) {
            key = POLICY_CONDITION_KEY_EC2_SOURCE_INSTANCE_ARN;
        } else if (arnService.isSecretsManagerSecretArn(authorizedClient)) {
            key = POLICY_CONDITION_KEY_KMS_ENCRYPTION_CONTEXT_SECRET_ARN;
        } else {
            throw new IllegalStateException(String.format("Encountered an unsupported kind of authorizedClient: [%s]", authorizedClient));
        }
        return key;
    }

    private void putKeyPolicyJsonByKeyId(AmazonKmsClient kmsClient, String keyArn, String keyPolicyJson) {
        PutKeyPolicyRequest putKeyPolicyRequest = PutKeyPolicyRequest.builder()
                .keyId(keyArn)
                .policyName(POLICY_NAME)
                .policy(keyPolicyJson)
                .build();
        kmsClient.putKeyPolicy(putKeyPolicyRequest);
    }

    @Override
    public void enableAutoRotationForEncryptionKey(EncryptionKeyEnableAutoRotationRequest request) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.cloudCredential());
        AmazonKmsClient kmsClient = awsClient.createAWSKMS(awsCredentialView, request.cloudContext().getLocation().getRegion().value());
        for (CloudResource cloudResource : request.cloudResources()) {
            String keyArn = cloudResource.getReference();
            LOGGER.info("Enabling auto rotation for KMS key [{}]", keyArn);
            kmsClient.enableKeyRotation(EnableKeyRotationRequest.builder()
                    .keyId(keyArn)
                    .rotationPeriodInDays(request.rotationPeriodInDays())
                    .build());
        }
    }

    @Override
    public void rotateEncryptionKey(EncryptionKeyRotationRequest request) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(request.cloudCredential());
        AmazonKmsClient kmsClient = awsClient.createAWSKMS(awsCredentialView, request.cloudContext().getLocation().getRegion().value());
        for (CloudResource cloudResource : request.cloudResources()) {
            String keyArn = cloudResource.getReference();
            LOGGER.info("Rotating KMS key [{}]", keyArn);
            kmsClient.rotateKeyOnDemand(RotateKeyOnDemandRequest.builder()
                    .keyId(keyArn)
                    .build());
        }
    }
}
