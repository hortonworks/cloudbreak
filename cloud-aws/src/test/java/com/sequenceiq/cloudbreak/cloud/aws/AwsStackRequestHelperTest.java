package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

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
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;

public class AwsStackRequestHelperTest {

    @Mock
    private AwsTagPreparationService awsTagPreparationService;

    @Mock
    private AwsClient awsClient;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private Image image;

    @Mock
    private Network network;

    @Mock
    private DatabaseServer databaseServer;

    @Mock
    private Security security;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @InjectMocks
    private AwsStackRequestHelper underTest;

    @Before
    public void setUp() {
        initMocks(this);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);

        when(cloudStack.getImage()).thenReturn(image);
        when(cloudStack.getNetwork()).thenReturn(network);
        when(databaseStack.getNetwork()).thenReturn(network);
        when(databaseStack.getDatabaseServer()).thenReturn(databaseServer);

        when(databaseServer.getSecurity()).thenReturn(security);

        when(awsClient.createAccess(any(AwsCredentialView.class), anyString())).thenReturn(amazonEC2Client);
    }

    @Test
    public void testCreateCreateStackRequestForCloudStack() {
        when(cloudContext.getLocation()).thenReturn(Location.location(Region.region("region"), new AvailabilityZone("az")));
        DescribeImagesResult imagesResult = new DescribeImagesResult();
        when(amazonEC2Client.describeImages(any(DescribeImagesRequest.class)))
                .thenReturn(imagesResult.withImages(new com.amazonaws.services.ec2.model.Image()));
        when(network.getStringParameter(anyString())).thenReturn("");

        Collection<Tag> tags = Lists.newArrayList(new Tag().withKey("mytag").withValue("myvalue"));
        when(awsTagPreparationService.prepareCloudformationTags(authenticatedContext, cloudStack.getTags())).thenReturn(tags);

        CreateStackRequest createStackRequest =
                underTest.createCreateStackRequest(authenticatedContext, cloudStack, "stackName", "subnet", "template");

        assertEquals("stackName", createStackRequest.getStackName());
        assertEquals("template", createStackRequest.getTemplateBody());

        verify(awsTagPreparationService).prepareCloudformationTags(authenticatedContext, cloudStack.getTags());
        assertEquals(tags, createStackRequest.getTags());
    }

    @Test
    public void testCreateCreateStackRequestForDatabaseStack() {
        Collection<Tag> tags = Lists.newArrayList(new Tag().withKey("mytag").withValue("myvalue"));
        when(awsTagPreparationService.prepareCloudformationTags(authenticatedContext, databaseStack.getTags())).thenReturn(tags);

        CreateStackRequest createStackRequest =
                underTest.createCreateStackRequest(authenticatedContext, databaseStack, "stackName", "template");

        assertEquals("stackName", createStackRequest.getStackName());
        assertEquals("template", createStackRequest.getTemplateBody());

        verify(awsTagPreparationService).prepareCloudformationTags(authenticatedContext, cloudStack.getTags());
        assertEquals(tags, createStackRequest.getTags());
    }

    // TODO: test getStackParameters
}
