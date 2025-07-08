package com.sequenceiq.cloudbreak.cloud.aws.common.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.service.RetryService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AttachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.CreateVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVolumeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DetachVolumeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceMetadataOptionsRequest;
import software.amazon.awssdk.services.ec2.model.ModifyVolumeRequest;

class AmazonEc2ClientTest {

    private Ec2Client ec2Client;

    private AmazonEc2Client underTest;

    @BeforeEach
    void setUp() {
        ec2Client = mock(Ec2Client.class);
        Retry retry = new RetryService();
        underTest = new AmazonEc2Client(ec2Client, retry);
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testCreateVolumeRetriableErrorCodes(String errorCode) {
        CreateVolumeRequest createVolumeRequest = mock(CreateVolumeRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.createVolume(createVolumeRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.createVolume(createVolumeRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testDescribeSubnetsRetriableErrorCodes(String errorCode) {
        DescribeSubnetsRequest describeSubnetsRequest = mock(DescribeSubnetsRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.describeSubnets(describeSubnetsRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeSubnets(describeSubnetsRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testDescribeNetworkInterfacesRetriableErrorCodes(String errorCode) {
        DescribeNetworkInterfacesRequest describeNetworkInterfacesRequest = mock(DescribeNetworkInterfacesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.describeNetworkInterfaces(describeNetworkInterfacesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeNetworkInterfaces(describeNetworkInterfacesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testModifyInstanceAttributeRetriableErrorCodes(String errorCode) {
        ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = mock(ModifyInstanceAttributeRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.modifyInstanceAttribute(modifyInstanceAttributeRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.modifyInstanceAttribute(modifyInstanceAttributeRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testDeleteVolumeRetriableErrorCodes(String errorCode) {
        DeleteVolumeRequest deleteVolumeRequest = mock(DeleteVolumeRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.deleteVolume(deleteVolumeRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.deleteVolume(deleteVolumeRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testDescribeVolumesRetriableErrorCodes(String errorCode) {
        DescribeVolumesRequest describeVolumesRequest = mock(DescribeVolumesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.describeVolumes(describeVolumesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeVolumes(describeVolumesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testAttachVolumeRetriableErrorCodes(String errorCode) {
        AttachVolumeRequest attachVolumeRequest = mock(AttachVolumeRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.attachVolume(attachVolumeRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.attachVolume(attachVolumeRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testDetachVolumeRetriableErrorCodes(String errorCode) {
        DetachVolumeRequest detachVolumeRequest = mock(DetachVolumeRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.detachVolume(detachVolumeRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.detachVolume(detachVolumeRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testModifyVolumeRetriableErrorCodes(String errorCode) {
        ModifyVolumeRequest modifyVolumeRequest = mock(ModifyVolumeRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.modifyVolume(modifyVolumeRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.modifyVolume(modifyVolumeRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testDescribeVolumesModificationsRetriableErrorCodes(String errorCode) {
        DescribeVolumesModificationsRequest describeVolumesModificationsRequest = mock(DescribeVolumesModificationsRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.describeVolumesModifications(describeVolumesModificationsRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () ->
                underTest.describeVolumeModifications(describeVolumesModificationsRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"RequestExpired", "IncorrectInstanceState", "IncorrectState", "IncorrectStateException",
            "InvalidHostState", "InvalidState", "VolumeInUse", "ServerInternal", "InternalFailure", "ServiceUnavailable", "InternalError", "Unavailable"})
    void testModifyInstanceMetadataOptionsRequestRetriableErrorCodes(String errorCode) {
        ModifyInstanceMetadataOptionsRequest modifyInstanceMetadataOptionsRequest = mock(ModifyInstanceMetadataOptionsRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(ec2Client.modifyInstanceMetadataOptions(modifyInstanceMetadataOptionsRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () ->
                underTest.modifyInstanceMetadataOptions(modifyInstanceMetadataOptionsRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }
}
