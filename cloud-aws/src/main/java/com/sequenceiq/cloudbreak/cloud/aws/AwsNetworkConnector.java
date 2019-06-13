package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
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

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class AwsNetworkConnector implements NetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkConnector.class);

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnets";

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
        AmazonCloudFormationRetryClient cloudFormationRetryClient = getCloudFormationRetryClient(credentialView, networkRequest);
        List<CreatedSubnet> createdSubnetList = getCloudSubnets(networkRequest.getCloudCredential(), new ArrayList<>(networkRequest.getSubnetCidrs()));
        String cloudFormationTemplate = createTemplate(networkRequest, createdSubnetList);
        String envName = networkRequest.getEnvName();

        cloudFormationRetryClient.createStack(createStackRequest(envName, cloudFormationTemplate));

        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' ", envName);
        PollTask<Boolean> pollTask = createPollTask(credentialView, networkRequest);
        try {
            awsBackoffSyncPollingScheduler.schedule(pollTask);
        } catch (RuntimeException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }

        Map<String, String> output = cfStackUtil.getOutputs(envName, cloudFormationRetryClient);
        String vpcId = getCreatedVpc(output);
        Set<CreatedSubnet> subnets = getCreatedSubnets(output, createdSubnetList);
        return new CreatedCloudNetwork(vpcId, subnets);
    }

    private AmazonCloudFormationRetryClient getCloudFormationRetryClient(AwsCredentialView credentialView, NetworkCreationRequest networkRequest) {
        return awsClient.createCloudFormationRetryClient(credentialView, networkRequest.getRegion().value());
    }

    private String createTemplate(NetworkCreationRequest networkRequest, List<CreatedSubnet> createdSubnetList) {
        return awsNetworkCfTemplateProvider.provide(networkRequest.getNetworkCidr(), createdSubnetList);
    }

    private CreateStackRequest createStackRequest(String stackName, String cloudFormationTemplate) {
        return new CreateStackRequest()
                .withStackName(stackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cloudFormationTemplate)
                .withCapabilities(CAPABILITY_IAM);
    }

    private PollTask<Boolean> createPollTask(AwsCredentialView credentialView, NetworkCreationRequest networkRequest) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, networkRequest.getRegion().value());
        return awsPollTaskFactory.newAwsCreateNetworkStatusCheckerTask(cfClient, CREATE_COMPLETE, CREATE_FAILED, ERROR_STATUSES, networkRequest.getEnvName());
    }

    private String getCreatedVpc(Map<String, String> output) {
        if (output.containsKey(CREATED_VPC)) {
            return output.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = String.format("Vpc could not be found in the Cloudformation stack output.");
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
                subnets.add(createdSubnet);
            } else {
                String outputKeyNotFound = String.format("Subnet could not be found in the Cloudformation stack output.");
                throw new CloudConnectorException(outputKeyNotFound);
            }
        }
        return subnets;
    }

    private List<CreatedSubnet> getCloudSubnets(CloudCredential cloudCredential, List<String> subnetCidrs) {
        return awsCloudSubnetProvider.provide(awsClient.createAccess(cloudCredential), subnetCidrs);
    }

    @Override
    public CreatedCloudNetwork deleteNetworkWithSubnets(CloudCredential cloudCredential) {
        return null;
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
