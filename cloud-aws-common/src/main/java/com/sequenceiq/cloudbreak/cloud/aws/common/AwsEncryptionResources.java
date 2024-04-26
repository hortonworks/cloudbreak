package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.common.api.type.ResourceType.AWS_KMS_KEY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AmazonKmsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;

import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.policybuilder.iam.IamResource;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.OriginType;
import software.amazon.awssdk.services.kms.model.Tag;

@Service
public class AwsEncryptionResources implements EncryptionResources {

    static final String TAG_KEY_CLOUDERA_KMS_KEY_TARGET = "Cloudera-KMS-Key-Target";

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEncryptionResources.class);

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AmazonKmsUtil amazonKmsUtil;

    @Inject
    private AwsIamService awsIamService;

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
    public CloudEncryptionKey createEncryptionKey(EncryptionKeyCreationRequest encryptionKeyCreationRequest) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(encryptionKeyCreationRequest.cloudCredential());
        CloudContext cloudContext = encryptionKeyCreationRequest.cloudContext();
        AmazonKmsClient kmsClient = awsClient.createAWSKMS(awsCredentialView, cloudContext.getLocation().getRegion().value());

        String tagValueClouderaKMSKeyTarget = encryptionKeyCreationRequest.keyName();
        LOGGER.info("Checking if AWS_KMS_KEY CloudResource with name \"{}\" exists", tagValueClouderaKMSKeyTarget);
        Optional<CloudResource> keyCloudResourceOptional = getKeyCloudResourceByTarget(encryptionKeyCreationRequest.cloudResources(),
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
                keyMetadata = createKey(encryptionKeyCreationRequest, awsCredentialView, kmsClient);
            }
            createKeyCloudResource(cloudContext, keyMetadata, tagValueClouderaKMSKeyTarget);
        }

        return toCloudEncryptionKey(keyMetadata, tagValueClouderaKMSKeyTarget);
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

    private KeyMetadata createKey(EncryptionKeyCreationRequest encryptionKeyCreationRequest, AwsCredentialView awsCredentialView, AmazonKmsClient kmsClient) {
        CreateKeyRequest createKeyRequest = CreateKeyRequest.builder()
                .keySpec(KeySpec.SYMMETRIC_DEFAULT)
                .keyUsage(KeyUsageType.ENCRYPT_DECRYPT)
                .description(encryptionKeyCreationRequest.description())
                .origin(OriginType.AWS_KMS)
                .policy(getKeyPolicy(awsCredentialView, encryptionKeyCreationRequest.targetPrincipalIds()))
                .tags(getTagsForCreate(encryptionKeyCreationRequest.tags(), encryptionKeyCreationRequest.keyName()))
                .build();

        return kmsClient.createKey(createKeyRequest).keyMetadata();
    }

    private String getKeyPolicy(AwsCredentialView awsCredentialView, List<String> targetPrincipalIds) {
        if (CollectionUtils.isEmpty(targetPrincipalIds)) {
            throw new IllegalArgumentException("targetPrincipalIds must not be null or empty");
        }
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
                        .sid("Allow access for the CDP credential IAM role as a key administrator")
                        .effect(IamEffect.ALLOW)
                        .addPrincipal(IamPrincipalType.AWS, credentialRoleArn)
                        .addAction("kms:GetKeyPolicy")
                        .addAction("kms:PutKeyPolicy")
                        .addResource(IamResource.ALL)
                        .build())
                .addStatement(IamStatement.builder()
                        .sid("Allow use of the key by CDP clusters")
                        .effect(IamEffect.ALLOW)
                        .addPrincipals(IamPrincipalType.AWS,
                                awsIamService.getEffectivePrincipals(awsClient.createAmazonIdentityManagement(awsCredentialView), targetPrincipalIds))
                        .addAction("kms:Decrypt")
                        .addAction("kms:Encrypt")
                        .addAction("kms:GenerateDataKey")
                        .addResource(IamResource.ALL)
                        .build())
                .build();
        String policyJson = iamPolicy.toJson();
        LOGGER.info("Generated key policy: \n{}", policyJson);
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

}
