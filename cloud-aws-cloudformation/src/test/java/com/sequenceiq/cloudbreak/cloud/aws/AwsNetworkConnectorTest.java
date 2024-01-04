package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsSubnetRequestProvider;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyType;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
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
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.Tunnel;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcCidrBlockAssociation;

@ExtendWith(MockitoExtension.class)
public class AwsNetworkConnectorTest {

    private static final String ENV_NAME = "testEnv";

    private static final String STACK_NAME = ENV_NAME + "-1";

    private static final String VPC_ID = "newVpc";

    private static final String SUBNET_ID_0 = "subnet-0";

    private static final String SUBNET_ID_1 = "subnet-1";

    private static final String SUBNET_ID_2 = "subnet-1";

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET_0 = "CreatedSubnets0";

    private static final String CREATED_SUBNET_1 = "CreatedSubnets1";

    private static final String CREATED_SUBNET_2 = "CreatedSubnets2";

    private static final int NUMBER_OF_SUBNETS = 3;

    private static final long ID = 1L;

    private static final String NETWORK_ID = String.join("-", ENV_NAME, String.valueOf(ID));

    private static final Region REGION = Region.region("US_WEST_2");

    private static final String ENV_CRN = "someCrn";

    @InjectMocks
    private AwsNetworkConnector underTest;

    @Mock
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private CloudFormationWaiter cfWaiters;

    @Mock
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @Mock
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private SubnetFilterStrategy subnetFilterStrategy;

    @Mock
    private Retry retryService;

