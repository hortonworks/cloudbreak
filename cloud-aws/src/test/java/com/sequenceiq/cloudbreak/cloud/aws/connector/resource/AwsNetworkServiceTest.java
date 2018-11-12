package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.google.common.net.InetAddresses;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTagPreparationService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;

@RunWith(MockitoJUnitRunner.class)
public class AwsNetworkServiceTest {

    private static final int ROOT_VOLUME_SIZE = 50;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AwsNetworkService underTest;

    @Mock
    private AwsClient awsClient;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @Mock
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Mock
    private AwsTagPreparationService awsTagPreparationService;

    @Test
    public void testFindNonOverLappingCIDR() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();

        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet5 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet6 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 100}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/16");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4, subnet5, subnet6));
        when(subnet1.getCidrBlock()).thenReturn("10.0.1.0/24");
        when(subnet2.getCidrBlock()).thenReturn("10.0.2.0/24");
        when(subnet3.getCidrBlock()).thenReturn("10.0.3.0/24");
        when(subnet4.getCidrBlock()).thenReturn("10.0.5.0/24");
        when(subnet5.getCidrBlock()).thenReturn("10.0.6.0/24");
        when(subnet6.getCidrBlock()).thenReturn("10.0.255.0/24");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.100.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWithNon24Subnets() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 23}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/16");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(subnet2.getCidrBlock()).thenReturn("10.0.16.0/20");
        when(subnet3.getCidrBlock()).thenReturn("10.0.32.0/20");
        when(subnet4.getCidrBlock()).thenReturn("10.0.48.0/24");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.49.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWithNon24Subnets2() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 76}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/16");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(subnet2.getCidrBlock()).thenReturn("10.0.16.0/20");
        when(subnet3.getCidrBlock()).thenReturn("10.0.32.0/20");
        when(subnet4.getCidrBlock()).thenReturn("10.0.48.0/20");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.76.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWithNon24Subnets3() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 15}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/16");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(subnet2.getCidrBlock()).thenReturn("10.0.16.0/20");
        when(subnet3.getCidrBlock()).thenReturn("10.0.32.0/20");
        when(subnet4.getCidrBlock()).thenReturn("10.0.48.0/20");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.64.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWit24Vpc() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/24");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(singletonList(subnet1));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/24");

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("The selected VPC has to be in a bigger CIDR range than /24");

        underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);
    }

    @Test
    public void testFindNonOverLappingCIDRWit24VpcEmptySubnet() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/24");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Collections.emptyList());

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("The selected VPC has to be in a bigger CIDR range than /24");

        underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);
    }

    @Test
    public void testFindNonOverLappingCIDRWit20Vpc() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 15}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/24");
        when(subnet2.getCidrBlock()).thenReturn("10.0.1.0/24");
        when(subnet3.getCidrBlock()).thenReturn("10.0.2.0/24");
        when(subnet4.getCidrBlock()).thenReturn("10.0.3.0/24");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.15.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWit20Vpc2() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 16}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/24");
        when(subnet2.getCidrBlock()).thenReturn("10.0.1.0/24");
        when(subnet3.getCidrBlock()).thenReturn("10.0.2.0/24");
        when(subnet4.getCidrBlock()).thenReturn("10.0.3.0/24");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.4.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWit20VpcFull() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet5 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet6 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet7 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet8 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 15}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4, subnet5, subnet6, subnet7, subnet8));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/23");
        when(subnet2.getCidrBlock()).thenReturn("10.0.2.0/23");
        when(subnet3.getCidrBlock()).thenReturn("10.0.4.0/23");
        when(subnet4.getCidrBlock()).thenReturn("10.0.6.0/23");
        when(subnet5.getCidrBlock()).thenReturn("10.0.8.0/23");
        when(subnet6.getCidrBlock()).thenReturn("10.0.10.0/23");
        when(subnet7.getCidrBlock()).thenReturn("10.0.12.0/23");
        when(subnet8.getCidrBlock()).thenReturn("10.0.14.0/23");

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Cannot find non-overlapping CIDR range");

        underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);
    }

    @Test
    public void testFindNonOverLappingCIDRWit20Vpc1Empty() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet5 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet6 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet7 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet8 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 127, (byte) 127, (byte) 127, (byte) 127, (byte) 127, (byte) 127, (byte) 83}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4, subnet5, subnet6, subnet7, subnet8));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/23");
        when(subnet2.getCidrBlock()).thenReturn("10.0.2.0/23");
        when(subnet3.getCidrBlock()).thenReturn("10.0.4.0/23");
        when(subnet4.getCidrBlock()).thenReturn("10.0.6.0/23");
        when(subnet5.getCidrBlock()).thenReturn("10.0.8.0/23");
        when(subnet6.getCidrBlock()).thenReturn("10.0.10.0/23");
        when(subnet7.getCidrBlock()).thenReturn("10.0.12.0/23");
        when(subnet8.getCidrBlock()).thenReturn("10.0.14.0/24");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.15.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWit20Vpc1Empty2() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet5 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet6 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet7 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet8 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 4}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4, subnet5, subnet6, subnet7, subnet8));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/23");
        when(subnet2.getCidrBlock()).thenReturn("10.0.2.0/24");
        when(subnet3.getCidrBlock()).thenReturn("10.0.4.0/23");
        when(subnet4.getCidrBlock()).thenReturn("10.0.6.0/23");
        when(subnet5.getCidrBlock()).thenReturn("10.0.8.0/23");
        when(subnet6.getCidrBlock()).thenReturn("10.0.10.0/23");
        when(subnet7.getCidrBlock()).thenReturn("10.0.12.0/23");
        when(subnet8.getCidrBlock()).thenReturn("10.0.14.0/23");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.3.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRWit20Vpc1EmptyInTheMiddle() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        com.amazonaws.services.ec2.model.Subnet subnet1 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet2 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet3 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet4 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet5 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet6 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet7 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        com.amazonaws.services.ec2.model.Subnet subnet8 = mock(com.amazonaws.services.ec2.model.Subnet.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 127, (byte) 127, (byte) 127, (byte) 127}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/20");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);
        when(subnetsResult.getSubnets()).thenReturn(Arrays.asList(subnet1, subnet2, subnet3, subnet4, subnet5, subnet6, subnet7, subnet8));
        when(subnet1.getCidrBlock()).thenReturn("10.0.0.0/23");
        when(subnet2.getCidrBlock()).thenReturn("10.0.2.0/23");
        when(subnet3.getCidrBlock()).thenReturn("10.0.4.0/23");
        when(subnet4.getCidrBlock()).thenReturn("10.0.6.0/23");
        when(subnet5.getCidrBlock()).thenReturn("10.0.8.0/23");
        when(subnet6.getCidrBlock()).thenReturn("10.0.10.0/23");
        when(subnet7.getCidrBlock()).thenReturn("10.0.12.0/24");
        when(subnet8.getCidrBlock()).thenReturn("10.0.14.0/23");

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("10.0.13.0/24", cidr);
    }

    @Test
    public void testFindNonOverLappingCIDRForFullVpc() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn(new String(new byte[]{(byte) 7}));
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("10.0.0.0/16");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);

        List<com.amazonaws.services.ec2.model.Subnet> subnetList = new ArrayList<>();
        String startRange = "10.0.0.0";
        for (int i = 0; i < 255; i++) {
            startRange = incrementIp(startRange);
            com.amazonaws.services.ec2.model.Subnet subnetMock = mock(com.amazonaws.services.ec2.model.Subnet.class);
            when(subnetMock.getCidrBlock()).thenReturn(startRange + "/24");
            subnetList.add(subnetMock);
        }
        when(subnetsResult.getSubnets()).thenReturn(subnetList);

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage("Cannot find non-overlapping CIDR range");

        underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);
    }

    @Test
    public void testFindNonOverLappingCIDRForOneSpot() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");

        Group group1 = new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE);
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(null), networkParameters);
        CloudStack cloudStack = new CloudStack(singletonList(group1), network, null, emptyMap(), emptyMap(), null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), null);
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudContext cloudContext = mock(CloudContext.class);
        Location location = mock(Location.class);
        Vpc vpc = mock(Vpc.class);
        DescribeVpcsResult describeVpcsResult = mock(DescribeVpcsResult.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        DescribeSubnetsResult subnetsResult = mock(DescribeSubnetsResult.class);

        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location);
        when(cloudContext.getName()).thenReturn("");
        when(location.getRegion()).thenReturn(Region.region("eu-west-1"));
        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(ec2Client.describeVpcs(any())).thenReturn(describeVpcsResult);
        when(describeVpcsResult.getVpcs()).thenReturn(singletonList(vpc));
        when(vpc.getCidrBlock()).thenReturn("172.14.0.0/16");
        when(ec2Client.describeSubnets(any())).thenReturn(subnetsResult);

        List<com.amazonaws.services.ec2.model.Subnet> subnetList = new ArrayList<>();
        String startRange = "172.14.0.0";
        for (int i = 0; i < 254; i++) {
            startRange = incrementIp(startRange);
            com.amazonaws.services.ec2.model.Subnet subnetMock = mock(com.amazonaws.services.ec2.model.Subnet.class);
            when(subnetMock.getCidrBlock()).thenReturn(startRange + "/24");
            subnetList.add(subnetMock);
        }
        when(subnetsResult.getSubnets()).thenReturn(subnetList);

        String cidr = underTest.findNonOverLappingCIDR(authenticatedContext, cloudStack);

        Assert.assertEquals("172.14.255.0/24", cidr);
    }

    private String incrementIp(String ip) {
        int ipValue = InetAddresses.coerceToInteger(InetAddresses.forString(ip)) + 256;
        return InetAddresses.fromInteger(ipValue).getHostAddress();
    }

}
