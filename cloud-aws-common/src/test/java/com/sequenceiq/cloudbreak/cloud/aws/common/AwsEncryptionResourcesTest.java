package com.sequenceiq.cloudbreak.cloud.aws.common;


import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources.POLICY_NAME;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources.POLICY_STATEMENT_ID_KEY_ADMINISTRATOR;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources.POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources.TAG_KEY_CLOUDERA_KMS_KEY_TARGET;
import static com.sequenceiq.common.api.type.ResourceType.AWS_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.AWS_KMS_KEY;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AmazonKmsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.UpdateEncryptionKeyResourceAccessRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;

import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.OriginType;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.Tag;

@ExtendWith(MockitoExtension.class)
class AwsEncryptionResourcesTest {

    private static final String REGION = "region";

    private static final String KEY_NAME = "keyName";

    private static final String KEY_NAME_DUMMY = "keyNameDummy";

    private static final String TAG_KEY = "tagKey";

    private static final String TAG_VALUE = "tagValue";

    private static final String DESCRIPTION = "description";

    private static final String KMS_KEY_ID = UUID.randomUUID().toString();

    private static final String KMS_KEY_ID_DUMMY = "dummy";

    private static final String ARN_KMS_KEY_PREFIX = "arn:aws-us-gov:kms:us-gov-west-1:123456789012:key/";

    private static final String ARN_KMS_KEY = ARN_KMS_KEY_PREFIX + KMS_KEY_ID;

    private static final String ARN_KMS_KEY_DUMMY = ARN_KMS_KEY_PREFIX + KMS_KEY_ID_DUMMY;

    private static final String ARN_INSTANCE_PROFILE = "arn:aws-us-gov:iam::123456789012:instance-profile/my-profile";

    private static final String ARN_INSTANCE_PROFILE_3 = "arn:aws-us-gov:iam::123456789012:instance-profile/my-profile-3";

    private static final String ARN_ROOT = "arn:aws-us-gov:iam::123456789012:root";

    private static final String ARN_CREDENTIAL_ROLE = "arn:aws-us-gov:iam::123456789012:role/my-credential-role";

    private static final String ARN_CREDENTIAL_ROLE_3 = "arn:aws-us-gov:iam::123456789012:role/my-credential-role-3";

    private static final String ARN_ROLE = "arn:aws-us-gov:iam::123456789012:role/my-role";

    private static final String ARN_ROLE_3 = "arn:aws-us-gov:iam::123456789012:role/my-role-3";

    private static final String ARN_EC2_INSTANCE = "arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-0bc43096314295350";

    private static final String ARN_EC2_INSTANCE_3 = "arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-1ec6923af50a86b21";

    private static final String ARN_SECRETSMANAGER_SECRET = "arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-rk4Dy0";

    private static final String ARN_SECRETSMANAGER_SECRET_3 = "arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-3-8Lwc1u";

