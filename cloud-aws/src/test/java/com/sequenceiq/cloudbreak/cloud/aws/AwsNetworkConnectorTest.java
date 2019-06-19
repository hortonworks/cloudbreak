package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@RunWith(MockitoJUnitRunner.class)
public class AwsNetworkConnectorTest {

    private static final String ENV_NAME = "testEnv";

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
    private AwsCloudSubnetProvider awsCloudSubnetProvider;

    @Test
    public void testPlatformShouldReturnAwsPlatform() {
        Platform actual = underTest.platform();

        Assert.assertEquals(AwsConstants.AWS_PLATFORM, actual);
    }

    @Test
    public void testVariantShouldReturnAwsPlatform() {
        Variant actual = underTest.variant();

        Assert.assertEquals(AwsConstants.AWS_VARIANT, actual);
    }

    @Test
    public void testCreateNetworkWithSubnetsShouldCreateTheNetworkAndSubnets() {
        Region region = Region.region("US_WEST_2");
        String networkCidr = "0.0.0.0/16";
        Set<String> subnetCidrs = Set.of("1.1.1.1/8", "1.1.1.2/8");
        AmazonCloudFormationRetryClient cloudFormationRetryClient = Mockito.mock(AmazonCloudFormationRetryClient.class);
        AmazonCloudFormationClient cfClient = Mockito.mock(AmazonCloudFormationClient.class);
        AmazonEC2Client ec2Client = Mockito.mock(AmazonEC2Client.class);
        PollTask pollTask = Mockito.mock(PollTask.class);
        Map<String, String> output = createOutput();
        NetworkCreationRequest networkCreationRequest = createNetworkRequest(region, networkCidr, subnetCidrs);
        List<CreatedSubnet> createdSubnetList = createCloudSubnetList();

        when(awsClient.createAccess(any(), any())).thenReturn(ec2Client);
        when(awsCloudSubnetProvider.provide(ec2Client, new ArrayList<>(subnetCidrs))).thenReturn(createdSubnetList);
        when(awsClient.createCloudFormationRetryClient(any(AwsCredentialView.class), eq(region.value()))).thenReturn(cloudFormationRetryClient);
        when(awsNetworkCfTemplateProvider.provide(networkCidr, createdSubnetList)).thenReturn(CF_TEMPLATE);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), eq(region.value()))).thenReturn(cfClient);
        when(awsPollTaskFactory.newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, NETWORK_ID)).thenReturn(pollTask);
        when(cfStackUtil.getOutputs(NETWORK_ID, cloudFormationRetryClient)).thenReturn(output);

        CreatedCloudNetwork actual = underTest.createNetworkWithSubnets(networkCreationRequest);

        verify(awsClient).createCloudFormationRetryClient(any(AwsCredentialView.class), eq(region.value()));
        verify(awsNetworkCfTemplateProvider).provide(networkCidr, createdSubnetList);
        verify(awsClient).createCloudFormationClient(any(AwsCredentialView.class), eq(region.value()));
        verify(awsPollTaskFactory).newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, NETWORK_ID);
        verify(cfStackUtil).getOutputs(NETWORK_ID, cloudFormationRetryClient);
        assertEquals(VPC_ID, actual.getNetworkId());
        assertEquals(NUMBER_OF_SUBNETS, actual.getSubnets().size());
    }

    private Map<String, String> createOutput() {
        Map<String, String> output = new HashMap<>();
        output.put(CREATED_VPC, VPC_ID);
        output.put(CREATED_SUBNET_0, SUBNET_ID_0);
        output.put(CREATED_SUBNET_1, SUBNET_ID_1);
        output.put(CREATED_SUBNET_2, SUBNET_ID_2);
        return output;
    }

    private NetworkCreationRequest createNetworkRequest(Region region, String networkCidr, Set<String> subnetCidrs) {
        return new NetworkCreationRequest.Builder()
                .withId(ID)
                .withEnvName(ENV_NAME)
                .withCloudCredential(new CloudCredential("1", "credential"))
                .withRegion(region)
                .withNetworkCidr(networkCidr)
                .withSubnetCidrs(subnetCidrs)
                .build();
    }

    private List<CreatedSubnet> createCloudSubnetList() {
        CreatedSubnet createdSubnet1 = new CreatedSubnet();
        createdSubnet1.setCidr("2.2.2.2/24");
        createdSubnet1.setAvailabilityZone("az1");

        CreatedSubnet createdSubnet2 = new CreatedSubnet();
        createdSubnet2.setCidr("2.2.2.2/24");
        createdSubnet2.setAvailabilityZone("az2");

        CreatedSubnet createdSubnet3 = new CreatedSubnet();
        createdSubnet2.setCidr("2.2.2.2/24");
        createdSubnet2.setAvailabilityZone("az3");

        return List.of(createdSubnet1, createdSubnet2, createdSubnet3);
    }

}