package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.model.network.SubnetType.PUBLIC;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcCidrBlockAssociation;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategyType;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.RetryService;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {"cb.max.aws.resource.name.length = 345678", "cb.aws.cf.network.template.path=src/main/resources/templates/aws-cf-network.ftl"})
@ActiveProfiles("Integration")
public class AwsNetworkConnectorIntegrationTest {

    @Inject
    private AwsNetworkConnector testAwsNetworkConnector;

    @SpyBean
    private AwsClient awsClient;

    @SpyBean
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @SpyBean
    private CloudFormationStackUtil cfStackUtil;

    @SpyBean
    private AmazonCloudFormationWaiters cfWaiters;

    @MockBean
    private Waiter<DescribeStacksRequest> deleteWaiter;

    @Mock
    private Waiter<DescribeStacksRequest> createWaiter;

    @MockBean
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @MockBean
    private AmazonCloudFormationClient amazonCloudFormationClient;

    @SpyBean
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @SpyBean
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @SpyBean
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @SpyBean
    private AwsTaggingService awsTaggingService;

    @Mock
    private DescribeStacksResult describeStacksResult;

    @Mock
    private CreateStackResult createStackResult;

    @Mock
    private DeleteStackResult deleteStackResult;

    @MockBean
    private Map<SubnetFilterStrategyType, SubnetFilterStrategy> subnetFilterStrategyMap;

    @MockBean(name = "cloudApiListeningScheduledExecutorService")
    private ListeningScheduledExecutorService cloudApiListeningScheduledExecutorService;

    @Mock
    private AmazonEC2Client amazonEC2Client;

    @Mock
    private PollTask<Boolean> pollTask;

    private Set<NetworkSubnetRequest> publicSubnetRequest = Set.of(new NetworkSubnetRequest("1.1.1.1/8", PUBLIC),
            new NetworkSubnetRequest("1.1.1.2/8", PUBLIC));

    private Set<CreatedSubnet> createdSubnets = Set.of(new CreatedSubnet(), new CreatedSubnet(), new CreatedSubnet());

    private CloudCredential credential = new CloudCredential("cloudCredId", "cloudCredName");

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

    private Map<String, String> createOutput() {
        Map<String, String> output = new HashMap<>();
        output.put("CreatedVPC", "CVP1");
        output.put("CreatedSubnet0", "subnet-0");
        output.put("CreatedSubnet1", "subnet-1");
        output.put("CreatedSubnet2", "subnet-2");
        return output;
    }

    private NetworkDeletionRequest createNetworkDeletionRequest(Boolean exist) {
        return new NetworkDeletionRequest.Builder().
                withStackName("stackName").
                withCloudCredential(credential).
                withRegion("region").
                withResourceGroup("resourceGroup").
                withExisting(exist).
                build();
    }

    private NetworkCreationRequest createNetworkCreationRequest() {
        return new NetworkCreationRequest.Builder()
                .withCloudCredential(credential)
                .withEnvCrn("creatorCRn")
                .withEnvId(1L)
                .withEnvName("envName")
                .withPublicSubnets(publicSubnetRequest)
                .withPrivateSubnets(publicSubnetRequest)
                .withNetworkCidr("networkCidr")
                .withPrivateSubnetEnabled(true)
                .withRegion(Region.region("HU_WEST_1"))
                .withUserName("user@cloudera.com")
                .withNetworkCidr("0.0.0.0/16")
                .withStackName("envName-1")
                .build();
    }

    private Map<String, String> createOutputVpcExist() {
        Map<String, String> output = new HashMap<>();
        output.put("CreatedVpc", "VpcId");
        output.put("Created_Subnet_0", "Subnet_Id_0");
        output.put("Created_Subnet_1", "Subnet_Id_1");
        output.put("Created_Subnet_2", "Subnet_Id_2");
        return output;
    }

    @BeforeEach
    void setUp() {
        doReturn(amazonCloudFormationRetryClient).when(awsClient).createCloudFormationRetryClient(any(), any());
        doReturn(amazonEC2Client).when(awsClient).createAccess(any(), any());
    }

