package com.sequenceiq.cloudbreak.cloud.aws.common;


import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsEncryptionResources.TAG_KEY_CLOUDERA_KMS_KEY_TARGET;
import static com.sequenceiq.common.api.type.ResourceType.AWS_KMS_KEY;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonIdentityManagementClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.kms.AmazonKmsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsIamService;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.common.api.type.CommonStatus;

import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.KeyListEntry;
import software.amazon.awssdk.services.kms.model.KeyMetadata;
import software.amazon.awssdk.services.kms.model.KeySpec;
import software.amazon.awssdk.services.kms.model.KeyUsageType;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.OriginType;
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

    private static final String ARN_ROOT = "arn:aws-us-gov:iam::123456789012:root";

    private static final String ARN_CREDENTIAL_ROLE = "arn:aws-us-gov:iam::123456789012:role/my-credential-role";

    private static final String ARN_ROLE = "arn:aws-us-gov:iam::123456789012:role/my-role";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AmazonKmsUtil amazonKmsUtil;

    @Mock
    private AwsIamService awsIamService;

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
        return createEncryptionKeyCreationRequest(Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null, List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithCloudResources(List<CloudResource> cloudResources) {
        return createEncryptionKeyCreationRequest(Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), cloudResources, List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithTags(Map<String, String> tags) {
        return createEncryptionKeyCreationRequest(tags, null, List.of(ARN_INSTANCE_PROFILE));
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequestWithTargetPrincipalIds(List<String> targetPrincipalIds) {
        return createEncryptionKeyCreationRequest(Map.ofEntries(entry(TAG_KEY, TAG_VALUE)), null, targetPrincipalIds);
    }

    private EncryptionKeyCreationRequest createEncryptionKeyCreationRequest(Map<String, String> tags, List<CloudResource> cloudResources,
            List<String> targetPrincipalIds) {
        return EncryptionKeyCreationRequest.builder()
                .withKeyName(KEY_NAME)
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withTags(tags)
                .withDescription(DESCRIPTION)
                .withCloudResources(cloudResources)
                .withTargetPrincipalIds(targetPrincipalIds)
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

        CloudResource kmsCloudResource = CloudResource.builder()
                .withName(KEY_NAME)
                .withType(AWS_KMS_KEY)
                .withReference(ARN_KMS_KEY)
                .build();
        List<CloudResource> cloudResourcesInRequest = List.of(kmsCloudResource);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCloudResources(cloudResourcesInRequest);
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

        CloudResource kmsCloudResource = CloudResource.builder()
                .withName(KEY_NAME)
                .withType(AWS_KMS_KEY)
                .withReference(ARN_KMS_KEY)
                .build();
        CloudResource kmsCloudResourceDummy = CloudResource.builder()
                .withName(KEY_NAME_DUMMY)
                .withType(AWS_KMS_KEY)
                .withReference(ARN_KMS_KEY_DUMMY)
                .build();
        List<CloudResource> cloudResourcesInRequest = List.of(kmsCloudResourceDummy, kmsCloudResource);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCloudResources(cloudResourcesInRequest);
        when(cloudResourceHelper.getResourceTypeInstancesFromList(AWS_KMS_KEY, cloudResourcesInRequest))
                .thenReturn(List.of(kmsCloudResourceDummy, kmsCloudResource));

        setupKeyMetadataForExistingKey();

        CloudEncryptionKey cloudEncryptionKey = underTest.createEncryptionKey(encryptionKeyCreationRequest);

        verifyCloudEncryptionKey(cloudEncryptionKey, metadataMap);
        verify(amazonKmsUtil, never()).listKeysWithAllPages(kmsClient);
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

    @Test
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndExistingKmsKey() {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

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

        CloudResource kmsCloudResourceDummy = CloudResource.builder()
                .withName(KEY_NAME_DUMMY)
                .withType(AWS_KMS_KEY)
                .withReference(ARN_KMS_KEY_DUMMY)
                .build();
        List<CloudResource> cloudResourcesInRequest = List.of(kmsCloudResourceDummy);
        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithCloudResources(cloudResourcesInRequest);
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

    private void setupPrincipalMapping() {
        when(awsIamService.getAccountRootArn(ARN_CREDENTIAL_ROLE)).thenReturn(ARN_ROOT);
        when(awsClient.createAmazonIdentityManagement(any(AwsCredentialView.class))).thenReturn(amazonIdentityManagementClient);
        when(awsIamService.getEffectivePrincipals(amazonIdentityManagementClient, encryptionKeyCreationRequest.targetPrincipalIds()))
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
                        "\"Action\":[\"kms:GetKeyPolicy\",\"kms:PutKeyPolicy\"],\"Resource\":\"*\"},{\"Sid\":\"Allow use of the key by CDP clusters\"," +
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

    static Object[][] emptyTargetPrincipalIdsDataProvider() {
        return new Object[][]{
                // targetPrincipalIds
                {null},
                {List.of()},
        };
    }

    @ParameterizedTest(name = "targetPrincipalIds={0}")
    @MethodSource("emptyTargetPrincipalIdsDataProvider")
    void createEncryptionKeyTestWhenCloudResourcesAbsentAndNoKmsKeysAtAllAndCreateNewKmsKeyAndEmptyTargetPrincipalIds(List<String> targetPrincipalIds) {
        when(awsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION))).thenReturn(kmsClient);

        encryptionKeyCreationRequest = createEncryptionKeyCreationRequestWithTargetPrincipalIds(targetPrincipalIds);

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
                () -> underTest.createEncryptionKey(encryptionKeyCreationRequest));

        assertThat(illegalArgumentException).hasMessage("targetPrincipalIds must not be null or empty");
        verify(amazonKmsUtil, never()).listResourceTagsWithAllPages(eq(kmsClient), any(ListResourceTagsRequest.class));
        verify(kmsClient, never()).createKey(any(CreateKeyRequest.class));
        verify(persistenceNotifier, never()).notifyAllocation(any(CloudResource.class), eq(cloudContext));
    }

}