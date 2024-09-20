package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.DescribeKeyRequest;
import software.amazon.awssdk.services.kms.model.DescribeKeyResponse;
import software.amazon.awssdk.services.kms.model.EnableKeyRotationRequest;
import software.amazon.awssdk.services.kms.model.EnableKeyRotationResponse;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.GetKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.ListAliasesRequest;
import software.amazon.awssdk.services.kms.model.ListAliasesResponse;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;
import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyRequest;
import software.amazon.awssdk.services.kms.model.PutKeyPolicyResponse;
import software.amazon.awssdk.services.kms.model.RotateKeyOnDemandRequest;
import software.amazon.awssdk.services.kms.model.RotateKeyOnDemandResponse;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionRequest;
import software.amazon.awssdk.services.kms.model.ScheduleKeyDeletionResponse;

@ExtendWith(MockitoExtension.class)
class AmazonKmsClientTest {

    @Mock
    private KmsClient client;

    @InjectMocks
    private AmazonKmsClient underTest;

    @Test
    void listKeysTest() {
        ListKeysRequest listKeysRequest = ListKeysRequest.builder().build();
        ListKeysResponse listKeysResponse = ListKeysResponse.builder().build();
        when(client.listKeys(listKeysRequest)).thenReturn(listKeysResponse);

        ListKeysResponse response = underTest.listKeys(listKeysRequest);

        assertThat(response).isSameAs(listKeysResponse);
    }

    @Test
    void listAliasesTest() {
        ListAliasesRequest listAliasesRequest = ListAliasesRequest.builder().build();
        ListAliasesResponse listAliasesResponse = ListAliasesResponse.builder().build();
        when(client.listAliases(listAliasesRequest)).thenReturn(listAliasesResponse);

        ListAliasesResponse response = underTest.listAliases(listAliasesRequest);

        assertThat(response).isSameAs(listAliasesResponse);
    }

    @Test
    void listResourceTagsTest() {
        ListResourceTagsRequest listResourceTagsRequest = ListResourceTagsRequest.builder().build();
        ListResourceTagsResponse listResourceTagsResponse = ListResourceTagsResponse.builder().build();
        when(client.listResourceTags(listResourceTagsRequest)).thenReturn(listResourceTagsResponse);

        ListResourceTagsResponse response = underTest.listResourceTags(listResourceTagsRequest);

        assertThat(response).isSameAs(listResourceTagsResponse);
    }

    @Test
    void describeKeyTest() {
        DescribeKeyRequest describeKeyRequest = DescribeKeyRequest.builder().build();
        DescribeKeyResponse describeKeyResponse = DescribeKeyResponse.builder().build();
        when(client.describeKey(describeKeyRequest)).thenReturn(describeKeyResponse);

        DescribeKeyResponse response = underTest.describeKey(describeKeyRequest);

        assertThat(response).isSameAs(describeKeyResponse);
    }

    @Test
    void createKeyTest() {
        CreateKeyRequest createKeyRequest = CreateKeyRequest.builder().build();
        CreateKeyResponse createKeyResponse = CreateKeyResponse.builder().build();
        when(client.createKey(createKeyRequest)).thenReturn(createKeyResponse);

        CreateKeyResponse response = underTest.createKey(createKeyRequest);

        assertThat(response).isSameAs(createKeyResponse);
    }

    @Test
    void getKeyPolicyTest() {
        GetKeyPolicyRequest getKeyPolicyRequest = GetKeyPolicyRequest.builder().build();
        GetKeyPolicyResponse getKeyPolicyResponse = GetKeyPolicyResponse.builder().build();
        when(client.getKeyPolicy(getKeyPolicyRequest)).thenReturn(getKeyPolicyResponse);

        GetKeyPolicyResponse response = underTest.getKeyPolicy(getKeyPolicyRequest);

        assertThat(response).isSameAs(getKeyPolicyResponse);
    }

    @Test
    void putKeyPolicyTest() {
        PutKeyPolicyRequest putKeyPolicyRequest = PutKeyPolicyRequest.builder().build();
        PutKeyPolicyResponse putKeyPolicyResponse = PutKeyPolicyResponse.builder().build();
        when(client.putKeyPolicy(putKeyPolicyRequest)).thenReturn(putKeyPolicyResponse);

        PutKeyPolicyResponse response = underTest.putKeyPolicy(putKeyPolicyRequest);

        assertThat(response).isSameAs(putKeyPolicyResponse);
    }

    @Test
    void testScheduleKeyDeletion() {
        ScheduleKeyDeletionRequest scheduleKeyDeletionRequest = mock(ScheduleKeyDeletionRequest.class);
        ScheduleKeyDeletionResponse expectedResponse = mock(ScheduleKeyDeletionResponse.class);
        when(client.scheduleKeyDeletion(scheduleKeyDeletionRequest)).thenReturn(expectedResponse);
        ScheduleKeyDeletionResponse response = underTest.scheduleKeyDeletion(scheduleKeyDeletionRequest);
        verify(client).scheduleKeyDeletion(scheduleKeyDeletionRequest);
        assertEquals(expectedResponse, response);
    }

    @Test
    void testEnableKeyRotation() {
        EnableKeyRotationRequest enableKeyRotationRequest = mock(EnableKeyRotationRequest.class);
        EnableKeyRotationResponse expectedResponse = mock(EnableKeyRotationResponse.class);
        when(client.enableKeyRotation(enableKeyRotationRequest)).thenReturn(expectedResponse);

        EnableKeyRotationResponse response = underTest.enableKeyRotation(enableKeyRotationRequest);

        assertEquals(expectedResponse, response);
    }

    @Test
    void testRotateKeyOnDemand() {
        RotateKeyOnDemandRequest rotateKeyOnDemandRequest = mock(RotateKeyOnDemandRequest.class);
        RotateKeyOnDemandResponse expectedResponse = mock(RotateKeyOnDemandResponse.class);
        when(client.rotateKeyOnDemand(rotateKeyOnDemandRequest)).thenReturn(expectedResponse);

        RotateKeyOnDemandResponse response = underTest.rotateKeyOnDemand(rotateKeyOnDemandRequest);

        assertEquals(expectedResponse, response);
    }

}