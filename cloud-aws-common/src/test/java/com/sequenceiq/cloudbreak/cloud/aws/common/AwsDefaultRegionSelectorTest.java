package com.sequenceiq.cloudbreak.cloud.aws.common;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsDefaultRegionSelector.EC2_AUTH_FAILURE_ERROR_CODE;
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

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeRegionsRequest;
import com.amazonaws.services.ec2.model.DescribeRegionsResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsDefaultRegionSelectionFailed;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Region;

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
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenThrow(new AmazonEC2Exception("SomethingBadHappened"));

        assertThrows(AmazonEC2Exception.class, () -> underTest.determineDefaultRegion(new CloudCredential()));

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
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("SomethingBadHappened");
        amazonEC2Exception.setErrorCode(EC2_AUTH_FAILURE_ERROR_CODE);
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
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("SomethingBadHappened");
        amazonEC2Exception.setErrorCode(EC2_AUTH_FAILURE_ERROR_CODE);
        when(ec2Client.describeRegions(any(DescribeRegionsRequest.class))).thenReturn(new DescribeRegionsResult());
        when(awsClient.createAccessWithMinimalRetries(any(AwsCredentialView.class), any()))
                .thenThrow(amazonEC2Exception)
                .thenThrow(amazonEC2Exception)
                .thenThrow(amazonEC2Exception)
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
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("SomethingBadHappened");
        amazonEC2Exception.setErrorCode(EC2_AUTH_FAILURE_ERROR_CODE);
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
}