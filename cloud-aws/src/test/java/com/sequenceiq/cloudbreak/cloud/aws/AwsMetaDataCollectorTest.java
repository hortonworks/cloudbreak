package com.sequenceiq.cloudbreak.cloud.aws;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;


@RunWith(MockitoJUnitRunner.class)
public class AwsMetaDataCollectorTest {

    @Mock
    private AwsClient awsClient;

    @Mock
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Mock
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private AmazonCloudFormationClient amazonCFClient;

    @Mock
    private AmazonAutoScalingClient amazonASClient;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @Mock
    private DescribeInstancesRequest describeInstancesRequestGw;

    @Mock
    private DescribeInstancesRequest describeInstancesRequestMaster;

    @Mock
    private DescribeInstancesRequest describeInstancesRequestSlave;

    @Mock
    private DescribeInstancesResult describeInstancesResultGw;

    @Mock
    private DescribeInstancesResult describeInstancesResultMaster;

    @Mock
    private DescribeInstancesResult describeInstancesResultSlave;

    @InjectMocks
    private AwsMetadataCollector awsMetadataCollector;

    @Before
    public void setUp() {
        Mockito.reset(amazonEC2Client);
    }

    @Test
    public void collectMigratedExistingOneGroup() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        vms.add(new CloudInstance("i-1", new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)));


        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonEC2Client);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance = Mockito.mock(Instance.class);
        when(instance.getInstanceId()).thenReturn("i-1");
        when(instance.getPrivateIpAddress()).thenReturn("privateIp");
        when(instance.getPublicIpAddress()).thenReturn("publicIp");

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, null, vms);

        verify(amazonEC2Client).createTags(any(CreateTagsRequest.class));
        Assert.assertEquals(1, statuses.size());
        Assert.assertEquals("i-1", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        Assert.assertEquals("privateIp", statuses.get(0).getMetaData().getPrivateIp());
        Assert.assertEquals("publicIp", statuses.get(0).getMetaData().getPublicIp());
    }

    @Test
    public void collectAlreadyTaggedOneGroup() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        vms.add(new CloudInstance("i-1", new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)));


        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonEC2Client);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance = Mockito.mock(Instance.class);
        when(instance.getInstanceId()).thenReturn("i-1");
        when(instance.getPrivateIpAddress()).thenReturn("privateIp");
        when(instance.getPublicIpAddress()).thenReturn("publicIp");
        Tag tag = new Tag();
        tag.setKey("cbname");
        tag.setValue("somevalue");
        when(instance.getTags()).thenReturn(Collections.singletonList(tag));

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, null, vms);

        verify(amazonEC2Client, never()).createTags(any(CreateTagsRequest.class));
        Assert.assertEquals(1, statuses.size());
        Assert.assertEquals("i-1", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        Assert.assertEquals("privateIp", statuses.get(0).getMetaData().getPrivateIp());
        Assert.assertEquals("publicIp", statuses.get(0).getMetaData().getPublicIp());
    }

    @Test
    public void collectNewOneGroup() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        vms.add(new CloudInstance(null, new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)));


        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonEC2Client);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Collections.singletonList("i-1");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance = Mockito.mock(Instance.class);
        when(instance.getInstanceId()).thenReturn("i-1");
        when(instance.getPrivateIpAddress()).thenReturn("privateIp");
        when(instance.getPublicIpAddress()).thenReturn("publicIp");

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, null, vms);

        verify(amazonEC2Client).createTags(any(CreateTagsRequest.class));
        Assert.assertEquals(1, statuses.size());
        Assert.assertEquals("i-1", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        Assert.assertEquals("privateIp", statuses.get(0).getMetaData().getPrivateIp());
        Assert.assertEquals("publicIp", statuses.get(0).getMetaData().getPublicIp());
    }

    @Test
    public void collectNewAndExistingOneGroup() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        vms.add(new CloudInstance(null, new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)));
        vms.add(new CloudInstance("i-1", new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)));


        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonEC2Client);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Arrays.asList("i-1", "i-2");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance1 = Mockito.mock(Instance.class);
        when(instance1.getInstanceId()).thenReturn("i-1");
        when(instance1.getPrivateIpAddress()).thenReturn("privateIp1");
        when(instance1.getPublicIpAddress()).thenReturn("publicIp1");

        Instance instance2 = Mockito.mock(Instance.class);
        when(instance2.getInstanceId()).thenReturn("i-2");
        when(instance2.getPrivateIpAddress()).thenReturn("privateIp2");
        when(instance2.getPublicIpAddress()).thenReturn("publicIp2");

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance1, instance2));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, null, vms);

        verify(amazonEC2Client, times(2)).createTags(any(CreateTagsRequest.class));
        Assert.assertEquals(2, statuses.size());
        Assert.assertEquals("i-1", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        Assert.assertEquals("privateIp1", statuses.get(0).getMetaData().getPrivateIp());
        Assert.assertEquals("publicIp1", statuses.get(0).getMetaData().getPublicIp());

        Assert.assertEquals("i-2", statuses.get(1).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        Assert.assertEquals("privateIp2", statuses.get(1).getMetaData().getPrivateIp());
        Assert.assertEquals("publicIp2", statuses.get(1).getMetaData().getPublicIp());
    }

    @Test
    public void collectNewOldIsTagged() {
        List<CloudInstance> vms = new ArrayList<>();
        List<Volume> volumes = new ArrayList<>();
        vms.add(new CloudInstance(null, new InstanceTemplate("fla", "cbgateway", 5L, volumes, InstanceStatus.CREATED, null)));

        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonCFClient);
        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonASClient);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("region"))).thenReturn(amazonEC2Client);

        when(cloudFormationStackUtil.getAutoscalingGroupName(any(AuthenticatedContext.class), any(AmazonCloudFormationClient.class), eq("cbgateway")))
                .thenReturn("cbgateway-AAA");

        List<String> gatewayIds = Arrays.asList("i-1", "i-new");
        when(cloudFormationStackUtil.getInstanceIds(any(AmazonAutoScalingClient.class), eq("cbgateway-AAA")))
                .thenReturn(gatewayIds);

        when(cloudFormationStackUtil.createDescribeInstancesRequest(eq(gatewayIds))).thenReturn(describeInstancesRequestGw);

        when(amazonEC2Client.describeInstances(describeInstancesRequestGw)).thenReturn(describeInstancesResultGw);

        Instance instance1 = Mockito.mock(Instance.class);
        when(instance1.getInstanceId()).thenReturn("i-1");
        when(instance1.getPrivateIpAddress()).thenReturn("privateIp1");
        when(instance1.getPublicIpAddress()).thenReturn("publicIp1");
        Tag tag = new Tag();
        tag.setKey("cbname");
        tag.setValue("somevalue");
        when(instance1.getTags()).thenReturn(Collections.singletonList(tag));

        Instance instance2 = Mockito.mock(Instance.class);
        when(instance2.getInstanceId()).thenReturn("i-new");
        when(instance2.getPrivateIpAddress()).thenReturn("privateIp2");
        when(instance2.getPublicIpAddress()).thenReturn("publicIp2");

        List<Reservation> gatewayReservations = Collections.singletonList(getReservation(instance1, instance2));

        when(describeInstancesResultGw.getReservations()).thenReturn(gatewayReservations);

        AuthenticatedContext ac = authenticatedContext();
        List<CloudVmMetaDataStatus> statuses = awsMetadataCollector.collect(ac, null, vms);

        verify(amazonEC2Client, times(1)).createTags(any(CreateTagsRequest.class));
        Assert.assertEquals(1, statuses.size());
        Assert.assertEquals("i-new", statuses.get(0).getCloudVmInstanceStatus().getCloudInstance().getInstanceId());
        Assert.assertEquals("privateIp2", statuses.get(0).getMetaData().getPrivateIp());
        Assert.assertEquals("publicIp2", statuses.get(0).getMetaData().getPublicIp());
    }

    private Reservation getReservation(Instance... instance) {
        List<Instance> instances = Arrays.asList(instance);
        Reservation r = new Reservation();
        r.setInstances(instances);
        return r;
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "owner", "variant", location);
        CloudCredential cc = new CloudCredential(1L, null, null, null);
        return new AuthenticatedContext(cloudContext, cc);
    }
}
