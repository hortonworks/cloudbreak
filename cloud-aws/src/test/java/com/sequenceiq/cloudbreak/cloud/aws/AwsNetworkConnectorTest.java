package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;

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
    private AwsClient awsClient;

    @Mock
    private AwsPollTaskFactory awsPollTaskFactory;

    @Mock
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Mock
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @Mock
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Test
    public void testPlatformShouldReturnAwsPlatform() {
        Platform actual = underTest.platform();

        assertEquals(AwsConstants.AWS_PLATFORM, actual);
    }

    @Test
    public void testVariantShouldReturnAwsPlatform() {
        Variant actual = underTest.variant();

        assertEquals(AwsConstants.AWS_VARIANT, actual);
    }

    @Test
    public void testCreateNetworkWithSubnetsShouldReturnTheNetworkAndSubnets() {
        String networkCidr = "0.0.0.0/16";
        Set<String> subnetCidrs = Set.of("1.1.1.1/8", "1.1.1.2/8");
        AmazonCloudFormationRetryClient cloudFormationRetryClient = mock(AmazonCloudFormationRetryClient.class);
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        PollTask pollTask = mock(PollTask.class);
        Map<String, String> output = createOutput();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(networkCidr, subnetCidrs);
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        Set<CreatedSubnet> createdSubnets = Set.of(new CreatedSubnet(), new CreatedSubnet(), new CreatedSubnet());

        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(awsSubnetRequestProvider.provide(ec2Client, new ArrayList<>(subnetCidrs))).thenReturn(subnetRequestList);
        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cloudFormationRetryClient);
        when(awsNetworkCfTemplateProvider.provide(networkCidr, subnetRequestList, true)).thenReturn(CF_TEMPLATE);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cfClient);
        when(awsPollTaskFactory.newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkCreationRequest))
                .thenReturn(pollTask);
        when(cfStackUtil.getOutputs(NETWORK_ID, cloudFormationRetryClient)).thenReturn(output);
        when(awsCreatedSubnetProvider.provide(output, networkCreationRequest.getSubnetCidrs().size(), networkCreationRequest.isPrivateSubnetEnabled()))
                .thenReturn(createdSubnets);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsNetworkCfTemplateProvider).provide(networkCidr, subnetRequestList, true);
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsPollTaskFactory).newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkCreationRequest);
        verify(cfStackUtil).getOutputs(NETWORK_ID, cloudFormationRetryClient);
        verify(defaultCostTaggingService, never()).prepareDefaultTags(any());
        verify(awsTaggingService, never()).prepareCloudformationTags(any(), any());
        verify(cloudFormationRetryClient, never()).createStack(any(CreateStackRequest.class));
        assertEquals(VPC_ID, actual.getNetworkId());
        assertEquals(NUMBER_OF_SUBNETS, actual.getSubnets().size());
    }

    @Test
    public void testCreateNewNetworkWithSubnetsShouldCreateTheNetworkAndSubnets() {
        String networkCidr = "0.0.0.0/16";
        Set<String> subnetCidrs = Set.of("1.1.1.1/8", "1.1.1.2/8");
        AmazonCloudFormationRetryClient cloudFormationRetryClient = mock(AmazonCloudFormationRetryClient.class);
        AmazonServiceException amazonServiceException = new AmazonServiceException("does not exist");
        amazonServiceException.setStatusCode(400);
        when(cloudFormationRetryClient.describeStacks(any(DescribeStacksRequest.class))).thenThrow(amazonServiceException);
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        AmazonEC2Client ec2Client = mock(AmazonEC2Client.class);
        PollTask pollTask = mock(PollTask.class);
        Map<String, String> output = createOutput();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(networkCidr, subnetCidrs);
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        Set<CreatedSubnet> createdSubnets = Set.of(new CreatedSubnet(), new CreatedSubnet(), new CreatedSubnet());

        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(awsSubnetRequestProvider.provide(ec2Client, new ArrayList<>(subnetCidrs))).thenReturn(subnetRequestList);
        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cloudFormationRetryClient);
        when(awsNetworkCfTemplateProvider.provide(networkCidr, subnetRequestList, true)).thenReturn(CF_TEMPLATE);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cfClient);
        when(awsPollTaskFactory.newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkCreationRequest))
                .thenReturn(pollTask);
        when(cfStackUtil.getOutputs(NETWORK_ID, cloudFormationRetryClient)).thenReturn(output);
        when(awsCreatedSubnetProvider.provide(output, networkCreationRequest.getSubnetCidrs().size(), networkCreationRequest.isPrivateSubnetEnabled()))
                .thenReturn(createdSubnets);
        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsNetworkCfTemplateProvider).provide(networkCidr, subnetRequestList, true);
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsPollTaskFactory).newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkCreationRequest);
        verify(defaultCostTaggingService).prepareDefaultTags(any());
        verify(awsTaggingService).prepareCloudformationTags(any(), any());
        verify(cloudFormationRetryClient).createStack(any(CreateStackRequest.class));
        verify(cfStackUtil).getOutputs(NETWORK_ID, cloudFormationRetryClient);
        assertEquals(VPC_ID, actual.getNetworkId());
        assertEquals(NUMBER_OF_SUBNETS, actual.getSubnets().size());
    }

    @Test
    public void testDeleteNetworkWithSubNetsShouldDeleteTheStackAndTheResourceGroup() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        AmazonCloudFormationRetryClient cloudFormationRetryClient = mock(AmazonCloudFormationRetryClient.class);
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        PollTask pollTask = mock(PollTask.class);

        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), eq(networkDeletionRequest.getRegion())))
                .thenReturn(cloudFormationRetryClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cfClient);
        when(awsPollTaskFactory.newAwsTerminateNetworkStatusCheckerTask(cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, NETWORK_ID))
                .thenReturn(pollTask);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(cloudFormationRetryClient).deleteStack(any(DeleteStackRequest.class));
        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsPollTaskFactory).newAwsTerminateNetworkStatusCheckerTask(cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, NETWORK_ID);
    }

    @Test(expected = CloudConnectorException.class)
    public void testDeleteNetworkWithSubNetsShouldThrowAnExceptionWhenTheStackDeletionFailed()
            throws InterruptedException, ExecutionException, TimeoutException {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest();
        AmazonCloudFormationRetryClient cloudFormationRetryClient = mock(AmazonCloudFormationRetryClient.class);
        AmazonCloudFormationClient cfClient = mock(AmazonCloudFormationClient.class);
        PollTask pollTask = mock(PollTask.class);

        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), eq(networkDeletionRequest.getRegion())))
                .thenReturn(cloudFormationRetryClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()))).thenReturn(cfClient);
        when(awsPollTaskFactory.newAwsTerminateNetworkStatusCheckerTask(cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, NETWORK_ID))
                .thenReturn(pollTask);
        doThrow(new TimeoutException()).when(awsBackoffSyncPollingScheduler).schedule(pollTask);

        underTest.deleteNetworkWithSubnets(networkDeletionRequest);

        verify(cloudFormationRetryClient).deleteStack(any(DeleteStackRequest.class));
        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(REGION.value()));
        verify(awsPollTaskFactory).newAwsTerminateNetworkStatusCheckerTask(cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, NETWORK_ID);
    }

    @Test
    public void testGetNetworkCidr() {
        String existingVpc = "vpc-1";
        String cidrBlock = "10.0.0.0/16";

        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        DescribeVpcsResult describeVpcsResult = describeVpcsResult(cidrBlock);

        when(awsClient.createAccess(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        String result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock, result);
    }

    @Test
    public void testGetNetworkCidrWithoutResult() {
        String existingVpc = "vpc-1";

        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        DescribeVpcsResult describeVpcsResult = describeVpcsResult();

        when(awsClient.createAccess(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("VPC cidr could not fetch from AWS: " + existingVpc);

        underTest.getNetworkCidr(network, credential);
    }

    @Test
    public void testGetNetworkCidrMoreThanOne() {
        String existingVpc = "vpc-1";
        String cidrBlock1 = "10.0.0.0/16";
        String cidrBlock2 = "10.23.0.0/16";

        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "us-west-2"));
        CloudCredential credential = new CloudCredential();
        AmazonEC2Client amazonEC2Client = mock(AmazonEC2Client.class);
        DescribeVpcsResult describeVpcsResult = describeVpcsResult(cidrBlock1, cidrBlock2);

        when(awsClient.createAccess(any(AwsCredentialView.class), eq("us-west-2"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        String result = underTest.getNetworkCidr(network, credential);
        assertEquals(cidrBlock1, result);
    }

    private DescribeVpcsResult describeVpcsResult(String... cidrBlocks) {
        DescribeVpcsResult describeVpcsResult = new DescribeVpcsResult();
        List<Vpc> vpcs = new ArrayList<>();
        for (String block : cidrBlocks) {
            Vpc vpc = new Vpc();
            vpc.setCidrBlock(block);
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

    private NetworkCreationRequest createNetworkRequest(String networkCidr, Set<String> subnetCidrs) {
        return new NetworkCreationRequest.Builder()
                .withStackName(STACK_NAME)
                .withEnvName(ENV_NAME)
                .withEnvCrn(ENV_CRN)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withRegion(REGION)
                .withNetworkCidr(networkCidr)
                .withSubnetCidrs(subnetCidrs)
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
