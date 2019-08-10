package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConstants.ERROR_STATUSES;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class AwsNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkConnector.class);

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnets";

    private static final int NOT_FOUND = 400;

    @Inject
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private AwsCloudSubnetProvider awsCloudSubnetProvider;

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkRequest) {
        AwsCredentialView credentialView = new AwsCredentialView(networkRequest.getCloudCredential());
        AmazonCloudFormationRetryClient cloudFormationRetryClient = getCloudFormationRetryClient(credentialView, networkRequest.getRegion().value());
        List<CreatedSubnet> subnetList = getCloudSubNets(networkRequest);
        String cloudFormationTemplate = createTemplate(networkRequest, subnetList);
        String cfStackName = networkRequest.getStackName();
        try {
            cloudFormationRetryClient.describeStacks(new DescribeStacksRequest().withStackName(cfStackName));
            LOGGER.warn("AWS CloudFormation stack for Network with stack name: '{}' already exists. Attaching this one to the network.", cfStackName);
            return getCreatedNetworkWithPolling(networkRequest, credentialView, cloudFormationRetryClient, subnetList);
        } catch (AmazonServiceException e) {
            if (networkDoesNotExist(e)) {
                LOGGER.warn("{} occurred during describe AWS CloudFormation stack for Network with stack name: '{}'. "
                        + "Assuming the CF Stack does not exist, so creating a new one. Exception message: {}", e.getClass(), cfStackName, e.getMessage());
                return createNewCfNetworkStack(networkRequest, credentialView, cloudFormationRetryClient,
                        subnetList, cloudFormationTemplate);
            } else {
                throw new CloudConnectorException("Failed to create network.", e);
            }
        }
    }

    private boolean networkDoesNotExist(AmazonServiceException e) {
        return e.getStatusCode() == NOT_FOUND && e.getErrorMessage().contains("does not exist");
    }

    private List<CreatedSubnet> getCloudSubNets(NetworkCreationRequest networkRequest) {
        AwsCredentialView awsCredential = new AwsCredentialView(networkRequest.getCloudCredential());
        AmazonEC2Client awsClientAccess = awsClient.createAccess(awsCredential, networkRequest.getRegion().value());
        return awsCloudSubnetProvider.provide(awsClientAccess, new ArrayList<>(networkRequest.getSubnetCidrs()));
    }

    private AmazonCloudFormationRetryClient getCloudFormationRetryClient(AwsCredentialView credentialView, String region) {
        return awsClient.createCloudFormationRetryClient(credentialView, region);
    }

    private String createTemplate(NetworkCreationRequest networkRequest, List<CreatedSubnet> createdSubnetList) {
        return awsNetworkCfTemplateProvider.provide(networkRequest.getNetworkCidr(), createdSubnetList);
    }

    private CreatedCloudNetwork createNewCfNetworkStack(NetworkCreationRequest networkRequest, AwsCredentialView credentialView,
            AmazonCloudFormationRetryClient cloudFormationRetryClient, List<CreatedSubnet> subnetList, String cloudFormationTemplate) {
        cloudFormationRetryClient.createStack(createStackRequest(networkRequest.getStackName(), cloudFormationTemplate));
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' ", networkRequest.getStackName());
        return getCreatedNetworkWithPolling(networkRequest, credentialView, cloudFormationRetryClient, subnetList);
    }

    private CreatedCloudNetwork getCreatedNetworkWithPolling(NetworkCreationRequest networkRequest, AwsCredentialView credentialView,
            AmazonCloudFormationRetryClient cloudFormationRetryClient, List<CreatedSubnet> subnetList) {
        PollTask<Boolean> pollTask = getNewNetworkPollTask(credentialView, networkRequest);
        try {
            awsBackoffSyncPollingScheduler.schedule(pollTask);
        } catch (RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
        return getCreatedCloudNetwork(cloudFormationRetryClient, subnetList, networkRequest.getStackName());
    }

    private CreatedCloudNetwork getCreatedCloudNetwork(AmazonCloudFormationRetryClient cloudFormationRetryClient,
            List<CreatedSubnet> subnetList, String cfStackName) {
        Map<String, String> output = cfStackUtil.getOutputs(cfStackName, cloudFormationRetryClient);
        String vpcId = getCreatedVpc(output);
        Set<CreatedSubnet> subnets = getCreatedSubnets(output, subnetList);
        return new CreatedCloudNetwork(cfStackName, vpcId, subnets);
    }

    private CreateStackRequest createStackRequest(String stackName, String cloudFormationTemplate) {
        return new CreateStackRequest()
                .withStackName(stackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cloudFormationTemplate)
                .withCapabilities(CAPABILITY_IAM);
    }

    private PollTask<Boolean> getNewNetworkPollTask(AwsCredentialView credentialView, NetworkCreationRequest networkRequest) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, networkRequest.getRegion().value());
        return awsPollTaskFactory
                .newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkRequest);
    }

    private PollTask<Boolean> getDeleteNetworkPollTask(AwsCredentialView credentialView, String region, String stackName) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, region);
        return awsPollTaskFactory.newAwsTerminateNetworkStatusCheckerTask(cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES, stackName);
    }

    private String getCreatedVpc(Map<String, String> output) {
        if (output.containsKey(CREATED_VPC)) {
            return output.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = String.format("Vpc could not be found in the CloudFormation stack output.");
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private Set<CreatedSubnet> getCreatedSubnets(Map<String, String> output, List<CreatedSubnet> createdSubnetList) {
        Set<CreatedSubnet> subnets = new HashSet<>();
        for (int i = 0; i < createdSubnetList.size(); i++) {
            String expectedSubnetName = CREATED_SUBNET + i;
            if (output.containsKey(expectedSubnetName)) {
                CreatedSubnet createdSubnet = new CreatedSubnet();
                createdSubnet.setSubnetId(output.get(expectedSubnetName));
                createdSubnet.setCidr(createdSubnetList.get(i).getCidr());
                createdSubnet.setAvailabilityZone(createdSubnetList.get(i).getAvailabilityZone());
                createdSubnet.setPrivateSubnet(false);
                createdSubnet.setMapPublicIpOnLaunch(true);
                createdSubnet.setIgwAvailable(true);
                subnets.add(createdSubnet);
            } else {
                String outputKeyNotFound = String.format("Subnet could not be found in the CloudFormation stack output.");
                throw new CloudConnectorException(outputKeyNotFound);
            }
        }
        return subnets;
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        AwsCredentialView credentialView = new AwsCredentialView(networkDeletionRequest.getCloudCredential());
        AmazonCloudFormationRetryClient cloudFormationRetryClient = getCloudFormationRetryClient(credentialView, networkDeletionRequest.getRegion());
        DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
        deleteStackRequest.setStackName(networkDeletionRequest.getStackName());
        cloudFormationRetryClient.deleteStack(deleteStackRequest);

        LOGGER.debug("CloudFormation stack deletion request sent with stack name: '{}' ", networkDeletionRequest.getStackName());
        PollTask<Boolean> pollTask = getDeleteNetworkPollTask(credentialView, networkDeletionRequest.getRegion(), networkDeletionRequest.getStackName());
        try {
            awsBackoffSyncPollingScheduler.schedule(pollTask);
        } catch (RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_VARIANT;
    }

}
