package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration.cancellableWaiterConfiguration;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.handleWaiterError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsElasticIpService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsModelService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsRequest;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;

@Service
public class AwsLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLaunchService.class);

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnet";

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsNetworkService awsNetworkService;

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

    @Inject
    private AwsModelService awsModelService;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentTypeWithThreshold adjustmentTypeWithThreshold) throws Exception {
        LOGGER.info("Launch AWS cluster with adjustment and threshold: {}", adjustmentTypeWithThreshold);
        createKeyPair(ac, stack);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView, regionName);
        Network network = stack.getNetwork();
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(awsNetworkView, amazonEC2Client);
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(cFStackName).build();

        ModelContext modelContext = null;
        try {
            cfClient.describeStacks(describeStacksRequest);
            LOGGER.debug("Stack already exists: {}", cFStackName);
        } catch (AwsServiceException ignored) {
            boolean existingVPC = awsNetworkView.isExistingVPC();
            boolean existingSubnet = awsNetworkView.isExistingSubnet();
            CloudResource cloudFormationStack = CloudResource.builder()
                    .withType(ResourceType.CLOUDFORMATION_STACK)
                    .withAvailabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                    .withName(cFStackName)
                    .build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

            String cidr = network.getSubnet().getCidr();
            String subnet = isNoCIDRProvided(existingVPC, existingSubnet, cidr) ? awsNetworkService.findNonOverLappingCIDR(ac, stack) : cidr;
            modelContext = awsModelService.buildDefaultModelContext(ac, stack, resourceNotifier);
            String cfTemplate = cloudFormationTemplateBuilder.build(modelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cfClient.createStack(awsStackRequestHelper.createCreateStackRequest(ac, stack, cFStackName, subnet, cfTemplate));
        }
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        try (CloudFormationWaiter waiter = cfClient.waiters()) {
            LOGGER.debug("Waiting for CloudFormation stack {} creation", cFStackName);
            StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
            waiter.waitUntilStackCreateComplete(describeStacksRequest, cancellableWaiterConfiguration(stackCancellationCheck));
        } catch (Exception e) {
            handleWaiterError(String.format("CloudFormation stack %s creation failed.", cFStackName),
                    () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.CREATE_FAILED), e);
        }

        List<CloudResource> networkResources = saveGeneratedSubnet(ac, stack, cFStackName, cfClient, resourceNotifier);
        suspendAutoscalingGoupsWhenNewInstancesAreReady(ac, stack);

        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, regionName);
        List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cfClient, amazonASClient, stack.getGroups());

        if (mapPublicIpOnLaunch) {
            associatePublicIpsToGatewayInstances(stack, cFStackName, cfClient, amazonEC2Client, instances);
        }

        awsComputeResourceService.buildComputeResourcesForLaunch(ac, stack, adjustmentTypeWithThreshold, instances, networkResources);

        awsTaggingService.tagRootVolumes(ac, amazonEC2Client, instances, stack.getTags());

        awsCloudWatchService.addCloudWatchAlarmsForSystemFailures(instances, regionName, credentialView);

        return awsResourceConnector.check(ac, instances);
    }

    private void associatePublicIpsToGatewayInstances(CloudStack stack, String cFStackName, AmazonCloudFormationClient cfRetryClient,
            AmazonEc2Client amazonEC2Client, List<CloudResource> instances) {
        List<Group> gateways = awsNetworkService.getGatewayGroups(stack.getGroups());
        Map<String, List<String>> gatewayGroupInstanceMapping = createGatewayToInstanceMap(instances, gateways);
        setElasticIps(cFStackName, cfRetryClient, amazonEC2Client, gateways, gatewayGroupInstanceMapping);
    }

    private void setElasticIps(String cFStackName, AmazonCloudFormationClient cfRetryClient, AmazonEc2Client amazonEC2Client,
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
                AmazonEc2Client client = awsClient.createEc2Client(awsCredential, region);
                String keyPairName = awsClient.getKeyPairName(ac);
                ImportKeyPairRequest importKeyPairRequest = ImportKeyPairRequest.builder()
                        .keyName(keyPairName)
                        .publicKeyMaterial(SdkBytes.fromUtf8String(stack.getInstanceAuthentication().getPublicKey()))
                        .build();
                try {
                    client.describeKeyPairs(DescribeKeyPairsRequest.builder().keyNames(keyPairName).build());
                    LOGGER.debug("Key-pair already exists: {}", keyPairName);
                } catch (AwsServiceException e) {
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

    private List<CloudResource> saveGeneratedSubnet(AuthenticatedContext ac, CloudStack stack, String cFStackName, AmazonCloudFormationClient client,
            PersistenceNotifier resourceNotifier) {
        List<CloudResource> resources = new ArrayList<>();
        AwsNetworkView awsNetworkView = new AwsNetworkView(stack.getNetwork());
        String availabilityZone = ac.getCloudContext().getLocation().getAvailabilityZone().value();
        if (awsNetworkView.isExistingVPC()) {
            String vpcId = awsNetworkView.getExistingVpc();
            CloudResource vpc = CloudResource.builder()
                    .withType(ResourceType.AWS_VPC)
                    .withName(vpcId)
                    .withAvailabilityZone(availabilityZone)
                    .build();
            resourceNotifier.notifyAllocation(vpc, ac.getCloudContext());
            resources.add(vpc);
        } else {
            String vpcId = getCreatedVpc(cFStackName, client);
            CloudResource vpc = CloudResource.builder()
                    .withType(ResourceType.AWS_VPC)
                    .withAvailabilityZone(availabilityZone)
                    .withName(vpcId)
                    .build();
            resourceNotifier.notifyAllocation(vpc, ac.getCloudContext());
            resources.add(vpc);
        }

        if (awsNetworkView.isExistingSubnet()) {
            String subnetId = awsNetworkView.getExistingSubnet();
            CloudResource subnet = CloudResource.builder()
                    .withType(ResourceType.AWS_SUBNET)
                    .withName(subnetId)
                    .withAvailabilityZone(availabilityZone)
                    .build();
            resourceNotifier.notifyAllocation(subnet, ac.getCloudContext());
            resources.add(subnet);
        } else {
            String subnetId = getCreatedSubnet(cFStackName, client);
            CloudResource subnet = CloudResource.builder()
                    .withType(ResourceType.AWS_SUBNET)
                    .withName(subnetId)
                    .withAvailabilityZone(availabilityZone)
                    .build();
            resourceNotifier.notifyAllocation(subnet, ac.getCloudContext());
            resources.add(subnet);
        }

        return resources;
    }

    private String getCreatedVpc(String cFStackName, AmazonCloudFormationClient client) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_VPC)) {
            return outputs.get(CREATED_VPC);
        } else {
            String outputKeyNotFound = String.format("Vpc could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private String getCreatedSubnet(String cFStackName, AmazonCloudFormationClient client) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_SUBNET)) {
            return outputs.get(CREATED_SUBNET);
        } else {
            String outputKeyNotFound = String.format("Subnet could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private void suspendAutoscalingGoupsWhenNewInstancesAreReady(AuthenticatedContext ac, CloudStack stack) {
        AmazonCloudFormationClient cloudFormationClient = awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        try {
            awsAutoScalingService.scheduleStatusChecks(stack.getGroups(), ac, cloudFormationClient);
        } catch (AmazonAutoscalingFailedException amazonAutoscalingFailedException) {
            LOGGER.info("Amazon autoscaling failed", amazonAutoscalingFailedException);
            throw new CloudConnectorException(amazonAutoscalingFailedException);
        }
        awsAutoScalingService.suspendAutoScaling(ac, stack);
    }
}