    private static final String POLICY_JSON_TO_UPDATE =
            "{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[" +
                    "{\"Sid\":\"Enable IAM user permissions\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"}," +
                    "\"Action\":\"kms:*\",\"Resource\":\"*\"},{\"Sid\":\"Allow access for the CDP credential IAM role as a key administrator\"," +
                    "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"arn:aws-us-gov:iam::123456789012:role/my-credential-role\"," +
                    "\"arn:aws-us-gov:iam::123456789012:role/my-credential-role-2\"]},\"Action\":[\"kms:GetKeyPolicy\",\"kms:PutKeyPolicy\"," +
                    "\"kms:ScheduleKeyDeletion\"]," +
                    "\"Resource\":\"*\"},{\"Sid\":\"Allow use of the key by CDP clusters for cryptographic operations\",\"Effect\":\"Allow\"," +
                    "\"Principal\":{\"AWS\":[\"arn:aws-us-gov:iam::123456789012:role/my-role\",\"arn:aws-us-gov:iam::123456789012:role/my-role-2\"]}," +
                    "\"Action\":[\"kms:Decrypt\",\"kms:Encrypt\",\"kms:GenerateDataKey\"],\"Resource\":\"*\",\"Condition\":{\"ArnEquals\":{" +
                    "\"ec2:SourceInstanceArn\":[\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-0bc43096314295350\"," +
                    "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-e842a720f901b547d\"]," +
                    "\"kms:EncryptionContext:SecretARN\":[\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-rk4Dy0\"," +
                    "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-2-Pi92sB\"]}}}]}";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AmazonKmsUtil amazonKmsUtil;

    @Mock
    private AwsIamService awsIamService;

    @Mock
    private ArnService arnService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @InjectMocks
    private AwsEncryptionResources underTest;

    private CloudCredential cloudCredential;

    private CloudContext cloudContext;

    private EncryptionKeyCreationRequest encryptionKeyCreationRequest;

    @Mock
    private AmazonKmsClient kmsClient;

    private Map<String, Object> metadataMap;

    @Captor
    private ArgumentCaptor<ListResourceTagsRequest> listResourceTagsRequestCaptor;

    @Captor
    private ArgumentCaptor<CloudResource> cloudResourceCaptor;

    @Mock
    private ResourcePersisted resourcePersisted;

    @Captor
    private ArgumentCaptor<CreateKeyRequest> createKeyRequestCaptor;

    @Mock
    private AmazonIdentityManagementClient amazonIdentityManagementClient;

    @Captor
    private ArgumentCaptor<GetKeyPolicyRequest> getKeyPolicyRequestCaptor;

    @Captor
    private ArgumentCaptor<PutKeyPolicyRequest> putKeyPolicyRequestCaptor;

    @BeforeEach
    void setUp() {
        cloudCredential = new CloudCredential("credentialId", "credentialName", Map.ofEntries(entry("roleArn", ARN_CREDENTIAL_ROLE)), "credentialAccount");
        cloudContext = CloudContext.Builder.builder()
                .withLocation(Location.location(Region.region(REGION)))
                .build();

        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestDefault();

        metadataMap = Map.ofEntries(entry("metaKey", "metaValue"));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestDefault() {
        return createEncryptionKeyCreationRequest(KEY_NAME, cloudContext, cloudCredential, Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null,
                List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithKeyName(String keyName) {
        return createEncryptionKeyCreationRequest(keyName, cloudContext, cloudCredential, Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null,
                List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithNullCloudContext() {
        return createEncryptionKeyCreationRequest(KEY_NAME, null, cloudCredential, Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null,
                List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithNullCloudCredential() {
        return createEncryptionKeyCreationRequest(KEY_NAME, cloudContext, null, Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null,
                List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithCloudResources(List<CloudResource> cloudResources) {
        return createEncryptionKeyCreationRequest(KEY_NAME, cloudContext, cloudCredential, Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), cloudResources,
                List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithTags(Map<String, String> tags) {
        return createEncryptionKeyCreationRequest(KEY_NAME, cloudContext, cloudCredential, tags, null, List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithCryptographicPrincipals(List<String> cryptographicPrincipals) {
        return createEncryptionKeyCreationRequest(KEY_NAME, cloudContext, cloudCredential, Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null,
                cryptographicPrincipals);
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequest(String keyName, CloudContext cloudContext, CloudCredential cloudCredential,
            Map<String, String> tags, List<CloudResource> cloudResources, List<String> cryptographicPrincipals) {
        return EncryptionKeyCreationRequest.builder()
                .withKeyName(keyName)
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withTags(tags)
                .withDescription(DESCRIPTION)
                .withCloudResources(cloudResources)
                .withCryptographicPrincipals(cryptographicPrincipals)
                .build();
    }

    private CloudResource createKeyCloudResource() {
        return CloudResource.builder()
                .withName(KEY_NAME)
                .withType(AWS_KMS_KEY)
                .withReference(ARN_KMS_KEY)
                .build();
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestWithNullCloudContext() {
        return createUpdateEncryptionKeyResourceAccessRequest(null, cloudCredential, createKeyCloudResource(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestWithNullCloudCredential() {
        return createUpdateEncryptionKeyResourceAccessRequest(cloudContext, null, createKeyCloudResource(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestWithCloudResource(CloudResource cloudResource) {
        return createUpdateEncryptionKeyResourceAccessRequest(cloudContext, cloudCredential, cloudResource, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestEmpty() {
        return createUpdateEncryptionKeyResourceAccessRequest(cloudContext, cloudCredential, createKeyCloudResource(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(List<String> administratorPrincipalsToAdd,
            List<String> cryptographicPrincipalsToAdd, List<String> cryptographicAuthorizedClientsToAdd) {
        return createUpdateEncryptionKeyResourceAccessRequest(cloudContext, cloudCredential, createKeyCloudResource(), administratorPrincipalsToAdd, List.of(),
                cryptographicPrincipalsToAdd, List.of(), cryptographicAuthorizedClientsToAdd, List.of());
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestWithRemoveOnly(List<String> administratorPrincipalsToRemove,
            List<String> cryptographicPrincipalsToRemove, List<String> cryptographicAuthorizedClientsToRemove) {
        return createUpdateEncryptionKeyResourceAccessRequest(cloudContext, cloudCredential, createKeyCloudResource(), List.of(),
                administratorPrincipalsToRemove, List.of(), cryptographicPrincipalsToRemove, List.of(), cryptographicAuthorizedClientsToRemove);
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequestWithAddAndRemove(List<String> administratorPrincipalsToAdd,
            List<String> administratorPrincipalsToRemove, List<String> cryptographicPrincipalsToAdd, List<String> cryptographicPrincipalsToRemove,
            List<String> cryptographicAuthorizedClientsToAdd, List<String> cryptographicAuthorizedClientsToRemove) {
        return createUpdateEncryptionKeyResourceAccessRequest(cloudContext, cloudCredential, createKeyCloudResource(), administratorPrincipalsToAdd,
                administratorPrincipalsToRemove, cryptographicPrincipalsToAdd, cryptographicPrincipalsToRemove, cryptographicAuthorizedClientsToAdd,
                cryptographicAuthorizedClientsToRemove);
    }

    private UpdateEncryptionKeyResourceAccessRequest createUpdateEncryptionKeyResourceAccessRequest(CloudContext cloudContext, CloudCredential cloudCredential,
            CloudResource cloudResource, List<String> administratorPrincipalsToAdd, List<String> administratorPrincipalsToRemove,
            List<String> cryptographicPrincipalsToAdd, List<String> cryptographicPrincipalsToRemove, List<String> cryptographicAuthorizedClientsToAdd,
            List<String> cryptographicAuthorizedClientsToRemove) {
        return UpdateEncryptionKeyResourceAccessRequest.builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResource(cloudResource)
                .withAdministratorPrincipalsToAdd(administratorPrincipalsToAdd)
                .withAdministratorPrincipalsToRemove(administratorPrincipalsToRemove)
                .withCryptographicPrincipalsToAdd(cryptographicPrincipalsToAdd)
                .withCryptographicPrincipalsToRemove(cryptographicPrincipalsToRemove)
                .withCryptographicAuthorizedClientsToAdd(cryptographicAuthorizedClientsToAdd)
                .withCryptographicAuthorizedClientsToRemove(cryptographicAuthorizedClientsToRemove)
                .build();
    }

    @Test
    void platformTest() {
        assertThat(underTest.platform()).isEqualTo(AwsConstants.AWS_PLATFORM);
    }

    @Test
    void variantTest() {
        assertThat(underTest.variant()).isEqualTo(AwsConstants.AWS_DEFAULT_VARIANT);
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesGivenAndFoundSingleKms() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        CloudResource kmsCloudResource = createKeyCloudResource();
        List<CloudResource> cloudResourcesInRequest = List.of(kmsCloudResource);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCloudResources(cloudResourcesInRequest);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);
        when(cloudResourceHelper.getResourceTypeInstancesFromList(AWS_KMS_KEY, cloudResourcesInRequest)).thenReturn(List.of(kmsCloudResource));

        setupKeyMetadataForExistingKey();

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verify(amazonKmsUtil, never()).listKeysWithAllPages(kmsClient);
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    private void setupKeyMetadataForExistingKey() {
        KeyMetadata keyMetadata = KeyMetadata.builder()
                .arn(ARN_KMS_KEY)
                .keyId(KMS_KEY_ID)
                .description(DESCRIPTION)
                .build();
        when(amazonKmsUtil.getKeyMetadataByKeyId(kmsClient, ARN_KMS_KEY)).thenReturn(keyMetadata);
        when(amazonKmsUtil.extractKeyMetadataMap(keyMetadata)).thenReturn(metadataMap);
    }

    private void verifyCloudEncryptionKey(CloudEncryptionKey cloudEncryptionKey, Map<String, Object> metadataMap) {
        assertThat(cloudEncryptionKey).isNotNull();
        assertThat(cloudEncryptionKey.getName()).isEqualTo(ARN_KMS_KEY);
        assertThat(cloudEncryptionKey.getId()).isEqualTo(KMS_KEY_ID);
        assertThat(cloudEncryptionKey.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(cloudEncryptionKey.getDisplayName()).isEqualTo(KEY_NAME);
        assertThat(cloudEncryptionKey.getProperties()).isSameAs(metadataMap);
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesGivenAndFoundMultipleKms() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        CloudResource kmsCloudResource = createKeyCloudResource();
        CloudResource kmsCloudResourceDummy = createKeyCloudResourceDummy();
        List<CloudResource> cloudResourcesInRequest = List.of(kmsCloudResourceDummy, kmsCloudResource);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCloudResources(cloudResourcesInRequest);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);
        when(cloudResourceHelper.getResourceTypeInstancesFromList(AWS_KMS_KEY, cloudResourcesInRequest))
                .thenReturn(List.of(kmsCloudResourceDummy, kmsCloudResource));

        setupKeyMetadataForExistingKey();

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verify(amazonKmsUtil, never()).listKeysWithAllPages(kmsClient);
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    private CloudResource createKeyCloudResourceDummy() {
        return CloudResource.builder()
                .withName(KEY_NAME_DUMMY)
                .withType(AWS_KMS_KEY)
                .withReference(ARN_KMS_KEY_DUMMY)
                .build();
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndExistingKmsKey() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        setupKeyListingForExistingKey(ARN_KMS_KEY, KMS_KEY_ID, KEY_NAME);
        setupKeyMetadataForExistingKey();

        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(resourcePersisted);

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verifyListResourceTagsRequest(ARN_KMS_KEY);
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verifyKeyCloudResourcePersistence();
    }

    private void setupKeyListingForExistingKey(String keyArn, String keyId, String tagValueClouderaKMSKeyTarget) {
        KeyListEntry keyListEntry = KeyListEntry.builder()
                .keyArn(keyArn)
                .keyId(keyId)
                .build();
        when(amazonKmsUtil.listKeysWithAllPages(kmsClient)).thenReturn(List.of(keyListEntry));
        Tag tag = Tag.builder()
                .tagKey(TAG_KEY_CLOUDERA_KMS_KEY_TARGET)
                .tagValue(tagValueClouderaKMSKeyTarget)
                .build();
        when(amazonKmsUtil.listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class))).thenReturn(List.of(tag));
    }

    private void setupKeyListingForExistingKeyFailure(String keyArn, String keyId) {
        KeyListEntry keyListEntry = KeyListEntry.builder()
                .keyArn(keyArn)
                .keyId(keyId)
                .build();
        when(amazonKmsUtil.listKeysWithAllPages(kmsClient)).thenReturn(List.of(keyListEntry));
        doThrow(new RuntimeException("Problem")).when(amazonKmsUtil).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
    }

    private void verifyListResourceTagsRequest(String keyArnExpected) {
        verify(amazonKmsUtil).listResourceTagsWithAllPages(eq(kmsClient), listResourceTagsRequestCaptor.capture());
        assertThat(listResourceTagsRequestCaptor.getValue().keyId()).isEqualTo(keyArnExpected);
    }

    private void verifyKeyCloudResourcePersistence() {
        verify(persistenceNotifier).notifyAllocation(cloudResourceCaptor.capture(), eq(cloudContext));
        CloudResource keyCloudResource = cloudResourceCaptor.getValue();
        assertThat(keyCloudResource.getName()).isEqualTo(KEY_NAME);
        assertThat(keyCloudResource.getType()).isEqualTo(AWS_KMS_KEY);
        assertThat(keyCloudResource.getReference()).isEqualTo(ARN_KMS_KEY);
        assertThat(keyCloudResource.getStatus()).isEqualTo(CommonStatus.CREATED);
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesGivenAndNotFoundAndExistingKmsKey() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        CloudResource kmsCloudResourceDummy = createKeyCloudResourceDummy();
        List<CloudResource> cloudResourcesInRequest = List.of(kmsCloudResourceDummy);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCloudResources(cloudResourcesInRequest);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);
        when(cloudResourceHelper.getResourceTypeInstancesFromList(AWS_KMS_KEY, cloudResourcesInRequest))
                .thenReturn(List.of(kmsCloudResourceDummy));

        setupKeyListingForExistingKey(ARN_KMS_KEY, KMS_KEY_ID, KEY_NAME);
        setupKeyMetadataForExistingKey();

        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(resourcePersisted);

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verifyListResourceTagsRequest(ARN_KMS_KEY);
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verifyKeyCloudResourcePersistence();
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndNoKmsKeysAtAllAndCreateNewKmsKey() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        setupPrincipalMapping();
        setupTagMapping();

        setupKeyMetadataForNewKey();

        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(resourcePersisted);

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verifyCreateKeyRequest();
        verifyKeyCloudResourcePersistence();
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndFetchingKmsKeysFailAndCreateNewKmsKey() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        setupKeyListingForExistingKeyFailure(ARN_KMS_KEY, KMS_KEY_ID);

        setupPrincipalMapping();
        setupTagMapping();

        setupKeyMetadataForNewKey();

        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(resourcePersisted);

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verify(amazonKmsUtil).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verifyCreateKeyRequest();
        verifyKeyCloudResourcePersistence();
    }

    private void setupPrincipalMapping() {
        when(awsIamService.getAccountRootArn(ARN_CREDENTIAL_ROLE)).thenReturn(ARN_ROOT);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);
        when(awsIamService.getEffectivePrincipals(amazonIdentityManagementClient, encryptionKeyCreationRequest.cryptographicPrincipals()))
                .thenReturn(List.of(ARN_ROLE));
    }

    private void setupTagMapping() {
        Map<String, String> effectiveTagsMap = Map.ofEntries(entry(TAG_KEY, TAG_VALUE), entry(TAG_KEY_CLOUDERA_KMS_KEY_TARGET, KEY_NAME));
        Tag tagFoo = Tag.builder()
                .tagKey(TAG_KEY)
                .tagValue(TAG_VALUE)
                .build();
        Tag tagTarget = Tag.builder()
                .tagKey(TAG_KEY_CLOUDERA_KMS_KEY_TARGET)
                .tagValue(KEY_NAME)
                .build();
        when(awsTaggingService.prepareKmsTags(effectiveTagsMap)).thenReturn(List.of(tagFoo, tagTarget));
    }

    private void setupKeyMetadataForNewKey() {
        KeyMetadata keyMetadata = KeyMetadata.builder()
                .arn(ARN_KMS_KEY)
                .keyId(KMS_KEY_ID)
                .description(DESCRIPTION)
                .build();
        CreateKeyResponse createKeyResponse = CreateKeyResponse.builder()
                .keyMetadata(keyMetadata)
                .build();
        when(kmsClient.createKey(any(CreateKeyRequest.class))).thenReturn(createKeyResponse);
        when(amazonKmsUtil.extractKeyMetadataMap(keyMetadata)).thenReturn(metadataMap);
    }

    private void verifyCreateKeyRequest() {
        verify(kmsClient).createKey(createKeyRequestCaptor.capture());
        CreateKeyRequest createKeyRequest = createKeyRequestCaptor.getValue();
        assertThat(createKeyRequest).isNotNull();
        assertThat(createKeyRequest.keySpec()).isEqualTo(KeySpec.SYMMETRIC_DEFAULT);
        assertThat(createKeyRequest.keyUsage()).isEqualTo(KeyUsageType.ENCRYPT_DECRYPT);
        assertThat(createKeyRequest.description()).isEqualTo(DESCRIPTION);
        assertThat(createKeyRequest.origin()).isEqualTo(OriginType.AWS_KMS);
        assertThat(createKeyRequest.policy()).isEqualTo(
                "{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\"," +
                        "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow access for the CDP credential IAM role as a key administrator\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:role/my-credential-role\"}," +
                        "\"Action\":[\"kms:GetKeyPolicy\",\"kms:PutKeyPolicy\",\"kms:ScheduleKeyDeletion\"],\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow use of the key by CDP clusters for cryptographic operations\"," +
                        "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:role/my-role\"}," +
                        "\"Action\":[\"kms:Decrypt\",\"kms:Encrypt\",\"kms:GenerateDataKey\"],\"Resource\":\"*\"}]}");

        List<Tag> resultTags = createKeyRequest.tags();
        assertThat(resultTags).isNotNull();
        assertThat(resultTags).hasSize(2);
        Map<String, String> resultTagsMap = resultTags.stream()
                .collect(Collectors.toMap(Tag::tagKey, Tag::tagValue));
        assertThat(resultTagsMap).isEqualTo(Map.ofEntries(entry(TAG_KEY, TAG_VALUE), entry(TAG_KEY_CLOUDERA_KMS_KEY_TARGET, KEY_NAME)));
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndOnlyMismatchingKmsKeysAndCreateNewKmsKey() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        setupKeyListingForExistingKey(ARN_KMS_KEY_DUMMY, KMS_KEY_ID_DUMMY, KEY_NAME_DUMMY);

        setupPrincipalMapping();
        setupTagMapping();

        setupKeyMetadataForNewKey();

        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(resourcePersisted);

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verifyListResourceTagsRequest(ARN_KMS_KEY_DUMMY);
        verify(amazonKmsUtil, never()).getKeyMetadataByKeyId(eq(kmsClient), anyString());
        verifyCreateKeyRequest();
        verifyKeyCloudResourcePersistence();
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndNoKmsKeysAtAllAndCreateNewKmsKeyAndTargetTagClash() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithTags(
                Map.ofEntries(entry(TAG_KEY, TAG_VALUE), entry(TAG_KEY_CLOUDERA_KMS_KEY_TARGET, KEY_NAME_DUMMY)));
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        setupPrincipalMapping();
        setupTagMapping();

        setupKeyMetadataForNewKey();

        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), eq(cloudContext))).thenReturn(resourcePersisted);

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verifyCreateKeyRequest();
        verifyKeyCloudResourcePersistence();
    }

    @Test
    void createEncryptionKeyTestWhenNullRequest() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> underTest.createEncryptionKey(null));

        assertThat(illegalArgumentException).hasMessage("request must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @ParameterizedTest(name = "keyName={0}")
    @NullSource
    @ValueSource(strings = {"", " "})
    void createEncryptionKeyTestWhenBlankKeyName(String keyName) {
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithKeyName(keyName);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage("request.keyName must not be null, empty or blank!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void createEncryptionKeyTestWhenNullCloudContext() {
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithNullCloudContext();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage("request.cloudContext must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void createEncryptionKeyTestWhenNullCloudCredential() {
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithNullCloudCredential();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage("request.cloudCredential must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void createEncryptionKeyTestWhenNullTags() {
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithTags(null);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage("request.tags must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    static Object[][] emptyCryptographicPrincipalsDataProvider() {
        return new Object[][]{
                // cryptographicPrincipals
                {null},
                {List.of()},
        };
    }

    @ParameterizedTest(name = "cryptographicPrincipals={0}")
    @MethodSource("emptyCryptographicPrincipalsDataProvider")
    void createEncryptionKeyTestWhenEmptyCryptographicPrincipals(List<String> cryptographicPrincipals) {
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCryptographicPrincipals(cryptographicPrincipals);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage("request.cryptographicPrincipals must not be null or empty!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void createEncryptionKeyTestWhenBadCryptographicPrincipals() {
        List<String> cryptographicPrincipals = List.of(ARN_ROOT);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCryptographicPrincipals(cryptographicPrincipals);
        when(arnService.isInstanceProfileArn(ARN_ROOT)).thenReturn(false);
        when(arnService.isRoleArn(ARN_ROOT)).thenReturn(false);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage(String.format("The following elements of request.cryptographicPrincipals are malformed. " +
                "Only IAM instance-profile and role resource ARNs are supported. %s", cryptographicPrincipals));
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenNullRequest() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(null));

        assertThat(illegalArgumentException).hasMessage("request must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenNullCloudContext() {
        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithNullCloudContext();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage("request.cloudContext must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenNullCloudCredential() {
        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest =
                createUpdateEncryptionKeyResourceAccessRequestWithNullCloudCredential();

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage("request.cloudCredential must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenNullCloudResource() {
        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest =
                createUpdateEncryptionKeyResourceAccessRequestWithCloudResource(null);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage("request.CloudResource must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenCloudResourceWithNullReference() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(KEY_NAME)
                .withType(AWS_KMS_KEY)
                .withReference(null)
                .build();

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest =
                createUpdateEncryptionKeyResourceAccessRequestWithCloudResource(cloudResource);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage("request.CloudResource.reference must not be null!");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenCloudResourceWithWrongResourceType() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(KEY_NAME)
                .withType(AWS_INSTANCE)
                .withReference(ARN_KMS_KEY)
                .build();

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest =
                createUpdateEncryptionKeyResourceAccessRequestWithCloudResource(cloudResource);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage("request.CloudResource has the wrong resource type! Expected: AWS_KMS_KEY, actual: AWS_INSTANCE");
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    static Object[][] updateEncryptionKeyResourceAccessTestWhenBadPrincipalsDataProvider() {
        return new Object[][]{
                // administratorPrincipalsToAdd, administratorPrincipalsToRemove, cryptographicPrincipalsToAdd, cryptographicPrincipalsToRemove,
                // propertyNameExpected
                {List.of(ARN_ROOT), List.of(), List.of(), List.of(), "request.administratorPrincipalsToAdd"},
                {List.of(), List.of(ARN_ROOT), List.of(), List.of(), "request.administratorPrincipalsToRemove"},
                {List.of(), List.of(), List.of(ARN_ROOT), List.of(), "request.cryptographicPrincipalsToAdd"},
                {List.of(), List.of(), List.of(), List.of(ARN_ROOT), "request.cryptographicPrincipalsToRemove"},
        };
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("updateEncryptionKeyResourceAccessTestWhenBadPrincipalsDataProvider")
    void updateEncryptionKeyResourceAccessTestWhenBadPrincipals(List<String> administratorPrincipalsToAdd, List<String> administratorPrincipalsToRemove,
            List<String> cryptographicPrincipalsToAdd, List<String> cryptographicPrincipalsToRemove, String propertyNameExpected) {
        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithAddAndRemove(
                administratorPrincipalsToAdd, administratorPrincipalsToRemove, cryptographicPrincipalsToAdd, cryptographicPrincipalsToRemove, List.of(),
                List.of());
        when(arnService.isInstanceProfileArn(ARN_ROOT)).thenReturn(false);
        when(arnService.isRoleArn(ARN_ROOT)).thenReturn(false);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage(
                String.format("The following elements of %s are malformed. Only IAM instance-profile and role resource ARNs are supported. %s",
                        propertyNameExpected, List.of(ARN_ROOT)));
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    static Object[][] updateEncryptionKeyResourceAccessTestWhenBadAuthorizedClientsDataProvider() {
        return new Object[][]{
                // cryptographicAuthorizedClientsToAdd, cryptographicAuthorizedClientsToRemove, propertyNameExpected
                {List.of(ARN_ROOT), List.of(), "request.cryptographicAuthorizedClientsToAdd"},
                {List.of(), List.of(ARN_ROOT), "request.cryptographicAuthorizedClientsToRemove"},
        };
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("updateEncryptionKeyResourceAccessTestWhenBadAuthorizedClientsDataProvider")
    void updateEncryptionKeyResourceAccessTestWhenBadAuthorizedClients(List<String> cryptographicAuthorizedClientsToAdd,
            List<String> cryptographicAuthorizedClientsToRemove, String propertyNameExpected) {
        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithAddAndRemove(
                List.of(), List.of(), List.of(), List.of(), cryptographicAuthorizedClientsToAdd, cryptographicAuthorizedClientsToRemove);
        when(arnService.isEc2InstanceArn(ARN_ROOT)).thenReturn(false);
        when(arnService.isSecretsManagerSecretArn(ARN_ROOT)).thenReturn(false);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage(
                String.format("The following elements of %s are malformed. Only EC2 instance and SecretsManager secret resource ARNs are supported. %s",
                        propertyNameExpected, List.of(ARN_ROOT)));
        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenEmpty() {
        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestEmpty();

        underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest);

        verify(awsClient, never()).createAWSKMS(any(AwsCredentialView.class), anyString());
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenGetKeyPolicyError() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        IllegalStateException ise = new IllegalStateException("Problem");
        when(kmsClient.getKeyPolicy(any(GetKeyPolicyRequest.class))).thenThrow(ise);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(
                List.of(ARN_INSTANCE_PROFILE), List.of(), List.of());
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalStateException).hasMessage("Problem");
        verifyGetKeyPolicyRequest();
        verify(kmsClient, never()).putKeyPolicy(any(PutKeyPolicyRequest.class));
    }

    private void verifyGetKeyPolicyRequest() {
        verify(kmsClient).getKeyPolicy(getKeyPolicyRequestCaptor.capture());
        GetKeyPolicyRequest getKeyPolicyRequest = getKeyPolicyRequestCaptor.getValue();
        assertThat(getKeyPolicyRequest).isNotNull();
        assertThat(getKeyPolicyRequest.keyId()).isEqualTo(ARN_KMS_KEY);
        assertThat(getKeyPolicyRequest.policyName()).isEqualTo(POLICY_NAME);
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenMalformedPolicyJson() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy("{}");

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(
                List.of(ARN_INSTANCE_PROFILE), List.of(), List.of());
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage("At least one policy statement is required.");
        verifyGetKeyPolicyRequest();
        verify(kmsClient, never()).putKeyPolicy(any(PutKeyPolicyRequest.class));
    }

    private void setupGetKeyPolicy(String policyJson) {
        GetKeyPolicyResponse getKeyPolicyResponse = GetKeyPolicyResponse.builder()
                .policy(policyJson)
                .build();
        when(kmsClient.getKeyPolicy(any(GetKeyPolicyRequest.class))).thenReturn(getKeyPolicyResponse);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void updateEncryptionKeyResourceAccessTestWhenUpdateAdministratorPrincipalButMissingKeyAdministratorStatement(boolean operationAdd) {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy("{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\",\n" +
                "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}]}");
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = operationAdd ?
                createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(List.of(ARN_INSTANCE_PROFILE), List.of(), List.of()) :
                createUpdateEncryptionKeyResourceAccessRequestWithRemoveOnly(List.of(ARN_INSTANCE_PROFILE), List.of(), List.of());
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage(
                String.format("Cannot find resource policy statement with ID [%s] for KMS key [%s]", POLICY_STATEMENT_ID_KEY_ADMINISTRATOR, ARN_KMS_KEY));

        verifyGetKeyPolicyRequest();
        verify(kmsClient, never()).putKeyPolicy(any(PutKeyPolicyRequest.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void updateEncryptionKeyResourceAccessTestWhenUpdateCryptographicPrincipalButMissingKeyCryptographicStatement(boolean operationAdd) {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy("{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\",\n" +
                "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}]}");
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = operationAdd ?
                createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(List.of(), List.of(ARN_INSTANCE_PROFILE), List.of()) :
                createUpdateEncryptionKeyResourceAccessRequestWithRemoveOnly(List.of(), List.of(ARN_INSTANCE_PROFILE), List.of());
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage(
                String.format("Cannot find resource policy statement with ID [%s] for KMS key [%s]", POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER, ARN_KMS_KEY));

        verifyGetKeyPolicyRequest();
        verify(kmsClient, never()).putKeyPolicy(any(PutKeyPolicyRequest.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void updateEncryptionKeyResourceAccessTestWhenUpdateAuthorizedClientButMissingKeyCryptographicStatement(boolean operationAdd) {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy("{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\",\n" +
                "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}]}");
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = operationAdd ?
                createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(List.of(), List.of(), List.of(ARN_EC2_INSTANCE)) :
                createUpdateEncryptionKeyResourceAccessRequestWithRemoveOnly(List.of(), List.of(), List.of(ARN_EC2_INSTANCE));
        when(arnService.isEc2InstanceArn(ARN_EC2_INSTANCE)).thenReturn(true);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest));

        assertThat(illegalArgumentException).hasMessage(
                String.format("Cannot find resource policy statement with ID [%s] for KMS key [%s]", POLICY_STATEMENT_ID_KEY_CRYPTOGRAPHIC_USER, ARN_KMS_KEY));

        verifyGetKeyPolicyRequest();
        verify(kmsClient, never()).putKeyPolicy(any(PutKeyPolicyRequest.class));
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenAddOnly() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy(POLICY_JSON_TO_UPDATE);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithAddOnly(
                List.of(ARN_CREDENTIAL_ROLE_3), List.of(ARN_INSTANCE_PROFILE_3), List.of(ARN_EC2_INSTANCE_3, ARN_SECRETSMANAGER_SECRET_3));
        when(arnService.isInstanceProfileArn(ARN_CREDENTIAL_ROLE_3)).thenReturn(false);
        when(arnService.isRoleArn(ARN_CREDENTIAL_ROLE_3)).thenReturn(true);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE_3)).thenReturn(true);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_CREDENTIAL_ROLE_3)).thenReturn(ARN_CREDENTIAL_ROLE_3);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_INSTANCE_PROFILE_3)).thenReturn(ARN_ROLE_3);
        when(arnService.isEc2InstanceArn(ARN_EC2_INSTANCE_3)).thenReturn(true);
        when(arnService.isEc2InstanceArn(ARN_SECRETSMANAGER_SECRET_3)).thenReturn(false);
        when(arnService.isSecretsManagerSecretArn(ARN_SECRETSMANAGER_SECRET_3)).thenReturn(true);

        underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest);

        verifyGetKeyPolicyRequest();
        verifyPutKeyPolicyRequest(
                "{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\"," +
                        "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow access for the CDP credential IAM role as a key administrator\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":[\"arn:aws-us-gov:iam::123456789012:role/my-credential-role\"," +
                        "\"arn:aws-us-gov:iam::123456789012:role/my-credential-role-2\",\"arn:aws-us-gov:iam::123456789012:role/my-credential-role-3\"]}," +
                        "\"Action\":[\"kms:GetKeyPolicy\",\"kms:PutKeyPolicy\",\"kms:ScheduleKeyDeletion\"],\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow use of the key by CDP clusters for cryptographic operations\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":[\"arn:aws-us-gov:iam::123456789012:role/my-role\",\"arn:aws-us-gov:iam::123456789012:role/my-role-2\"," +
                        "\"arn:aws-us-gov:iam::123456789012:role/my-role-3\"]},\"Action\":[\"kms:Decrypt\",\"kms:Encrypt\",\"kms:GenerateDataKey\"]," +
                        "\"Resource\":\"*\",\"Condition\":{\"ArnEquals\":{\"ec2:SourceInstanceArn\":[" +
                        "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-0bc43096314295350\"," +
                        "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-e842a720f901b547d\"," +
                        "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-1ec6923af50a86b21\"],\"kms:EncryptionContext:SecretARN\":[" +
                        "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-rk4Dy0\"," +
                        "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-2-Pi92sB\"," +
                        "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-3-8Lwc1u\"]}}}]}");
    }

    private void verifyPutKeyPolicyRequest(String policyJsonExpected) {
        verify(kmsClient).putKeyPolicy(putKeyPolicyRequestCaptor.capture());
        PutKeyPolicyRequest putKeyPolicyRequest = putKeyPolicyRequestCaptor.getValue();
        assertThat(putKeyPolicyRequest).isNotNull();
        assertThat(putKeyPolicyRequest.keyId()).isEqualTo(ARN_KMS_KEY);
        assertThat(putKeyPolicyRequest.policyName()).isEqualTo(POLICY_NAME);
        assertThat(putKeyPolicyRequest.policy()).isEqualTo(policyJsonExpected);
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenRemoveOnly() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy(POLICY_JSON_TO_UPDATE);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithRemoveOnly(
                List.of(ARN_CREDENTIAL_ROLE), List.of(ARN_INSTANCE_PROFILE), List.of(ARN_EC2_INSTANCE, ARN_SECRETSMANAGER_SECRET));
        when(arnService.isInstanceProfileArn(ARN_CREDENTIAL_ROLE)).thenReturn(false);
        when(arnService.isRoleArn(ARN_CREDENTIAL_ROLE)).thenReturn(true);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_CREDENTIAL_ROLE)).thenReturn(ARN_CREDENTIAL_ROLE);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_INSTANCE_PROFILE)).thenReturn(ARN_ROLE);
        when(arnService.isEc2InstanceArn(ARN_EC2_INSTANCE)).thenReturn(true);
        when(arnService.isEc2InstanceArn(ARN_SECRETSMANAGER_SECRET)).thenReturn(false);
        when(arnService.isSecretsManagerSecretArn(ARN_SECRETSMANAGER_SECRET)).thenReturn(true);

        underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest);

        verifyGetKeyPolicyRequest();
        verifyPutKeyPolicyRequest(
                "{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\"," +
                        "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow access for the CDP credential IAM role as a key administrator\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:role/my-credential-role-2\"}," +
                        "\"Action\":[\"kms:GetKeyPolicy\",\"kms:PutKeyPolicy\",\"kms:ScheduleKeyDeletion\"],\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow use of the key by CDP clusters for cryptographic operations\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:role/my-role-2\"}," +
                        "\"Action\":[\"kms:Decrypt\",\"kms:Encrypt\",\"kms:GenerateDataKey\"]," +
                        "\"Resource\":\"*\",\"Condition\":{\"ArnEquals\":{\"ec2:SourceInstanceArn\":" +
                        "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-e842a720f901b547d\"" +
                        ",\"kms:EncryptionContext:SecretARN\":" +
                        "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-2-Pi92sB\"}}}]}");
    }

    @Test
    void updateEncryptionKeyResourceAccessTestWhenAddAndRemove() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);
        setupGetKeyPolicy(POLICY_JSON_TO_UPDATE);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);

        UpdateEncryptionKeyResourceAccessRequest updateEncryptionKeyResourceAccessRequest = createUpdateEncryptionKeyResourceAccessRequestWithAddAndRemove(
                List.of(ARN_CREDENTIAL_ROLE_3), List.of(ARN_CREDENTIAL_ROLE), List.of(ARN_INSTANCE_PROFILE_3), List.of(ARN_INSTANCE_PROFILE),
                List.of(ARN_EC2_INSTANCE_3, ARN_SECRETSMANAGER_SECRET_3), List.of(ARN_EC2_INSTANCE, ARN_SECRETSMANAGER_SECRET));
        when(arnService.isInstanceProfileArn(ARN_CREDENTIAL_ROLE_3)).thenReturn(false);
        when(arnService.isRoleArn(ARN_CREDENTIAL_ROLE_3)).thenReturn(true);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE_3)).thenReturn(true);
        when(arnService.isInstanceProfileArn(ARN_CREDENTIAL_ROLE)).thenReturn(false);
        when(arnService.isRoleArn(ARN_CREDENTIAL_ROLE)).thenReturn(true);
        when(arnService.isInstanceProfileArn(ARN_INSTANCE_PROFILE)).thenReturn(true);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_CREDENTIAL_ROLE_3)).thenReturn(ARN_CREDENTIAL_ROLE_3);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_INSTANCE_PROFILE_3)).thenReturn(ARN_ROLE_3);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_CREDENTIAL_ROLE)).thenReturn(ARN_CREDENTIAL_ROLE);
        when(awsIamService.getEffectivePrincipal(amazonIdentityManagementClient, ARN_INSTANCE_PROFILE)).thenReturn(ARN_ROLE);
        when(arnService.isEc2InstanceArn(ARN_EC2_INSTANCE_3)).thenReturn(true);
        when(arnService.isEc2InstanceArn(ARN_SECRETSMANAGER_SECRET_3)).thenReturn(false);
        when(arnService.isSecretsManagerSecretArn(ARN_SECRETSMANAGER_SECRET_3)).thenReturn(true);
        when(arnService.isEc2InstanceArn(ARN_EC2_INSTANCE)).thenReturn(true);
        when(arnService.isEc2InstanceArn(ARN_SECRETSMANAGER_SECRET)).thenReturn(false);
        when(arnService.isSecretsManagerSecretArn(ARN_SECRETSMANAGER_SECRET)).thenReturn(true);

        underTest.updateEncryptionKeyResourceAccess(updateEncryptionKeyResourceAccessRequest);

        verifyGetKeyPolicyRequest();
        verifyPutKeyPolicyRequest(
                "{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":[{\"Sid\":\"Enable IAM user permissions\"," +
                        "\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"arn:aws-us-gov:iam::123456789012:root\"},\"Action\":\"kms:*\",\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow access for the CDP credential IAM role as a key administrator\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":[" +
                        "\"arn:aws-us-gov:iam::123456789012:role/my-credential-role-2\",\"arn:aws-us-gov:iam::123456789012:role/my-credential-role-3\"]}," +
                        "\"Action\":[\"kms:GetKeyPolicy\",\"kms:PutKeyPolicy\",\"kms:ScheduleKeyDeletion\"],\"Resource\":\"*\"}," +
                        "{\"Sid\":\"Allow use of the key by CDP clusters for cryptographic operations\",\"Effect\":\"Allow\"," +
                        "\"Principal\":{\"AWS\":[\"arn:aws-us-gov:iam::123456789012:role/my-role-2\"," +
                        "\"arn:aws-us-gov:iam::123456789012:role/my-role-3\"]},\"Action\":[\"kms:Decrypt\",\"kms:Encrypt\",\"kms:GenerateDataKey\"]," +
                        "\"Resource\":\"*\",\"Condition\":{\"ArnEquals\":{\"ec2:SourceInstanceArn\":[" +
                        "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-e842a720f901b547d\"," +
                        "\"arn:aws-us-gov:ec2:us-gov-west-1:123456789012:instance/i-1ec6923af50a86b21\"],\"kms:EncryptionContext:SecretARN\":[" +
                        "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-2-Pi92sB\"," +
                        "\"arn:aws-us-gov:secretsmanager:us-gov-west-1:123456789012:secret:my-secret-3-8Lwc1u\"]}}}]}");
    }

}