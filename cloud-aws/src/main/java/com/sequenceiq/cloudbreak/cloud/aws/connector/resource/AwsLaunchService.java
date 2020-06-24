package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.PrefixList;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNetworkCfTemplateProvider;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLaunchService.class);

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnet";

    @Value("${cb.aws.vpcendpoints.enabled.gateway.services}")
    private Set<String> enabledGatewayServices;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsNetworkService awsNetworkService;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsAutoScalingService awsAutoScalingService;

    @Inject
    private AwsElasticIpService awsElasticIpService;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        createKeyPair(ac, stack);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationRetryClient cfRetryClient = awsClient.createCloudFormationRetryClient(credentialView, regionName);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
        Network network = stack.getNetwork();
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(awsNetworkView, amazonEC2Client);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
        try {
            cfRetryClient.describeStacks(describeStacksRequest);
            LOGGER.debug("Stack already exists: {}", cFStackName);
        } catch (AmazonServiceException ignored) {
            boolean existingVPC = awsNetworkView.isExistingVPC();
            boolean existingSubnet = awsNetworkView.isExistingSubnet();
            CloudResource cloudFormationStack = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

            String cidr = network.getSubnet().getCidr();
            String subnet = isNoCIDRProvided(existingVPC, existingSubnet, cidr) ? awsNetworkService.findNonOverLappingCIDR(ac, stack) : cidr;
            AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
            ModelContext modelContext = new ModelContext()
                    .withAuthenticatedContext(ac)
                    .withStack(stack)
                    .withExistingVpc(existingVPC)
                    .withExistingIGW(awsNetworkView.isExistingIGW())
                    .withExistingSubnetCidr(existingSubnet ? awsNetworkService.getExistingSubnetCidr(ac, stack) : null)
                    .withExistinVpcCidr(awsNetworkService.getVpcCidrs(ac, stack))
                    .withExistingSubnetIds(existingSubnet ? awsNetworkView.getSubnetList() : null)
                    .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
                    .withEnableInstanceProfile(awsInstanceProfileView.isInstanceProfileAvailable())
                    .withInstanceProfileAvailable(awsInstanceProfileView.isInstanceProfileAvailable())
                    .withTemplate(stack.getTemplate())
                    .withDefaultSubnet(subnet)
                    .withOutboundInternetTraffic(network.getOutboundInternetTraffic())
                    .withVpcCidrs(network.getNetworkCidrs())
                    .withPrefixListIds(getPrefixListIds(amazonEC2Client, regionName, network.getOutboundInternetTraffic()))
                    .withEncryptedAMIByGroupName(encryptedImageCopyService.createEncryptedImages(ac, stack, resourceNotifier));
            String cfTemplate = cloudFormationTemplateBuilder.build(modelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cfRetryClient.createStack(awsStackRequestHelper.createCreateStackRequest(ac, stack, cFStackName, subnet, cfTemplate));
        }
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        Waiter<DescribeStacksRequest> creationWaiter = cfClient.waiters().stackCreateComplete();
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        run(creationWaiter, describeStacksRequest, stackCancellationCheck);

        List<CloudResource> networkResources = saveGeneratedSubnet(ac, stack, cFStackName, cfRetryClient, resourceNotifier);
        suspendAutoscalingGoupsWhenNewInstancesAreReady(ac, stack);

        AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(credentialView, regionName);
        List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cfRetryClient, amazonASClient, stack.getGroups());

        if (mapPublicIpOnLaunch) {
            associatePublicIpsToGatewayInstances(stack, cFStackName, cfRetryClient, amazonEC2Client, instances);
        }

        awsComputeResourceService.buildComputeResourcesForLaunch(ac, stack, adjustmentType, threshold, instances, networkResources);

        awsTaggingService.tagRootVolumes(ac, amazonEC2Client, instances, stack.getTags());

        awsCloudWatchService.addCloudWatchAlarmsForSystemFailures(instances, stack, regionName, credentialView);

        return awsResourceConnector.check(ac, instances);
    }

    private void associatePublicIpsToGatewayInstances(CloudStack stack, String cFStackName, AmazonCloudFormationRetryClient cfRetryClient,
            AmazonEC2Client amazonEC2Client, List<CloudResource> instances) {
        List<Group> gateways = awsNetworkService.getGatewayGroups(stack.getGroups());
        Map<String, List<String>> gatewayGroupInstanceMapping = createGatewayToInstanceMap(instances, gateways);
        setElasticIps(cFStackName, cfRetryClient, amazonEC2Client, gateways, gatewayGroupInstanceMapping);
    }

    private void setElasticIps(String cFStackName, AmazonCloudFormationRetryClient cfRetryClient, AmazonEC2Client amazonEC2Client,
            List<Group> gateways, Map<String, List<String>> gatewayGroupInstanceMapping) {
        Map<String, String> eipAllocationIds = awsElasticIpService.getElasticIpAllocationIds(cfStackUtil.getOutputs(cFStackName, cfRetryClient), cFStackName);
        for (Group gateway : gateways) {
            List<String> eips = awsElasticIpService.getEipsForGatewayGroup(eipAllocationIds, gateway);
            List<String> instanceIds = gatewayGroupInstanceMapping.get(gateway.getName());
            awsElasticIpService.associateElasticIpsToInstances(amazonEC2Client, eips, instanceIds);
        }
    }

    private Map<String, List<String>> createGatewayToInstanceMap(List<CloudResource> instances, List<Group> gateways) {
        return instances.stream()
                .filter(instance -> gateways.stream().anyMatch(gw -> gw.getName().equals(instance.getGroup())))
                .collect(Collectors.toMap(
                        CloudResource::getGroup,
                        instance -> List.of(instance.getInstanceId()),
                        (listOne, listTwo) -> Stream.concat(listOne.stream(), listTwo.stream()).collect(Collectors.toList())));
    }

    private void createKeyPair(AuthenticatedContext ac, CloudStack stack) {
        if (!awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
            try {
                String region = ac.getCloudContext().getLocation().getRegion().value();
                LOGGER.debug("Importing public key to {} region on AWS", region);
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                String keyPairName = awsClient.getKeyPairName(ac);
                ImportKeyPairRequest importKeyPairRequest = new ImportKeyPairRequest(keyPairName, stack.getInstanceAuthentication().getPublicKey());
                try {
                    client.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(keyPairName));
                    LOGGER.debug("Key-pair already exists: {}", keyPairName);
                } catch (AmazonServiceException e) {
                    client.importKeyPair(importKeyPairRequest);
                }
            } catch (Exception e) {
                String errorMessage = String.format("Failed to import public key [roleArn:'%s'], detailed message: %s", awsCredential.getRoleArn(),
                        e.getMessage());
                LOGGER.info(errorMessage, e);
                throw new CloudConnectorException(e.getMessage(), e);
            }
        }
    }

    private boolean isNoCIDRProvided(boolean existingVPC, boolean existingSubnet, String cidr) {
        return existingVPC && !existingSubnet && cidr == null;
    }

    private List<CloudResource> saveGeneratedSubnet(AuthenticatedContext ac, CloudStack stack, String cFStackName, AmazonCloudFormationRetryClient client,
            PersistenceNotifier resourceNotifier) {
        List<CloudResource> resources = new ArrayList<>();
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        if (awsNetworkView.isExistingVPC()) {
            String vpcId = awsNetworkView.getExistingVpc();
            CloudResource vpc = new Builder().type(ResourceType.AWS_VPC).name(vpcId).build();
            resourceNotifier.notifyAllocation(vpc, ac.getCloudContext());
            resources.add(vpc);
        } else {
            String vpcId = getCreatedVpc(cFStackName, client);
            CloudResource vpc = new Builder().type(ResourceType.AWS_VPC).name(vpcId).build();
            resourceNotifier.notifyAllocation(vpc, ac.getCloudContext());
            resources.add(vpc);
        }

        if (awsNetworkView.isExistingSubnet()) {
            String subnetId = awsNetworkView.getExistingSubnet();
            CloudResource subnet = new Builder().type(ResourceType.AWS_SUBNET).name(subnetId).build();
            resourceNotifier.notifyAllocation(subnet, ac.getCloudContext());
            resources.add(subnet);
        } else {
            String subnetId = getCreatedSubnet(cFStackName, client);
            CloudResource subnet = new Builder().type(ResourceType.AWS_SUBNET).name(subnetId).build();
            resourceNotifier.notifyAllocation(subnet, ac.getCloudContext());
            resources.add(subnet);
        }

        return resources;
    }

    private String getCreatedVpc(String cFStackName, AmazonCloudFormationRetryClient client) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_VPC)) {
            return outputs.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = String.format("Vpc could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private String getCreatedSubnet(String cFStackName, AmazonCloudFormationRetryClient client) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_SUBNET)) {
            return outputs.get(CREATED_SUBNET);
        } else {
            String outputKeyNotFound = String.format("Subnet could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private void suspendAutoscalingGoupsWhenNewInstancesAreReady(AuthenticatedContext ac, CloudStack stack) {
        AmazonCloudFormationRetryClient cloudFormationClient = awsClient.createCloudFormationRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        try {
            awsAutoScalingService.scheduleStatusChecks(stack.getGroups(), ac, cloudFormationClient);
        } catch (AmazonAutoscalingFailed amazonAutoscalingFailed) {
            LOGGER.info("Amazon autoscaling failed", amazonAutoscalingFailed);
            throw new CloudConnectorException(amazonAutoscalingFailed);
        }
        awsAutoScalingService.suspendAutoScaling(ac, stack);
    }

    private List<String> getPrefixListIds(AmazonEC2Client amazonEC2Client, String regionName, OutboundInternetTraffic outboundInternetTraffic) {
        List<String> result = List.of();
        if (outboundInternetTraffic == OutboundInternetTraffic.DISABLED && CollectionUtils.isNotEmpty(enabledGatewayServices)) {
            Set<String> gatewayRegionServices = enabledGatewayServices.stream()
                    .map(s -> String.format(AwsNetworkCfTemplateProvider.VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, regionName, s))
                    .collect(Collectors.toSet());
            result = amazonEC2Client.describePrefixLists().getPrefixLists().stream()
                    .filter(pl -> gatewayRegionServices.contains(pl.getPrefixListName()))
                    .map(PrefixList::getPrefixListId)
                    .collect(Collectors.toList());
        }
        return result;
    }
}
