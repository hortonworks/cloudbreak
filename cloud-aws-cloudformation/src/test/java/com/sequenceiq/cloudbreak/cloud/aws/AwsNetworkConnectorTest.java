package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcCidrBlockAssociation;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSubnetRequestProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyType;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.Tunnel;

@RunWith(MockitoJUnitRunner.class)
public class AwsNetworkConnectorTest {

    private static final String ENV_NAME = "testEnv";

    private static final String STACK_NAME = ENV_NAME + "-1";

    private static final String VPC_ID = "newVpc";

    private static final String SUBNET_ID_0 = "subnet-0";

    private static final String SUBNET_ID_1 = "subnet-1";

    private static final String SUBNET_ID_2 = "subnet-1";

    private static final String CF_TEMPLATE = "template";

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET_0 = "CreatedSubnets0";

    private static final String CREATED_SUBNET_1 = "CreatedSubnets1";

    private static final String CREATED_SUBNET_2 = "CreatedSubnets2";

    private static final int NUMBER_OF_SUBNETS = 3;

    private static final long ID = 1L;

    private static final String NETWORK_ID = String.join("-", ENV_NAME, String.valueOf(ID));

    private static final Region REGION = Region.region("US_WEST_2");

    private static final String ENV_CRN = "someCrn";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AwsNetworkConnector underTest;

    @Mock
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private LegacyAwsClient awsClient;

    @Mock
    private AmazonCloudFormationWaiters cfWaiters;

    @Mock
    private Waiter<DescribeStacksRequest> creationWaiter;

    @Mock
    private Waiter<DescribeStacksRequest> deletionWaiter;

    @Mock
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @Mock
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private Map<SubnetFilterStrategyType, SubnetFilterStrategy> subnetFilterStrategyMap;

    @Mock
    private SubnetFilterStrategy subnetFilterStrategy;

    @Before
    public void before() {
        ReflectionTestUtils.setField(underTest, "minSubnetCountInDifferentAz", 2);
        ReflectionTestUtils.setField(underTest, "maxSubnetCountInDifferentAz", 3);
    }

    @Test
    public void testPlatformShouldReturnAwsPlatform() {
        Platform actual = underTest.platform();

        assertEquals(AwsConstants.AWS_PLATFORM, actual);
    }

