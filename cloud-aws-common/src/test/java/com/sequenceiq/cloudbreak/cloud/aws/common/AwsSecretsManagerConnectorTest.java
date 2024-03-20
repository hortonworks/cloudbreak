package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecretsManagerClient;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
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
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutResourcePolicyRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutResourcePolicyResponse;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import software.amazon.awssdk.services.secretsmanager.model.Tag;
import software.amazon.awssdk.services.secretsmanager.model.UpdateSecretRequest;

@ExtendWith(MockitoExtension.class)
public class AwsSecretsManagerConnectorTest {

    private static final String REGION = "REGION";

    private static final String ARN = "ARN";

    private static final String NAME = "NAME";

    private static final String DESCRIPTION = "DESCRIPTION";

    private static final String SECRET = "SECRET";

    private static final Instant DELETION_DATE = Instant.EPOCH;

    private static final EncryptionKeySource ENCRYPTION_KEY_SOURCE = EncryptionKeySource.builder()
            .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
            .withKeyValue("AWS_KMS_KEY_ARN")
            .build();

    private static final Map<String, String> TAGS = Map.of(
            "KEY1", "VALUE1",
            "KEY2", "VALUE2"
    );

    private static final List<Tag> SECRETSMANAGER_TAGS = List.of(
            Tag.builder().key("KEY1").value("VALUE1").build(),
            Tag.builder().key("KEY2").value("VALUE2").build()
    );

    private static final List<String> PRINCIPALS = List.of("principal_arn1", "principal_arn2");

    private static final List<String> AUTHORIZED_CLIENTS = List.of("instance_id1", "instance_id2");

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder()
            .withLocation(Location.location(Region.region(REGION)))
            .build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonSecretsManagerClient secretsManagerClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @InjectMocks
    private AwsSecretsManagerConnector underTest;

    @Captor
    private ArgumentCaptor<DescribeSecretRequest> describeSecretRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<GetSecretValueRequest> getSecretValueRequestArgumentCaptor;

    @BeforeEach
    void setup() {
        when(awsClient.createSecretsManagerClient(any(), any())).thenReturn(secretsManagerClient);
    }

