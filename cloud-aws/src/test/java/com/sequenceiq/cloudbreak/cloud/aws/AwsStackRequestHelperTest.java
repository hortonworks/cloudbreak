package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@RunWith(MockitoJUnitRunner.class)
public class AwsStackRequestHelperTest {

    @Mock
    private AwsTagPreparationService awsTagPreparationService;

    @Mock
    private AwsClient awsClient;

    @InjectMocks
    private AwsStackRequestHelper underTest;

    @Test
    public void testTegPreparationIsCalled() {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        Image image = mock(Image.class);
        CloudContext cloudContext = mock(CloudContext.class);
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        Network network = mock(Network.class);

        when(cloudStack.getImage()).thenReturn(image);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region"), new AvailabilityZone("az")));
        when(awsClient.createAccess(any(AwsCredentialView.class), anyString())).thenReturn(amazonEC2Client);
        DescribeImagesResult imagesResult = new DescribeImagesResult();
        when(amazonEC2Client.describeImages(any(DescribeImagesRequest.class)))
                .thenReturn(imagesResult.withImages(new com.amazonaws.services.ec2.model.Image()));
        when(cloudStack.getNetwork()).thenReturn(network);
        when(network.getStringParameter(anyString())).thenReturn("");
        when(awsTagPreparationService.prepareCloudformationTags(authenticatedContext, cloudStack.getTags())).thenReturn(Lists.newArrayList(new Tag()));

        CreateStackRequest createStackRequest =
                underTest.createCreateStackRequest(authenticatedContext, cloudStack, "stackName", "subnet", "template");

        verify(awsTagPreparationService).prepareCloudformationTags(authenticatedContext, cloudStack.getTags());
        Assert.assertFalse(createStackRequest.getTags().isEmpty());
    }
}