    @Test
    public void testVariantShouldReturnAwsPlatform() {
        Variant actual = underTest.variant();

        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, actual);
    }

    @Test
    public void testCreateNetworkWithSubnetsShouldReturnTheNetworkAndSubnets() {
        String networkCidr = "0.0.0.0/16";
        Set<NetworkSubnetRequest> subnets = Set.of(new NetworkSubnetRequest("1.1.1.1/8", PUBLIC), new NetworkSubnetRequest("1.1.1.2/8", PUBLIC));
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        Map<String, String> output = createOutput();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(networkCidr, subnets);
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        Set<CreatedSubnet> createdSubnets = Set.of(new CreatedSubnet(), new CreatedSubnet(), new CreatedSubnet());

        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        when(awsSubnetRequestProvider.provide(ec2Client, new ArrayList<>(subnets), new ArrayList<>(subnets))).thenReturn(subnetRequestList);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cfClient);

        when(cfClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackCreateComplete()).thenReturn(creationWaiter);
        when(cfStackUtil.getOutputs(NETWORK_ID, cfClient)).thenReturn(output);
        when(awsCreatedSubnetProvider.provide(output, subnetRequestList, true)).thenReturn(createdSubnets);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(creationWaiter, times(1)).run(any());
        verify(cfStackUtil).getOutputs(NETWORK_ID, cfClient);
        verify(awsTaggingService, never()).prepareCloudformationTags(any(), any());
        verify(cfClient, never()).createStack(any(CreateStackRequest.class));
        assertEquals(VPC_ID, actual.getNetworkId());
        assertEquals(NUMBER_OF_SUBNETS, actual.getSubnets().size());
    }

    @Test
    public void testCreateNewNetworkWithSubnetsShouldCreateTheNetworkAndSubnets() {
        String networkCidr = "0.0.0.0/16";
        Set<NetworkSubnetRequest> subnets = Set.of(new NetworkSubnetRequest("1.1.1.1/8", PUBLIC), new NetworkSubnetRequest("1.1.1.2/8", PUBLIC));
        AmazonServiceException amazonServiceException = new AmazonServiceException("does not exist");
        amazonServiceException.setStatusCode(400);
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        when(cfClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(amazonServiceException);
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        Map<String, String> output = createOutput();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(networkCidr, subnets);
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        Set<CreatedSubnet> createdSubnets = Set.of(new CreatedSubnet(), new CreatedSubnet(), new CreatedSubnet());

        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);
        when(awsSubnetRequestProvider.provide(ec2Client, new ArrayList<>(subnets), new ArrayList<>(subnets))).thenReturn(subnetRequestList);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cfClient);
        when(cfClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackCreateComplete()).thenReturn(creationWaiter);
        when(cfStackUtil.getOutputs(NETWORK_ID, cfClient)).thenReturn(output);
        when(awsCreatedSubnetProvider.provide(output, subnetRequestList, true)).thenReturn(createdSubnets);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsNetworkCfTemplateProvider).provide(networkCreationRequest, subnetRequestList);
        verify(creationWaiter, times(1)).run(any());
        verify(awsTaggingService).prepareCloudformationTags(any(), any());
        verify(cfClient).createStack(any(CreateStackRequest.class));
        verify(cfStackUtil).getOutputs(NETWORK_ID, cfClient);
        assertEquals(VPC_ID, actual.getNetworkId());
        assertEquals(NUMBER_OF_SUBNETS, actual.getSubnets().size());
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldDeleteTheStackAndTheResourceGroup() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(networkDeletionRequest.getRegion())))
                .thenReturn(cfClient);
        when(cfClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackDeleteComplete()).thenReturn(deletionWaiter);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(cfClient).deleteStack(any(DeleteStackRequest.class));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(deletionWaiter, times(1)).run(any());
    }

    @Test(expected = CloudConnectorException.class)
    public void testDeleteNetworkWithSubNetsShouldThrowAnExceptionWhenTheStackDeletionFailed()
            throws InterruptedException, ExecutionException, TimeoutException {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(networkDeletionRequest.getRegion())))
                .thenReturn(cfClient);
        when(cfClient.waiters()).thenReturn(cfWaiters);
        when(cfWaiters.stackDeleteComplete()).thenReturn(deletionWaiter);
        doThrow(new WaiterTimedOutException("fail")).when(deletionWaiter).run(any());

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(cfClient).deleteStack(any(DeleteStackRequest.class));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
    }

    @Test
    public void testGetNetworkCidr() {
        String existingVpc = "vpc-1";
        String cidrBlock = "10.0.0.0/16";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResult describeVpcsResult = describeVpcsResult(cidrBlock);

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        NetworkCidr result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock, result.getCidr());
    }

    @Test
    public void testGetNetworkCidrWithoutResult() {
        String existingVpc = "vpc-1";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResult describeVpcsResult = describeVpcsResult();

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("VPC cidr could not fetch from AWS: " + existingVpc);

        underTest.getNetworkCidr(network, credential);
    }

    @Test
    public void testGetNetworkCidrMoreThanOneAssociatedCidrOnOneVpcShouldReturn2Cidr() {
        String existingVpc = "vpc-1";
        String cidrBlock1 = "10.0.0.0/16";
        String cidrBlock2 = "10.23.0.0/16";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResult describeVpcsResult = describeVpcsResult(cidrBlock1, cidrBlock2);

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        NetworkCidr result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock1, result.getCidr());
        assertTrue(result.getCidrs().contains(cidrBlock1));
        assertTrue(result.getCidrs().contains(cidrBlock2));
    }

    @Test
    public void testSubnetSelectionWhenHaRequiredAnd2DifferentAZDeclaredShouldReturn2DifferentAz() {
        List<CloudSubnet> cloudSubnets = Lists.newArrayList(
                getSubnet("a1", 1),
                getSubnet("a1", 2),
                getSubnet("a2", 3),
                getSubnet("a2", 4));

        prepareMock(cloudSubnets);

        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withPreferPrivateIfExist()
                .withTunnel(Tunnel.CCM)
                .withHa(true)
                .build();
        SubnetSelectionResult result = underTest.chooseSubnets(cloudSubnets, subnetSelectionParameters);
        Assert.assertTrue(result.getResult().size() == 2);
        Assert.assertTrue(result.getResult().size() == collectUniqueAzs(result).size());

    }

    @Test
    public void testSubnetSelectionWhenHaRequiredAnd1DifferentAZDeclaredShouldReturnError() {
        List<CloudSubnet> cloudSubnets = Lists.newArrayList(
                getSubnet("a1", 1),
                getSubnet("a1", 2),
                getSubnet("a1", 3),
                getSubnet("a1", 4));

        prepareMock(cloudSubnets);

        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withPreferPrivateIfExist()
                .withTunnel(Tunnel.CCM)
                .withHa(true)
                .build();

        SubnetSelectionResult result = underTest.chooseSubnets(cloudSubnets, subnetSelectionParameters);
        Assert.assertTrue(result.hasError());
    }

    @Test
    public void testSubnetSelectionWhenHaRequiredAnd4DifferentAZDeclaredShouldReturn3DifferentAz() {
        List<CloudSubnet> cloudSubnets = Lists.newArrayList(
                getSubnet("a1", 1),
                getSubnet("a2", 2),
                getSubnet("a3", 3),
                getSubnet("a4", 4));

        prepareMock(cloudSubnets);

        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withPreferPrivateIfExist()
                .withTunnel(Tunnel.CCM)
                .withHa(true)
                .build();

        SubnetSelectionResult result = underTest.chooseSubnets(cloudSubnets, subnetSelectionParameters);
        Assert.assertTrue(result.getResult().size() == 3);
        Assert.assertTrue(result.getResult().size() == collectUniqueAzs(result).size());
    }

    public Set<String> collectUniqueAzs(SubnetSelectionResult result) {
        return result.getResult().stream().map(e -> e.getAvailabilityZone()).collect(Collectors.toSet());
    }

    @Test
    public void testSubnetSelectionWhenNonHaRequiredAnd2DifferentAZDeclaredShouldReturn1Az() {
        List<CloudSubnet> cloudSubnets = Lists.newArrayList(
                getSubnet("a1", 1),
                getSubnet("a1", 2),
                getSubnet("a2", 3),
                getSubnet("a2", 4));

        prepareMock(cloudSubnets);

        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withPreferPrivateIfExist()
                .withTunnel(Tunnel.CCM)
                .withHa(false)
                .build();

        SubnetSelectionResult result = underTest.chooseSubnets(cloudSubnets, subnetSelectionParameters);
        Assert.assertTrue(result.getResult().size() == 1);
    }

    public void prepareMock(List<CloudSubnet> cloudSubnets) {
        Map<SubnetFilterStrategyType, SubnetFilterStrategy> subnetFilterStrategyMap = new HashMap<>();
        subnetFilterStrategyMap.put(SubnetFilterStrategyType.MULTIPLE_PREFER_PRIVATE, subnetFilterStrategy);
        subnetFilterStrategyMap.put(SubnetFilterStrategyType.MULTIPLE_PREFER_PUBLIC, subnetFilterStrategy);
        when(subnetFilterStrategy.filter(any(), anyInt())).thenReturn(new SubnetSelectionResult(cloudSubnets));
        ReflectionTestUtils.setField(underTest, "subnetFilterStrategyMap", subnetFilterStrategyMap);
    }

    @Test
    public void testSubnetSelectionWhenNonHaRequiredAnd1DifferentAZDeclaredShouldReturnOneSubnet() {
        List<CloudSubnet> cloudSubnets = Lists.newArrayList(
                getSubnet("a1", 1),
                getSubnet("a1", 2),
                getSubnet("a1", 3),
                getSubnet("a1", 4));

        prepareMock(cloudSubnets);

        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withPreferPrivateIfExist()
                .withTunnel(Tunnel.CCM)
                .withHa(false)
                .build();

        SubnetSelectionResult result = underTest.chooseSubnets(cloudSubnets, subnetSelectionParameters);
        Assert.assertTrue(result.getResult().size() == 1);
    }

    @Test
    public void testSubnetSelectionWhenNonHaRequiredAnd4DifferentAZDeclaredShouldReturn1Az() {
        List<CloudSubnet> cloudSubnets = Lists.newArrayList(
                getSubnet("a1", 1),
                getSubnet("a2", 2),
                getSubnet("a3", 3),
                getSubnet("a4", 4));

        prepareMock(cloudSubnets);

        SubnetSelectionParameters subnetSelectionParameters = SubnetSelectionParameters
                .builder()
                .withPreferPrivateIfExist()
                .withTunnel(Tunnel.CCM)
                .withHa(false)
                .build();

        SubnetSelectionResult result = underTest.chooseSubnets(cloudSubnets, subnetSelectionParameters);
        Assert.assertTrue(result.getResult().size() == 1);
    }

    private CloudSubnet getSubnet(String az, int index) {
        String nextSubnetId = "subnet-" + index;
        return new CloudSubnet(nextSubnetId, "", az, "", true, false, false, PUBLIC);
    }

    private DescribeVpcsResult describeVpcsResult(String... cidrBlocks) {
        DescribeVpcsResult describeVpcsResult = new DescribeVpcsResult();
        List<Vpc> vpcs = new ArrayList<>();
        for (String block : cidrBlocks) {
            Vpc vpc = new Vpc();
            vpc.setCidrBlock(block);

            VpcCidrBlockAssociation vpcCidrBlockAssociation = new VpcCidrBlockAssociation();
            vpcCidrBlockAssociation.setCidrBlock(block);

            vpc.getCidrBlockAssociationSet().add(vpcCidrBlockAssociation);

            vpcs.add(vpc);
        }
        describeVpcsResult.withVpcs(vpcs);
        return describeVpcsResult;
    }

    private NetworkDeletionRequest createNetworkDeletionRequest() {
        return new NetworkDeletionRequest.Builder()
                .withRegion(REGION.value())
                .withStackName(NETWORK_ID)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .build();
    }

    private Map<String, String> createOutput() {
        Map<String, String> output = new HashMap<>();
        output.put(CREATED_VPC, VPC_ID);
        output.put(CREATED_SUBNET_0, SUBNET_ID_0);
        output.put(CREATED_SUBNET_1, SUBNET_ID_1);
        output.put(CREATED_SUBNET_2, SUBNET_ID_2);
        return output;
    }

    private NetworkCreationRequest createNetworkRequest(String networkCidr, Set<NetworkSubnetRequest> subnets) {
        return new NetworkCreationRequest.Builder()
                .withStackName(STACK_NAME)
                .withEnvName(ENV_NAME)
                .withEnvId(1L)
                .withEnvCrn(ENV_CRN)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withRegion(REGION)
                .withNetworkCidr(networkCidr)
                .withPrivateSubnets(subnets)
                .withPublicSubnets(subnets)
                .withPrivateSubnetEnabled(true)
                .withUserName("user@cloudera.com")
                .withCreatorCrn("user-crn")
                .build();
    }

    private List<SubnetRequest> createSubnetRequestList() {
        SubnetRequest subnetRequest1 = new SubnetRequest();
        subnetRequest1.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest1.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest1.setAvailabilityZone("az1");

        SubnetRequest subnetRequest2 = new SubnetRequest();
        subnetRequest2.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest2.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest2.setAvailabilityZone("az2");

        SubnetRequest subnetRequest3 = new SubnetRequest();
        subnetRequest3.setPublicSubnetCidr("2.2.2.2/24");
        subnetRequest3.setPrivateSubnetCidr("2.2.2.2/24");
        subnetRequest3.setAvailabilityZone("az3");

        return List.of(subnetRequest1, subnetRequest2, subnetRequest3);
    }

}
