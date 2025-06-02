package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.AUTH_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.OPT_IN_REQUIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.UNAUTHORIZED_OPERATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsDefaultRegionSelectionFailed;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;

@ExtendWith(MockitoExtension.class)
class AwsDefaultRegionSelectorTest {

    private static final String GLOBAL_DEFAULT_ZONE = "us-east-1";

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private AwsPlatformResources platformResources;

    @Mock
    private CommonAwsClient awsClient;

    @Mock
    private AwsDefaultZoneProvider defaultZoneProvider;

    @InjectMocks
    private AwsDefaultRegionSelector underTest;

    @BeforeEach
    void setUp() {
        Set<Region> possibleDefaultRegions = Set.of(Region.region("sa-east-1"), Region.region("eu-central-1"), Region.region("eu-north-1"));
        when(platformResources.getEnabledRegions()).thenReturn(possibleDefaultRegions);
        when(defaultZoneProvider.getDefaultZone(any(CloudCredential.class))).thenReturn(GLOBAL_DEFAULT_ZONE);
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionIsViable() {
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE))).thenReturn(ec2Client);

        String actual = underTest.determineDefaultRegion(new CloudCredential());

        assertNull(actual);
        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionDescribeFailsWithAwsClientException() {
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE))).thenReturn(ec2Client);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class)))
                .thenThrow(Ec2Exception.builder().awsErrorDetails(AwsErrorDetails.builder().errorCode("SomethingBadHappened").build()).build());

        assertThrows(Ec2Exception.class, () -> underTest.determineDefaultRegion(new CloudCredential()));

        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionDescribeFailsWithRuntimeException() {
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE))).thenReturn(ec2Client);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> underTest.determineDefaultRegion(new CloudCredential()));

        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionDescribeFailsWithAuthExceptionAndNoAdditionalRegionsAreConfigured() {
        Ec2Exception amazonEC2Exception = getEc2Exception("SomethingBadHappened", AUTH_FAILURE);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenThrow(amazonEC2Exception);
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE))).thenReturn(ec2Client);
        when(platformResources.getEnabledRegions()).thenReturn(null);

        AwsDefaultRegionSelectionFailed ex = assertThrows(AwsDefaultRegionSelectionFailed.class, () -> underTest.determineDefaultRegion(new CloudCredential()));

        assertEquals(String.format("Failed to describe available EC2 regions in region '%s'", GLOBAL_DEFAULT_ZONE), ex.getMessage());
        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionDescribeFailsWithUnAuthorizedOperationAndNoAdditionalRegionsAreConfigured() {
        Ec2Exception amazonEC2Exception = getEc2Exception("You are not authorized to perform this operation.", UNAUTHORIZED_OPERATION);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenThrow(amazonEC2Exception);
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE))).thenReturn(ec2Client);
        when(platformResources.getEnabledRegions()).thenReturn(null);

        AwsDefaultRegionSelectionFailed ex = assertThrows(AwsDefaultRegionSelectionFailed.class, () -> underTest.determineDefaultRegion(new CloudCredential()));

        assertEquals(String.format("Failed to describe available EC2 regions in region '%s'", GLOBAL_DEFAULT_ZONE), ex.getMessage());
        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionDescribeFailsWithNotOptInOperationAndNoAdditionalRegionsAreConfigured() {
        Ec2Exception amazonEC2Exception = getEc2Exception("You are not authorized to use the requested service", OPT_IN_REQUIRED);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenThrow(amazonEC2Exception);
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE))).thenReturn(ec2Client);
        when(platformResources.getEnabledRegions()).thenReturn(null);

        AwsDefaultRegionSelectionFailed ex = assertThrows(AwsDefaultRegionSelectionFailed.class, () -> underTest.determineDefaultRegion(new CloudCredential()));

        assertEquals(String.format("Failed to describe available EC2 regions in region '%s'", GLOBAL_DEFAULT_ZONE), ex.getMessage());
        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(GLOBAL_DEFAULT_ZONE));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionIsNotViableAndOneOfTheAdditionalRegionsIsViable() {
        Ec2Exception authFailureException = getEc2Exception("SomethingBadHappened", AUTH_FAILURE);
        Ec2Exception unauthorizedOperation = getEc2Exception("You are not authorized to perform this operation.", UNAUTHORIZED_OPERATION);
        Ec2Exception optInRequiredException = getEc2Exception("You are not authorized to use the requested service", OPT_IN_REQUIRED);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenReturn(DescribeRegionsResponse.builder().build());
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), any()))
                .thenThrow(authFailureException)
                .thenThrow(optInRequiredException)
                .thenThrow(unauthorizedOperation)
                .thenReturn(ec2Client);

        String actual = underTest.determineDefaultRegion(new CloudCredential());

        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(awsClient, times(4)).createAccessWithMinimalRetries(any(AwsCredentialView.class), captor.capture());
        final String expected = captor.getValue();
        assertEquals(expected, actual);
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionWhenGlobalDefaultRegionIsNotViableAndNoneOfTheAdditionalRegionsIsViable() {
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), any())).thenReturn(ec2Client);
        Ec2Exception amazonEC2Exception = getEc2Exception("SomethingBadHappened", AUTH_FAILURE);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenThrow(amazonEC2Exception);

        AwsDefaultRegionSelectionFailed exception = assertThrows(AwsDefaultRegionSelectionFailed.class,
                () -> underTest.determineDefaultRegion(new CloudCredential()));

        assertTrue(exception.getMessage().startsWith("Failed to describe available EC2 regions by configuring SDK to use the following regions:"));
        verify(awsClient, times(4)).createAccessWithMinimalRetries(any(AwsCredentialView.class), any());
        verify(ec2Client, times(4)).describeRegions(any(DescribeRegionsRequest.class));
    }

    @Test
    void testDetermineDefaultRegionShouldReturnNullWhenCredentialSpecificDefaultRegionIsViable() {
        String credentialSpecificDefaultRegion = "eu-central-1";
        CloudCredential aCloudCredential = new CloudCredential();
        when(defaultZoneProvider.getDefaultZone(aCloudCredential)).thenReturn(credentialSpecificDefaultRegion);
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(credentialSpecificDefaultRegion))).thenReturn(ec2Client);

        String actual = underTest.determineDefaultRegion(aCloudCredential);

        assertNull(actual);
        verify(awsClient, times(1)).createAccessWithMinimalRetries(any(AwsCredentialView.class), eq(credentialSpecificDefaultRegion));
        verify(ec2Client, times(1)).describeRegions(any(DescribeRegionsRequest.class));
    }

    private static Ec2Exception getEc2Exception(String message, String errorCode) {
        return (Ec2Exception) Ec2Exception.builder()
                .message(message)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).build())
                .build();
    }
}