    @Test
    public void createNetworkWithSubnetsFailedToConnectServer() {
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(), any(), any());
        doThrow(new AmazonServiceException("Failed to connect server")).when(amazonCloudFormationRetryClient).describeStacks(any());
        assertThatThrownBy(() -> testAwsNetworkConnector.createNetworkWithSubnets(createNetworkCreationRequest())).isInstanceOf(CloudConnectorException.class)
                .hasMessage("Failed to create network.");

    }

    @Test
    public void createNetworkWithSubnetsWaiterTimeoutException() {
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        WaiterTimedOutException waiterTimedOutException = new WaiterTimedOutException("Process Timedout");
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(), any(), any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(), any());
        doReturn(describeStacksResult).when(amazonCloudFormationRetryClient).describeStacks(any());
        doReturn(cfWaiters).when(amazonCloudFormationClient).waiters();
        doReturn(createWaiter).when(cfWaiters).stackCreateComplete();
        doThrow(waiterTimedOutException).when(createWaiter).run(any());
        assertThatThrownBy(() -> testAwsNetworkConnector.createNetworkWithSubnets(createNetworkCreationRequest())).isInstanceOf(CloudConnectorException.class)
                .hasMessage("Process Timedout");
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), any());
        verify(amazonCloudFormationRetryClient).describeStacks(any(DescribeStacksRequest.class));
    }

    @Test
    public void createNetworkWithSubnetsCreateCloudNetwork() throws InterruptedException, ExecutionException, TimeoutException {
        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(), any(), any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(), any());
        doReturn(describeStacksResult).when(amazonCloudFormationRetryClient).describeStacks(any());
        doReturn(cfWaiters).when(amazonCloudFormationClient).waiters();
        doReturn(createWaiter).when(cfWaiters).stackCreateComplete();
        doReturn(createOutputVpcExist()).when(cfStackUtil).getOutputs(any(), any());
        doReturn(createdSubnets).when(awsCreatedSubnetProvider).provide(any(), any(), anyBoolean());

        CreatedCloudNetwork createdCloudNetwork = testAwsNetworkConnector.createNetworkWithSubnets(createNetworkCreationRequest());

        assertEquals(createdCloudNetwork.getNetworkId(), "VpcId");
        assertEquals(3, createdCloudNetwork.getSubnets().size());
        assertEquals(createdCloudNetwork.getStackName(), createNetworkCreationRequest().getStackName());
        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), any());
        verify(amazonCloudFormationRetryClient).describeStacks(any(DescribeStacksRequest.class));
        verify(createWaiter, times(1)).run(any());
        verify(amazonCloudFormationClient, never()).createStack(any(CreateStackRequest.class));
    }

    @Test
    public void createNetworkWithSubnetsCfStackDoesNotExist() throws InterruptedException, ExecutionException, TimeoutException {

        List<SubnetRequest> subnetRequestList = createSubnetRequestList();
        doReturn(subnetRequestList).when(awsSubnetRequestProvider).provide(any(), any(), any());
        AmazonServiceException amazonServiceException = new AmazonServiceException("Network does not exist");
        amazonServiceException.setStatusCode(400);
        doThrow(amazonServiceException).when(amazonCloudFormationRetryClient).describeStacks(any());
        doReturn(createStackResult).when(amazonCloudFormationRetryClient).createStack(any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(), any());
        doReturn(cfWaiters).when(amazonCloudFormationClient).waiters();
        doReturn(createWaiter).when(cfWaiters).stackCreateComplete();
        doReturn(createOutputVpcExist()).when(cfStackUtil).getOutputs(any(), any());
        doReturn(createdSubnets).when(awsCreatedSubnetProvider).provide(any(), any(), anyBoolean());

        CreatedCloudNetwork createdCloudNetwork = testAwsNetworkConnector.createNetworkWithSubnets(createNetworkCreationRequest());

        assertEquals(createdCloudNetwork.getNetworkId(), "VpcId");
        assertEquals(createdCloudNetwork.getStackName(), createNetworkCreationRequest().getStackName());
        assertEquals(3, createdCloudNetwork.getSubnets().size());
        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), any());
        verify(amazonCloudFormationRetryClient).createStack(any(CreateStackRequest.class));
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), any());
        verify(createWaiter, times(1)).run(any());
        verify(amazonCloudFormationClient, never()).createStack(any(CreateStackRequest.class));

    }

    @Test
    public void getNetworkCidrReturnFirsCidr() {
        String existingVpc = "vpc-1";
        String cidrBlock1 = "10.0.0.0/16";
        String cidrBlock2 = "10.23.0.0/16";

        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "hu-east-1"));
        CloudCredential credential = new CloudCredential();
        DescribeVpcsResult describeVpcsResult = describeVpcsResult(cidrBlock1, cidrBlock2);
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("hu-east-1"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);

        NetworkCidr result = testAwsNetworkConnector.getNetworkCidr(network, credential);
        assertEquals(cidrBlock1, result.getCidr());

        verify(awsClient).createAccess(any(AwsCredentialView.class), eq("hu-east-1"));
    }

    @Test
    public void getNetworkCidrThrowsError() throws BadRequestException {
        String existingVpc = "vpc-1";
        Network network = new Network(null, Map.of(AwsNetworkView.VPC_ID, existingVpc, "region", "hu-east-1"));
        CloudCredential credential = new CloudCredential();
        DescribeVpcsResult describeVpcsResult = new DescribeVpcsResult().withVpcs();
        when(awsClient.createAccess(any(AwsCredentialView.class), eq("hu-east-1"))).thenReturn(amazonEC2Client);
        when(amazonEC2Client.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc))).thenReturn(describeVpcsResult);
        assertThatThrownBy(() -> testAwsNetworkConnector.getNetworkCidr(network, credential)).isInstanceOf(BadRequestException.class)
                .hasMessage("VPC cidr could not fetch from AWS: " + existingVpc);

        verify(awsClient).createAccess(any(AwsCredentialView.class), eq("hu-east-1"));

    }

    @Test
    public void deleteNetworkWithSubnetsRequestExists() {
        NetworkDeletionRequest networkDeletionRequest = createNetworkDeletionRequest(Boolean.TRUE);
        testAwsNetworkConnector.deleteNetworkWithSubnets(networkDeletionRequest);

        verifyNoMoreInteractions(awsClient);
    }

    @Test
    public void deleteNetworkWithSubnetsWaiterThrowsError() {
        WaiterTimedOutException waiterTimedOutException = new WaiterTimedOutException("Connection Timedout");
        doReturn(deleteStackResult).when(amazonCloudFormationRetryClient).deleteStack(any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(AwsCredentialView.class), any());
        doReturn(cfWaiters).when(amazonCloudFormationClient).waiters();
        doReturn(deleteWaiter).when(cfWaiters).stackDeleteComplete();
        doThrow(waiterTimedOutException).when(deleteWaiter).run(any());
        assertThatThrownBy(() -> testAwsNetworkConnector.deleteNetworkWithSubnets(createNetworkDeletionRequest(Boolean.FALSE)))
                .isInstanceOf(CloudConnectorException.class)
                .hasMessage("Connection Timedout");
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), any());
        verify(deleteWaiter, times(1)).run(any());
    }

    @Test
    public void deleteNetworkWithSubnetsSuccessfull() throws InterruptedException, ExecutionException, TimeoutException {

        doReturn(deleteStackResult).when(amazonCloudFormationRetryClient).deleteStack(any());
        doReturn(amazonCloudFormationClient).when(awsClient).createCloudFormationClient(any(AwsCredentialView.class), any());
        doReturn(cfWaiters).when(amazonCloudFormationClient).waiters();
        doReturn(deleteWaiter).when(cfWaiters).stackDeleteComplete();
        testAwsNetworkConnector.deleteNetworkWithSubnets(createNetworkDeletionRequest(Boolean.FALSE));

        verify(awsClient, times(0)).createAccess(any(AwsCredentialView.class), any());
        verify(awsClient, times(1)).createCloudFormationClient(any(AwsCredentialView.class), any());
        verify(deleteWaiter, times(1)).run(any());
    }

    @Configuration
    @Profile("Integration")
    @Import({
            AwsCreatedSubnetProvider.class,
            AwsSubnetRequestProvider.class,
            AwsNetworkCfTemplateProvider.class,
            AwsNetworkConnector.class,
            AwsClient.class,
            FreeMarkerConfigurationFactoryBean.class,
            FreeMarkerTemplateUtils.class,
            JsonHelper.class,
            CloudFormationStackUtil.class,
            AwsSessionCredentialClient.class,
            AwsEnvironmentVariableChecker.class,
            AwsDefaultZoneProvider.class,
            RetryService.class,
            AmazonCloudFormationWaiters.class

    })
    static class Config {

    }

}