    @BeforeEach
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
        when(cfStackUtil.getOutputs(NETWORK_ID, cfClient)).thenReturn(output);
        when(awsCreatedSubnetProvider.provide(output, subnetRequestList, true)).thenReturn(createdSubnets);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(cfWaiters).waitUntilStackCreateComplete(eq(DescribeStacksRequest.builder().stackName(STACK_NAME).build()), any());
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
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
        AwsServiceException amazonServiceException = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorMessage("does not exist")
                        .sdkHttpResponse(SdkHttpResponse.builder().statusCode(400).build()).build()).build();
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
        when(cfStackUtil.getOutputs(NETWORK_ID, cfClient)).thenReturn(output);
        when(awsCreatedSubnetProvider.provide(output, subnetRequestList, true)).thenReturn(createdSubnets);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsNetworkCfTemplateProvider).provide(networkCreationRequest, subnetRequestList);
        verify(cfWaiters).waitUntilStackCreateComplete(eq(DescribeStacksRequest.builder().stackName(STACK_NAME).build()), any());
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
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(true);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(cfClient).deleteStack(any(DeleteStackRequest.class));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(cfWaiters).waitUntilStackDeleteComplete(any(DescribeStacksRequest.class));
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldThrowAnExceptionWhenTheStackDeletionFailed() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(networkDeletionRequest.getRegion()))).thenReturn(cfClient);
        when(cfClient.waiters()).thenReturn(cfWaiters);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(true);
        doThrow(SdkException.builder().message("error").build()).when(cfWaiters).waitUntilStackDeleteComplete(any(DescribeStacksRequest.class), any());

        assertThrows(CloudConnectorException.class, () -> underTest.deleteNetworkWithSubnets(networkDeletionRequest));

        verify(cfClient).deleteStack(any(DeleteStackRequest.class));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
    }

    @Test
    public void testDeleteNetworkWithSubNetsWhenCfStackDoesNotExist() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(networkDeletionRequest.getRegion()))).thenReturn(cfClient);
        when(retryService.testWith2SecDelayMax15Times(any())).thenReturn(false);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(cfClient, never()).deleteStack(any(DeleteStackRequest.class));
    }

    @Test
    public void testGetNetworkCidr() {
        String existingVpc = "vpc-1";
        String cidrBlock = "10.0.0.0/16";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResponse describeVpcsResult = describeVpcsResult(cidrBlock);

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(DescribeVpcsRequest.builder().vpcIds(existingVpc).build())).thenReturn(describeVpcsResult);

        NetworkCidr result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock, result.getCidr());
    }

    @Test
    public void testGetNetworkCidrWithDuplicatedCidr() {
        String existingVpc = "vpc-1";
        String cidrBlock = "10.0.0.0/16";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResponse describeVpcsResponse = describeVpcsResult(cidrBlock, cidrBlock);

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(DescribeVpcsRequest.builder().vpcIds(existingVpc).build())).thenReturn(describeVpcsResponse);

        NetworkCidr result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock, result.getCidr());
        assertEquals(1, result.getCidrs().size());
        assertEquals(cidrBlock, result.getCidrs().get(0));
    }

    @Test
    public void testGetNetworkCidrWithoutResult() {
        String existingVpc = "vpc-1";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResponse describeVpcsResponse = describeVpcsResult();

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(DescribeVpcsRequest.builder().vpcIds(existingVpc).build())).thenReturn(describeVpcsResponse);

        Exception exception = assertThrows(BadRequestException.class, () -> underTest.getNetworkCidr(network, credential));

        assertEquals("VPC cidr could not fetch from AWS: " + existingVpc, exception.getMessage());
    }

    @Test
    public void testGetNetworkCidrMoreThanOneAssociatedCidrOnOneVpcShouldReturn2Cidr() {
        String existingVpc = "vpc-1";
        String cidrBlock1 = "10.0.0.0/16";
        String cidrBlock2 = "10.23.0.0/16";

        Network network = new Network(null, Map.of(NetworkConstants.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEc2Client amazonEC2Client = mock(AmazonEc2Client.class);
        DescribeVpcsResponse describeVpcsResponse = describeVpcsResult(cidrBlock1, cidrBlock2);

        when(awsClient.createEc2Client(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(DescribeVpcsRequest.builder().vpcIds(existingVpc).build())).thenReturn(describeVpcsResponse);

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
        assertEquals(2, result.getResult().size());
        assertEquals(result.getResult().size(), collectUniqueAzs(result).size());
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
        assertTrue(result.hasError());
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
        assertEquals(3, result.getResult().size());
        assertEquals(result.getResult().size(), collectUniqueAzs(result).size());
    }

    public Set<String> collectUniqueAzs(SubnetSelectionResult result) {
        return result.getResult().stream().map(CloudSubnet::getAvailabilityZone).collect(Collectors.toSet());
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
        assertEquals(1, result.getResult().size());
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
        assertEquals(1, result.getResult().size());
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
        assertEquals(1, result.getResult().size());
    }

    private CloudSubnet getSubnet(String az, int index) {
        String nextSubnetId = "subnet-" + index;
        return new CloudSubnet(nextSubnetId, "", az, "", true, false, false, PUBLIC);
    }

    private DescribeVpcsResponse describeVpcsResult(String... cidrBlocks) {
        List<Vpc> vpcs = new ArrayList<>();
        for (String block : cidrBlocks) {
            Vpc vpc = Vpc.builder()
                    .cidrBlock(block)
                    .cidrBlockAssociationSet(VpcCidrBlockAssociation.builder().cidrBlock(block).build()).build();
            vpcs.add(vpc);
        }
        return DescribeVpcsResponse.builder().vpcs(vpcs).build();
    }

    private NetworkDeletionRequest createNetworkDeletionRequest() {
        return new NetworkDeletionRequest.Builder()
                .withRegion(REGION.value())
                .withStackName(NETWORK_ID)
                .withCloudCredential(new CloudCredential("1", "credential", "account"))
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
                .withCloudCredential(new CloudCredential("1", "credential", "account"))
                .withRegion(REGION)
                .withNetworkCidr(networkCidr)
                .withPrivateSubnets(subnets)
                .withPublicSubnets(subnets)
                .withPrivateSubnetEnabled(true)
                .withUserName("user@cloudera.com")
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