    @Test
    void testCreateCloudSecretWhenSecretDoesNotExistAnywhere() {
        ArgumentCaptor<CreateSecretRequest> createSecretRequestArgumentCaptor = ArgumentCaptor.forClass(CreateSecretRequest.class);
        when(secretsManagerClient.describeSecret(any()))
                .thenThrow(ResourceNotFoundException.class)
                .thenReturn(getDescribeSecretResponse(NAME, ARN));
        when(secretsManagerClient.getSecretValue(any())).thenReturn(getGetSecretValueResponse(NAME, ARN));
        when(secretsManagerClient.createSecret(any())).thenReturn(CreateSecretResponse.builder().arn(ARN).build());
        when(awsTaggingService.prepareSecretsManagerTags(TAGS)).thenReturn(SECRETSMANAGER_TAGS);
        CreateCloudSecretRequest createCloudSecretRequest = CreateCloudSecretRequest.builder()
                .withCloudContext(CLOUD_CONTEXT)
                .withCloudCredential(CLOUD_CREDENTIAL)
                .withCloudResources(List.of())
                .withSecretName(NAME)
                .withDescription(DESCRIPTION)
                .withSecretValue(SECRET)
                .withEncryptionKeySource(Optional.of(ENCRYPTION_KEY_SOURCE))
                .withTags(TAGS)
                .build();


        CloudSecret result = underTest.createCloudSecret(createCloudSecretRequest);

        fullAssertions(result, NAME, ARN);
        verify(secretsManagerClient, times(1)).createSecret(createSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(2)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        verify(persistenceNotifier, times(1)).notifyAllocation(any(), eq(CLOUD_CONTEXT));
        CreateSecretRequest createRequest = createSecretRequestArgumentCaptor.getValue();
        DescribeSecretRequest describeRequest1 = describeSecretRequestArgumentCaptor.getAllValues().getFirst();
        DescribeSecretRequest describeRequest2 = describeSecretRequestArgumentCaptor.getAllValues().getLast();
        GetSecretValueRequest getRequest = getSecretValueRequestArgumentCaptor.getValue();
        assertEquals(NAME, createRequest.name());
        assertEquals(SECRET, createRequest.secretString());
        assertEquals(DESCRIPTION, createRequest.description());
        assertEquals(ENCRYPTION_KEY_SOURCE.keyValue(), createRequest.kmsKeyId());
        assertEquals(SECRETSMANAGER_TAGS, createRequest.tags());
        assertEquals(NAME, describeRequest1.secretId());
        assertEquals(ARN, describeRequest2.secretId());
        assertEquals(ARN, getRequest.secretId());
    }

    @Test
    void testCreateCloudSecretWhenSecretExistsOnBothSides() {
        when(secretsManagerClient.describeSecret(any()))
                .thenReturn(getDescribeSecretResponse("existing_secret_name", "existing_secret_arn"));
        when(secretsManagerClient.getSecretValue(any()))
                .thenReturn(getGetSecretValueResponse("existing_secret_name", "existing_secret_arn"));
        CreateCloudSecretRequest createCloudSecretRequest = CreateCloudSecretRequest.builder()
                .withCloudContext(CLOUD_CONTEXT)
                .withCloudResources(List.of(CloudResource.builder()
                        .withName("existing_secret_name")
                        .withReference("existing_secret_arn")
                        .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                        .withParameters(Map.of())
                        .build()))
                .withSecretName("existing_secret_name")
                .build();

        CloudSecret result = underTest.createCloudSecret(createCloudSecretRequest);

        fullAssertions(result, "existing_secret_name", "existing_secret_arn");
        verify(secretsManagerClient, times(1)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        verify(secretsManagerClient, never()).createSecret(any());
        verify(persistenceNotifier, never()).notifyAllocation(any(), any());
        getCloudSecretAssertions("existing_secret_arn");
    }

    @Test
    void testCreateCloudSecretWhenSecretOnlyExistsOnProvider() {
        when(secretsManagerClient.describeSecret(any()))
                .thenReturn(getDescribeSecretResponse("existing_secret_name", "existing_secret_arn"));
        when(secretsManagerClient.getSecretValue(any()))
                .thenReturn(getGetSecretValueResponse("existing_secret_name", "existing_secret_arn"));
        CreateCloudSecretRequest createCloudSecretRequest = CreateCloudSecretRequest.builder()
                .withCloudContext(CLOUD_CONTEXT)
                .withCloudResources(List.of())
                .withSecretName("existing_secret_name")
                .build();

        CloudSecret result = underTest.createCloudSecret(createCloudSecretRequest);

        fullAssertions(result, "existing_secret_name", "existing_secret_arn");
        verify(secretsManagerClient, times(1)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        verify(secretsManagerClient, never()).createSecret(any());
        verify(persistenceNotifier, times(1)).notifyAllocation(any(), eq(CLOUD_CONTEXT));
        DescribeSecretRequest describeRequest = describeSecretRequestArgumentCaptor.getValue();
        GetSecretValueRequest getRequest = getSecretValueRequestArgumentCaptor.getValue();
        assertEquals("existing_secret_name", describeRequest.secretId());
        assertEquals("existing_secret_arn", getRequest.secretId());
    }

    @Test
    void testGetCloudSecretWhenSecretDoesNotExist() {
        when(secretsManagerClient.getSecretValue(any())).thenThrow(ResourceNotFoundException.class);
        CloudResource cloudResource = CloudResource.builder()
                .withReference("arn_of_non_existent_secret")
                .withName("name_of_non_existent_secret")
                .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                .withParameters(Map.of())
                .build();
        GetCloudSecretRequest getCloudSecretRequest = new GetCloudSecretRequest(CLOUD_CONTEXT, CLOUD_CREDENTIAL, cloudResource);

        assertThrows(NotFoundException.class, () -> underTest.getCloudSecret(getCloudSecretRequest));

        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        assertEquals("arn_of_non_existent_secret", getSecretValueRequestArgumentCaptor.getValue().secretId());
    }

    @Test
    void testGetCloudSecretWhenSecretExists() {
        when(secretsManagerClient.describeSecret(any()))
                .thenReturn(getDescribeSecretResponse("existing_secret_name", "existing_secret_arn"));
        when(secretsManagerClient.getSecretValue(any()))
                .thenReturn(getGetSecretValueResponse("existing_secret_name", "existing_secret_arn"));
        CloudResource cloudResource = CloudResource.builder()
                .withReference("existing_secret_arn")
                .withName("existing_secret_name")
                .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                .withParameters(Map.of())
                .build();
        GetCloudSecretRequest getCloudSecretRequest = new GetCloudSecretRequest(CLOUD_CONTEXT, CLOUD_CREDENTIAL, cloudResource);

        CloudSecret result = underTest.getCloudSecret(getCloudSecretRequest);

        fullAssertions(result, "existing_secret_name", "existing_secret_arn");
        verify(secretsManagerClient, times(1)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        getCloudSecretAssertions("existing_secret_arn");
    }

    @Test
    void testUpdateCloudSecretWhenNewSecretValueAndKMSKeyAreProvided() {
        ArgumentCaptor<UpdateSecretRequest> updateSecretRequestArgumentCaptor = ArgumentCaptor.forClass(UpdateSecretRequest.class);
        when(secretsManagerClient.describeSecret(any())).thenReturn(getDescribeSecretResponse(NAME, ARN));
        when(secretsManagerClient.getSecretValue(any())).thenReturn(getGetSecretValueResponse(NAME, ARN));
        EncryptionKeySource newEncryptionKeySource = EncryptionKeySource.builder()
                .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                .withKeyValue("new_key_value")
                .build();
        UpdateCloudSecretRequest updateCloudSecretRequest = UpdateCloudSecretRequest.builder()
                .withCloudContext(CLOUD_CONTEXT)
                .withCloudCredential(CLOUD_CREDENTIAL)
                .withCloudResource(CloudResource.builder()
                        .withReference(ARN)
                        .withName(NAME)
                        .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                        .withParameters(Map.of())
                        .build())
                .withNewSecretValue(Optional.of("new_secret_value"))
                .withNewEncryptionKeySource(Optional.of(newEncryptionKeySource))
                .build();

        CloudSecret result = underTest.updateCloudSecret(updateCloudSecretRequest);

        assertEquals(ARN, result.secretId());
        assertEquals(NAME, result.secretName());
        assertEquals(DESCRIPTION, result.description());
        assertEquals("new_secret_value", result.secretValue());
        assertEquals(newEncryptionKeySource, result.keySource());
        assertEquals(TAGS, result.tags());
        verify(secretsManagerClient, times(1)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).updateSecret(updateSecretRequestArgumentCaptor.capture());
        UpdateSecretRequest updateSecretRequest = updateSecretRequestArgumentCaptor.getValue();
        assertEquals(ARN, updateSecretRequest.secretId());
        assertEquals("new_secret_value", updateSecretRequest.secretString());
        assertEquals(newEncryptionKeySource.keyValue(), updateSecretRequest.kmsKeyId());
        getCloudSecretAssertions(ARN);
    }

    @Test
    void testUpdateCloudSecretWhenNewValuesAreTheSameAsCurrentOnes() {
        when(secretsManagerClient.describeSecret(any())).thenReturn(getDescribeSecretResponse(NAME, ARN));
        when(secretsManagerClient.getSecretValue(any())).thenReturn(getGetSecretValueResponse(NAME, ARN));
        UpdateCloudSecretRequest updateCloudSecretRequest = UpdateCloudSecretRequest.builder()
                .withCloudContext(CLOUD_CONTEXT)
                .withCloudCredential(CLOUD_CREDENTIAL)
                .withCloudResource(CloudResource.builder()
                        .withReference(ARN)
                        .withName(NAME)
                        .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                        .withParameters(Map.of())
                        .build())
                .withNewSecretValue(Optional.of(SECRET))
                .withNewEncryptionKeySource(Optional.of(EncryptionKeySource.builder()
                        .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                        .withKeyValue("AWS_KMS_KEY_ARN")
                        .build()))
                .build();

        CloudSecret result = underTest.updateCloudSecret(updateCloudSecretRequest);

        fullAssertions(result, NAME, ARN);
        verify(secretsManagerClient, times(1)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        verify(secretsManagerClient, never()).updateSecret(any());
        getCloudSecretAssertions(ARN);
    }

    @Test
    void testDeleteCloudSecretWhenItIsPresentOnBothSides() {
        List<CloudResource> cloudResources = List.of(CloudResource.builder()
                .withReference(ARN)
                .withName(NAME)
                .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                .withParameters(Map.of())
                .build());
        DeleteCloudSecretRequest deleteCloudSecretRequest = new DeleteCloudSecretRequest(CLOUD_CONTEXT, CLOUD_CREDENTIAL, cloudResources, NAME);

        underTest.deleteCloudSecret(deleteCloudSecretRequest);

        verify(secretsManagerClient, times(1)).deleteSecret(any());
        verify(persistenceNotifier, times(1)).notifyDeletion(any(), eq(CLOUD_CONTEXT));
    }

    @Test
    void testupdateCloudSecretResourceAccess() {
        ArgumentCaptor<PutResourcePolicyRequest> putResourcePolicyRequestArgumentCaptor = ArgumentCaptor.forClass(PutResourcePolicyRequest.class);
        when(secretsManagerClient.describeSecret(any())).thenReturn(getDescribeSecretResponse(NAME, ARN));
        when(secretsManagerClient.getSecretValue(any())).thenReturn(getGetSecretValueResponse(NAME, ARN));
        when(secretsManagerClient.putResourcePolicy(any())).thenReturn(PutResourcePolicyResponse.builder().build());
        CloudResource cloudResource = CloudResource.builder()
                .withReference(ARN)
                .withName(NAME)
                .withType(ResourceType.AWS_SECRETSMANAGER_SECRET)
                .withParameters(Map.of())
                .build();
        UpdateCloudSecretResourceAccessRequest updateCloudSecretResourceAccessRequest =
                new UpdateCloudSecretResourceAccessRequest(CLOUD_CONTEXT, CLOUD_CREDENTIAL, cloudResource, PRINCIPALS, AUTHORIZED_CLIENTS);

        CloudSecret result = underTest.updateCloudSecretResourceAccess(updateCloudSecretResourceAccessRequest);

        String expectedPolicyJsonString = "{\"Version\":\"2012-10-17\",\"Id\":\"Policy generated by CDP\",\"Statement\":{" +
                "\"Sid\":\"RestrictAccessToSpecificInstances\",\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"principal_arn1\",\"principal_arn2\"]}," +
                "\"Action\":[\"secretsmanager:DeleteSecret\",\"secretsmanager:GetSecretValue\"]," +
                "\"Resource\":\"*\",\"Condition\":{\"ArnEquals\":{\"ec2:SourceInstanceArn\":[\"instance_id1\",\"instance_id2\"]}}}}";
        fullAssertions(result, NAME, ARN);
        assertEquals(PRINCIPALS, result.principals());
        assertEquals(AUTHORIZED_CLIENTS, result.authorizedClients());
        verify(secretsManagerClient, times(1)).putResourcePolicy(putResourcePolicyRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).describeSecret(describeSecretRequestArgumentCaptor.capture());
        verify(secretsManagerClient, times(1)).getSecretValue(getSecretValueRequestArgumentCaptor.capture());
        PutResourcePolicyRequest putResourcePolicyRequest = putResourcePolicyRequestArgumentCaptor.getValue();
        assertEquals(expectedPolicyJsonString, putResourcePolicyRequest.resourcePolicy());
        assertEquals(ARN, putResourcePolicyRequest.secretId());
        getCloudSecretAssertions(ARN);
    }

    private void fullAssertions(CloudSecret cloudSecret, String secretName, String secretArn) {
        assertEquals(secretArn, cloudSecret.secretId());
        assertEquals(secretName, cloudSecret.secretName());
        assertEquals(DESCRIPTION, cloudSecret.description());
        assertEquals(SECRET, cloudSecret.secretValue());
        assertEquals(ENCRYPTION_KEY_SOURCE, cloudSecret.keySource());
        assertEquals(TAGS, cloudSecret.tags());
        assertEquals(DELETION_DATE, cloudSecret.deletionDate());
    }

    private void getCloudSecretAssertions(String arn) {
        DescribeSecretRequest describeRequest = describeSecretRequestArgumentCaptor.getValue();
        GetSecretValueRequest getRequest = getSecretValueRequestArgumentCaptor.getValue();
        assertEquals(arn, describeRequest.secretId());
        assertEquals(arn, getRequest.secretId());
    }

    private DescribeSecretResponse getDescribeSecretResponse(String secretName, String secretArn) {
        return DescribeSecretResponse.builder()
                .arn(secretArn)
                .name(secretName)
                .description(DESCRIPTION)
                .kmsKeyId(ENCRYPTION_KEY_SOURCE.keyValue())
                .deletedDate(DELETION_DATE)
                .tags(SECRETSMANAGER_TAGS)
                .build();
    }

    private GetSecretValueResponse getGetSecretValueResponse(String secretName, String secretArn) {
        return GetSecretValueResponse.builder()
                .arn(secretArn)
                .name(secretName)
                .secretString(SECRET)
                .build();
    }
}
