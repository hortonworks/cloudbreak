package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeKeyPairsResult;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;

@ExtendWith(MockitoExtension.class)
class AwsPublicKeyConnectorTest {

    private static final String PUBLIC_KEY = "publicKey";

    private static final String PUBLIC_KEY_ID = "publicKeyId";

    private static final String RUNTIME_EXCEPTION = "runtime exception";

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AmazonEc2Client ec2client;

    @InjectMocks
    private AwsPublicKeyConnector underTest;

    @BeforeEach
    void setUp() {
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2client);
    }

    @Test
    void registerExisting() {
        PublicKeyRegisterRequest request = generateRegisterRequest();
        when(ec2client.describeKeyPairs(any())).thenReturn(new DescribeKeyPairsResult());
        underTest.register(request);
        verifyNoMoreInteractions(ec2client);
    }

    @Test
    void registerNew() {
        PublicKeyRegisterRequest request = generateRegisterRequest();
        when(ec2client.describeKeyPairs(any())).thenThrow(new AmazonServiceException("no such key"));
        underTest.register(request);
        ArgumentCaptor<ImportKeyPairRequest> captor = ArgumentCaptor.forClass(ImportKeyPairRequest.class);
        verify(ec2client).importKeyPair(captor.capture());
        ImportKeyPairRequest result = captor.getValue();
        assertThat(result.getKeyName()).isEqualTo(PUBLIC_KEY_ID);
        assertThat(result.getPublicKeyMaterial()).isEqualTo(PUBLIC_KEY);
        verifyNoMoreInteractions(ec2client);
    }

    @Test
    void registerProviderException() {
        PublicKeyRegisterRequest request = generateRegisterRequest();
        when(ec2client.describeKeyPairs(any())).thenThrow(new AmazonServiceException("no such key"));
        when(ec2client.importKeyPair(any())).thenThrow(new RuntimeException(RUNTIME_EXCEPTION));
        assertThatThrownBy(() -> underTest.register(request)).isInstanceOf(CloudConnectorException.class).hasMessage(RUNTIME_EXCEPTION);
        verifyNoMoreInteractions(ec2client);
    }

    @Test
    void unregister() {
        PublicKeyUnregisterRequest request = generateUnregisterRequest();
        underTest.unregister(request);
        ArgumentCaptor<DeleteKeyPairRequest> captor = ArgumentCaptor.forClass(DeleteKeyPairRequest.class);
        verify(ec2client).deleteKeyPair(captor.capture());
        DeleteKeyPairRequest result = captor.getValue();
        assertThat(result.getKeyName()).isEqualTo(PUBLIC_KEY_ID);
        verifyNoMoreInteractions(ec2client);
    }

    @Test
    void unregisterNotFoundDoesNotThrow() {
        PublicKeyUnregisterRequest request = generateUnregisterRequest();
        when(ec2client.deleteKeyPair(any())).thenThrow(new AmazonServiceException("no such key"));
        underTest.unregister(request);
        ArgumentCaptor<DeleteKeyPairRequest> captor = ArgumentCaptor.forClass(DeleteKeyPairRequest.class);
        verify(ec2client).deleteKeyPair(captor.capture());
        DeleteKeyPairRequest result = captor.getValue();
        assertThat(result.getKeyName()).isEqualTo(PUBLIC_KEY_ID);
        verifyNoMoreInteractions(ec2client);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void platform() {
        assertThat(underTest.platform()).isEqualTo(AwsConstants.AWS_PLATFORM);
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    void variant() {
        assertThat(underTest.variant()).isEqualTo(AwsConstants.AWS_DEFAULT_VARIANT);
    }

    private PublicKeyRegisterRequest generateRegisterRequest() {
        CloudCredential credential = new CloudCredential("credId", "credName", "account");
        return PublicKeyRegisterRequest.builder()
                .withCloudPlatform("cloudPlatform")
                .withPublicKey(PUBLIC_KEY)
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withRegion("region")
                .withCredential(credential)
                .build();
    }

    private PublicKeyUnregisterRequest generateUnregisterRequest() {
        CloudCredential credential = new CloudCredential("credId", "credName", "account");
        return PublicKeyUnregisterRequest.builder()
                .withCloudPlatform("cloudPlatform")
                .withPublicKeyId(PUBLIC_KEY_ID)
                .withRegion("region")
                .withCredential(credential)
                .build();
    }
}
