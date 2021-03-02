package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.OnFailure;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.Tag;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ec2.model.VpcCidrBlockAssociation;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.DefaultNetworkConnector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.EnvironmentCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetFilterStrategyType;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.NetworkDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;

@Service
public class AwsNetworkConnector implements DefaultNetworkConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNetworkConnector.class);

    private static final String CREATED_VPC = "CreatedVpc";

    private static final int NOT_FOUND = 400;

    @Value("${cb.aws.subnet.ha.different.az.min:2}")
    private int minSubnetCountInDifferentAz;

    @Value("${cb.aws.subnet.ha.different.az.max:3}")
    private int maxSubnetCountInDifferentAz;

    @Inject
    private AwsNetworkCfTemplateProvider awsNetworkCfTemplateProvider;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsSubnetRequestProvider awsSubnetRequestProvider;

    @Inject
    private AwsCreatedSubnetProvider awsCreatedSubnetProvider;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    @Inject
    private Map<SubnetFilterStrategyType, SubnetFilterStrategy> subnetFilterStrategyMap;

    @Override
    public CreatedCloudNetwork createNetworkWithSubnets(NetworkCreationRequest networkRequest) {
        AwsCredentialView credentialView = new AwsCredentialView(networkRequest.getCloudCredential());
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(credentialView, networkRequest.getRegion().value());
        List<SubnetRequest> subnetRequests = getCloudSubNets(networkRequest);
        String cfStackName = networkRequest.getStackName();
        try {
            cloudFormationClient.describeStacks(new DescribeStacksRequest().withStackName(cfStackName));
            LOGGER.warn("AWS CloudFormation stack for Network with stack name: '{}' already exists. Attaching this one to the network.", cfStackName);
            return getCreatedNetworkWithPolling(networkRequest, credentialView, cloudFormationClient, subnetRequests);
        } catch (AmazonServiceException e) {
            if (networkDoesNotExist(e)) {
                LOGGER.warn("{} occurred during describe AWS CloudFormation stack for Network with stack name: '{}'. "
                        + "Assuming the CF Stack does not exist, so creating a new one. Exception message: {}", e.getClass(), cfStackName, e.getMessage());
                String cloudFormationTemplate = createTemplate(networkRequest, subnetRequests);
                return createNewCfNetworkStack(networkRequest, credentialView, cloudFormationClient, cloudFormationTemplate, subnetRequests);
            } else {
                String region = networkRequest.getRegion().getRegionName();
                String errorReason = awsCloudFormationErrorMessageProvider.getErrorReason(credentialView, region, cfStackName, ResourceStatus.CREATE_FAILED);
                String message = String.format("Failed to create network: %s", errorReason);
                LOGGER.debug(message, e);
                throw new CloudConnectorException(message, e);
            }
        }
    }

    @Override
    public NetworkCidr getNetworkCidr(Network network, CloudCredential credential) {
        AwsCredentialView awsCredentialView = new AwsCredentialView(credential);
        AmazonEc2Client awsClientAccess = awsClient.createEc2Client(awsCredentialView, network.getStringParameter(AwsNetworkView.REGION));
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        String existingVpc = awsNetworkView.getExistingVpc();
        DescribeVpcsResult describeVpcsResult = awsClientAccess.describeVpcs(new DescribeVpcsRequest().withVpcIds(existingVpc));
        List<String> vpcCidrs = new ArrayList<>();
        for (Vpc vpc : describeVpcsResult.getVpcs()) {
            if (vpc.getCidrBlockAssociationSet() != null) {
                LOGGER.info("The VPC {} has associated CIDR block so using the CIDR blocks in the VPC.", vpc.getVpcId());
                List<String> cidrs = vpc.getCidrBlockAssociationSet()
                        .stream()
                        .map(VpcCidrBlockAssociation::getCidrBlock)
                        .collect(Collectors.toList());
                LOGGER.info("The VPC {} CIDRs block are {}.", vpc.getVpcId(), cidrs);
                vpcCidrs.addAll(cidrs);
            } else {
                LOGGER.info("The VPC {} has no associated CIDR block so using the CIDR block in the VPC.", vpc.getVpcId());
                vpcCidrs.add(vpc.getCidrBlock());
            }
        }

        if (vpcCidrs.isEmpty()) {
            throw new BadRequestException("VPC cidr could not fetch from AWS: " + existingVpc);
        }
        if (vpcCidrs.size() > 1) {
            LOGGER.info("More than one vpc cidrs for VPC {}. We will use the first one: {}", existingVpc, vpcCidrs.get(0));
        }
        return new NetworkCidr(vpcCidrs.get(0), vpcCidrs);
    }

    @Override
    public SubnetSelectionResult filterSubnets(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        boolean preferPrivate = subnetSelectionParameters.isPreferPrivateIfExist() || subnetSelectionParameters.getTunnel().useCcm();

        SubnetFilterStrategyType subnetSelectorStrategyType = preferPrivate ?
                SubnetFilterStrategyType.MULTIPLE_PREFER_PRIVATE : SubnetFilterStrategyType.MULTIPLE_PREFER_PUBLIC;
        int azCount = subnetSelectionParameters.isHa() ? subnetCountInDifferentAzMin() : 1;
        return subnetFilterStrategyMap.get(subnetSelectorStrategyType).filter(subnetMetas, azCount);
    }

    @Override
    public int subnetCountInDifferentAzMin() {
        return minSubnetCountInDifferentAz;
    }

    @Override
    public int subnetCountInDifferentAzMax() {
        return maxSubnetCountInDifferentAz;
    }

    private boolean networkDoesNotExist(AmazonServiceException e) {
        return e.getStatusCode() == NOT_FOUND && e.getErrorMessage().contains("does not exist");
    }

    private List<SubnetRequest> getCloudSubNets(NetworkCreationRequest networkRequest) {
        AwsCredentialView awsCredential = new AwsCredentialView(networkRequest.getCloudCredential());
        AmazonEc2Client awsClientAccess = awsClient.createEc2Client(awsCredential, networkRequest.getRegion().value());
        return awsSubnetRequestProvider.provide(
                awsClientAccess,
                new ArrayList<>(networkRequest.getPublicSubnets()),
                new ArrayList<>(networkRequest.getPrivateSubnets()));
    }

    private String createTemplate(NetworkCreationRequest networkRequest, List<SubnetRequest> subnetRequestList) {
        return awsNetworkCfTemplateProvider.provide(networkRequest, subnetRequestList);
    }

    private CreatedCloudNetwork createNewCfNetworkStack(
            NetworkCreationRequest networkRequest,
            AwsCredentialView credentialView,
            AmazonCloudFormationClient cloudFormationRetryClient,
            String cloudFormationTemplate, List<SubnetRequest> subnetRequests) {

        cloudFormationRetryClient.createStack(createStackRequest(networkRequest.getStackName(),
                cloudFormationTemplate,
                networkRequest.getTags(),
                networkRequest.getCreatorCrn()));
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' ", networkRequest.getStackName());
        return getCreatedNetworkWithPolling(networkRequest, credentialView, cloudFormationRetryClient, subnetRequests);
    }

    private CreatedCloudNetwork getCreatedNetworkWithPolling(NetworkCreationRequest networkRequest, AwsCredentialView credentialView,
        AmazonCloudFormationClient cloudFormationRetryClient, List<SubnetRequest> subnetRequests) {

        String regionName = networkRequest.getRegion().value();
        Waiter<DescribeStacksRequest> creationWaiter = cloudFormationRetryClient.waiters().stackCreateComplete();
        String cfStackName = networkRequest.getStackName();
        DescribeStacksRequest stackRequestWithStackId = new DescribeStacksRequest().withStackName(cfStackName);
        EnvironmentCancellationCheck environmentCancellationCheck = new EnvironmentCancellationCheck(networkRequest.getEnvId(), networkRequest.getEnvName());

        run(creationWaiter, stackRequestWithStackId, environmentCancellationCheck,
                String.format("Network creation failed (cloudformation stack: %s).", cfStackName),
                () -> awsCloudFormationErrorMessageProvider.getErrorReason(credentialView, regionName, cfStackName, ResourceStatus.CREATE_FAILED));

        return getCreatedCloudNetwork(cloudFormationRetryClient, networkRequest, subnetRequests);
    }

    private CreatedCloudNetwork getCreatedCloudNetwork(AmazonCloudFormationClient cloudFormationRetryClient, NetworkCreationRequest networkRequest,
        List<SubnetRequest> subnetRequests) {
        Map<String, String> output = cfStackUtil.getOutputs(networkRequest.getStackName(), cloudFormationRetryClient);
        String vpcId = getCreatedVpc(output);
        Set<CreatedSubnet> subnets = awsCreatedSubnetProvider.provide(output, subnetRequests, networkRequest.isPrivateSubnetEnabled());
        return new CreatedCloudNetwork(networkRequest.getStackName(), vpcId, subnets);
    }

    private CreateStackRequest createStackRequest(String stackName, String cloudFormationTemplate, Map<String, String> tags, String creatorUser) {
        Collection<Tag> awsTags = awsTaggingService.prepareCloudformationTags(null, tags);
        return new CreateStackRequest()
                .withStackName(stackName)
                .withOnFailure(OnFailure.DO_NOTHING)
                .withTemplateBody(cloudFormationTemplate)
                .withTags(awsTags)
                .withCapabilities(CAPABILITY_IAM);
    }

    private String getCreatedVpc(Map<String, String> output) {
        if (output.containsKey(CREATED_VPC)) {
            return output.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = "Vpc could not be found in the CloudFormation stack output.";
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    @Override
    public void deleteNetworkWithSubnets(NetworkDeletionRequest networkDeletionRequest) {
        if (!networkDeletionRequest.isExisting()) {
            AwsCredentialView credentialView = new AwsCredentialView(networkDeletionRequest.getCloudCredential());
            String regionName = networkDeletionRequest.getRegion();
            AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
            String stackName = networkDeletionRequest.getStackName();
            deleteStackRequest.setStackName(stackName);
            cfClient.deleteStack(deleteStackRequest);
            Waiter<DescribeStacksRequest> deletionWaiter = cfClient.waiters().stackDeleteComplete();
            LOGGER.debug("CloudFormation stack deletion request sent with stack name: '{}' ", stackName);
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
            run(deletionWaiter, describeStacksRequest, null,
                    String.format("Network delete failed (cloudformation: %s)", stackName),
                    () -> awsCloudFormationErrorMessageProvider.getErrorReason(credentialView, regionName, stackName, ResourceStatus.DELETE_FAILED));
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