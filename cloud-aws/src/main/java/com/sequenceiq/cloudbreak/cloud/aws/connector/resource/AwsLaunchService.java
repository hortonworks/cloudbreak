package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.ImportKeyPairRequest;
import com.amazonaws.services.ec2.model.PrefixList;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.waiters.Waiter;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNetworkCfTemplateProvider;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.efs.AwsEfsFileSystem;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.LoadBalancerType;
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
    private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        createKeyPair(ac, stack);
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView, regionName);
        Network network = stack.getNetwork();
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(awsNetworkView, amazonEC2Client);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);

        ModelContext modelContext = null;
        try {
            cfClient.describeStacks(describeStacksRequest);
            LOGGER.debug("Stack already exists: {}", cFStackName);
        } catch (AmazonServiceException ignored) {
            boolean existingVPC = awsNetworkView.isExistingVPC();
            boolean existingSubnet = awsNetworkView.isExistingSubnet();
            CloudResource cloudFormationStack = new Builder().type(ResourceType.CLOUDFORMATION_STACK).name(cFStackName).build();
            resourceNotifier.notifyAllocation(cloudFormationStack, ac.getCloudContext());

            String cidr = network.getSubnet().getCidr();
            String subnet = isNoCIDRProvided(existingVPC, existingSubnet, cidr) ? awsNetworkService.findNonOverLappingCIDR(ac, stack) : cidr;
            modelContext =
                buildDefaultModelContext(ac, stack, resourceNotifier, regionName, amazonEC2Client, network, awsNetworkView, mapPublicIpOnLaunch);
            String cfTemplate = cloudFormationTemplateBuilder.build(modelContext);
            LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
            cfClient.createStack(awsStackRequestHelper.createCreateStackRequest(ac, stack, cFStackName, subnet, cfTemplate));
        }
        LOGGER.debug("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", cFStackName, ac.getCloudContext().getId());

        Waiter<DescribeStacksRequest> creationWaiter = cfClient.waiters().stackCreateComplete();
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        run(creationWaiter, describeStacksRequest, stackCancellationCheck, String.format("CloudFormation stack %s creation failed.", cFStackName),
                () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.CREATE_FAILED));

        List<CloudResource> networkResources = saveGeneratedSubnet(ac, stack, cFStackName, cfClient, resourceNotifier);
        suspendAutoscalingGoupsWhenNewInstancesAreReady(ac, stack);

        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, regionName);
        List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cfClient, amazonASClient, stack.getGroups());

        if (mapPublicIpOnLaunch) {
            associatePublicIpsToGatewayInstances(stack, cFStackName, cfClient, amazonEC2Client, instances);
        }

        awsComputeResourceService.buildComputeResourcesForLaunch(ac, stack, adjustmentType, threshold, instances, networkResources);

        awsTaggingService.tagRootVolumes(ac, amazonEC2Client, instances, stack.getTags());

        awsCloudWatchService.addCloudWatchAlarmsForSystemFailures(instances, regionName, credentialView);

        updateCloudformationWithLoadBalancers(ac, stack, resourceNotifier, modelContext, instances, regionName,
            amazonEC2Client, network, awsNetworkView, mapPublicIpOnLaunch);

        return awsResourceConnector.check(ac, instances);
    }

    @SuppressWarnings("ParameterNumber")
    private ModelContext buildDefaultModelContext(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            String regionName, AmazonEc2Client amazonEC2Client, Network network, AwsNetworkView awsNetworkView, boolean mapPublicIpOnLaunch) {

        boolean existingVPC = awsNetworkView.isExistingVPC();
        boolean existingSubnet = awsNetworkView.isExistingSubnet();

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
            .withPrefixListIds(getPrefixListIds(amazonEC2Client, regionName, network.getOutboundInternetTraffic()));

        AwsEfsFileSystem efsFileSystem = getAwsEfsFileSystem(stack);

        if (efsFileSystem != null) {
            modelContext.withEnableEfs(true);
            modelContext.withEfsFileSystem(efsFileSystem);
        } else {
            modelContext.withEnableEfs(false);
        }

        return modelContext;
    }

    // there should be at most one file system configured for EFS. return the first EFS configuration
    private AwsEfsFileSystem getAwsEfsFileSystem(CloudStack stack) {
        AwsEfsFileSystem efsFileSystem = null;

        if (stack.getFileSystem().isPresent()) {
            efsFileSystem = AwsEfsFileSystem.toAwsEfsFileSystem(stack.getFileSystem().get());
        }

        if (efsFileSystem == null && stack.getAdditionalFileSystem().isPresent()) {
            return AwsEfsFileSystem.toAwsEfsFileSystem(stack.getAdditionalFileSystem().get());
        }
        return efsFileSystem;
    }

    @VisibleForTesting
    @SuppressWarnings("ParameterNumber")
    void updateCloudformationWithLoadBalancers(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            ModelContext modelContext, List<CloudResource> instances, String regionName, AmazonEc2Client amazonEC2Client,
            Network network, AwsNetworkView awsNetworkView, boolean mapPublicIpOnLaunch) {

        List<CloudLoadBalancer> cloudLoadBalancers = stack.getLoadBalancers();
        if (!cloudLoadBalancers.isEmpty()) {
            if (modelContext == null) {
                modelContext = buildDefaultModelContext(
                    ac, stack, resourceNotifier, regionName, amazonEC2Client, network, awsNetworkView, mapPublicIpOnLaunch);
            }

            List<AwsLoadBalancer> awsLoadBalancers = getAwsLoadBalancers(cloudLoadBalancers, instances, awsNetworkView, amazonEC2Client);

            modelContext.withLoadBalancers(awsLoadBalancers);
            LOGGER.debug("Starting CloudFormation update to create load balancer and target groups.");
            ListStackResourcesResult result = updateCloudFormationStack(ac, stack, modelContext);

            setLoadBalancerMetadata(awsLoadBalancers, result);

            LOGGER.debug("Starting CloudFormation update to create listeners.");
            updateCloudFormationStack(ac, stack, modelContext);
        }
    }

    private List<AwsLoadBalancer> getAwsLoadBalancers(List<CloudLoadBalancer> cloudLoadBalancers, List<CloudResource> instances,
            AwsNetworkView awsNetworkView, AmazonEc2Client amazonEC2Client) {
        LOGGER.debug("Converting internal load balancer model to AWS cloud provider model.");
        List<AwsLoadBalancer> awsLoadBalancers = new ArrayList<>();
        for (CloudLoadBalancer cloudLoadBalancer : cloudLoadBalancers) {
            LOGGER.debug("Found load balancer model of type {}", cloudLoadBalancer.getType());
            AwsLoadBalancer loadBalancer = convertLoadBalancer(cloudLoadBalancer, instances, awsNetworkView, amazonEC2Client, awsLoadBalancers);
            if (loadBalancer != null && !awsLoadBalancers.contains(loadBalancer)) {
                awsLoadBalancers.add(loadBalancer);
            }
        }

        Set<String> requestedTypes = cloudLoadBalancers.stream()
            .map(lb -> lb.getType().name())
            .collect(Collectors.toSet());
        Set<String> awsTypes = awsLoadBalancers.stream()
            .map(lb -> AwsLoadBalancerScheme.INTERNAL.awsScheme().equals(lb.getAwsScheme()) ? "PRIVATE" : "PUBLIC")
            .collect(Collectors.toSet());
        if (!requestedTypes.equals(awsTypes)) {
            throw new CloudConnectorException(String.format("Can not create all requested AWS load balancers. " +
                "Types requested: [%s]; type to be created: [%s]", requestedTypes, awsTypes));
        }

        return awsLoadBalancers;
    }

    @VisibleForTesting
    void setLoadBalancerMetadata(List<AwsLoadBalancer> awsLoadBalancers, ListStackResourcesResult result) {
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            LOGGER.debug("Processing load balancer {}", loadBalancer.getName());
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Processing listener {} and target group {}", listener.getName(), listener.getTargetGroup().getName());
                Optional<StackResourceSummary> targetGroupSummary = result.getStackResourceSummaries().stream()
                    .filter(stackResourceSummary -> listener.getTargetGroup().getName().equals(stackResourceSummary.getLogicalResourceId()))
                    .findFirst();
                if (targetGroupSummary.isEmpty()) {
                    throw new CloudConnectorException(String.format("Could not create load balancer listeners: target group %s not found.",
                        listener.getTargetGroup().getName()));
                }
                if (StringUtils.isEmpty(targetGroupSummary.get().getPhysicalResourceId())) {
                    throw new CloudConnectorException(String.format("Could not create load balancer listeners: target group %s arn not found.",
                        listener.getTargetGroup().getName()));
                }
                listener.getTargetGroup().setArn(targetGroupSummary.get().getPhysicalResourceId());
                LOGGER.debug("Found arn {} for target group {}", listener.getTargetGroup().getArn(), listener.getTargetGroup().getName());
            }
            Optional<StackResourceSummary> loadBalancerSummary = result.getStackResourceSummaries().stream()
                .filter(stackResourceSummary -> loadBalancer.getName().equals(stackResourceSummary.getLogicalResourceId()))
                .findFirst();
            if (loadBalancerSummary.isEmpty()) {
                throw new CloudConnectorException(String.format("Could not create load balancer listeners: load balancer %s not found.",
                    loadBalancer.getName()));
            }
            if (StringUtils.isEmpty(loadBalancerSummary.get().getPhysicalResourceId())) {
                throw new CloudConnectorException(String.format("Could not create load balancer listeners: load balancer %s arn not found.",
                    loadBalancer.getName()));
            }
            loadBalancer.setArn(loadBalancerSummary.get().getPhysicalResourceId());
            loadBalancer.validateListenerConfigIsSet();
            LOGGER.debug("Found arn {} for load balancer {}", loadBalancer.getArn(), loadBalancer.getName());
        }
    }

    @VisibleForTesting
    AwsLoadBalancer convertLoadBalancer(CloudLoadBalancer cloudLoadBalancer, List<CloudResource> instances,
            AwsNetworkView awsNetworkView, AmazonEc2Client amazonEC2Client, List<AwsLoadBalancer> awsLoadBalancers) {
        // Check and see if we already have a load balancer whose scheme matches this one.
        AwsLoadBalancer currentLoadBalancer = null;
        LoadBalancerType cloudLbType = cloudLoadBalancer.getType();
        Set<String> subnetIds = selectLoadBalancerSubnetIds(cloudLbType, awsNetworkView);
        AwsLoadBalancerScheme scheme = determinePublicVsPrivateSchema(subnetIds, awsNetworkView.getExistingVpc(), amazonEC2Client);

        if ((AwsLoadBalancerScheme.INTERNAL.equals(scheme) && LoadBalancerType.PUBLIC.equals(cloudLbType)) ||
                (AwsLoadBalancerScheme.INTERNET_FACING.equals(scheme) && LoadBalancerType.PRIVATE.equals(cloudLbType))) {
            LOGGER.debug("Discovered mismatch between load balancer type and subnet type. Load balancer type is {}, " +
                "and subnet type is {}. Load balancer will not be created.", cloudLbType,
                AwsLoadBalancerScheme.INTERNAL.equals(scheme) ? "PRIVATE" : "PUBLIC");
        } else {
            Optional<AwsLoadBalancer> existingLoadBalancer = awsLoadBalancers.stream()
                .filter(lb -> lb.getScheme() == scheme)
                .findFirst();

            if (existingLoadBalancer.isPresent()) {
                currentLoadBalancer = existingLoadBalancer.get();
            } else {
                currentLoadBalancer = new AwsLoadBalancer(scheme);
            }

            currentLoadBalancer.addSubnets(subnetIds);
            setupLoadBalancer(cloudLoadBalancer, instances, currentLoadBalancer);
        }
        return currentLoadBalancer;
    }

    @VisibleForTesting
    Set<String> selectLoadBalancerSubnetIds(LoadBalancerType type, AwsNetworkView awsNetworkView) {
        List<String> subnetIds;
        if (type == LoadBalancerType.PRIVATE) {
            LOGGER.debug("Private load balancer detected. Using instance subnet for load balancer creation.");
            subnetIds = awsNetworkView.getSubnetList();
        } else {
            subnetIds = awsNetworkView.getEndpointGatewaySubnetList();
            LOGGER.debug("Public load balancer detected. Using endpoint gateway subnet for load balancer creation.");
            if (subnetIds.isEmpty()) {
                LOGGER.debug("Endpoint gateway subnet is not set. Falling back to instance subnet for load balancer creation.");
                subnetIds = awsNetworkView.getSubnetList();
            }
        }
        if (subnetIds.isEmpty()) {
            throw new CloudConnectorException("Unable to configure load balancer: Could not identify subnets.");
        }
        return new HashSet<>(subnetIds);
    }

    private AwsLoadBalancerScheme determinePublicVsPrivateSchema(Set<String> subnetIds, String vpcId, AmazonEc2Client amazonEC2Client) {
        DescribeRouteTablesRequest describeRouteTablesRequest = new DescribeRouteTablesRequest()
                .withFilters(new Filter().withName("vpc-id").withValues(vpcId));
        List<RouteTable> routeTableList = AwsPageCollector.getAllRouteTables(amazonEC2Client, describeRouteTablesRequest);
        return subnetIds.stream()
            .anyMatch(subnetId -> awsSubnetIgwExplorer.hasInternetGatewayOfSubnet(routeTableList, subnetId, vpcId))
                ? AwsLoadBalancerScheme.INTERNET_FACING : AwsLoadBalancerScheme.INTERNAL;
    }

    private void setupLoadBalancer(CloudLoadBalancer cloudLoadBalancer, List<CloudResource> instances,
            AwsLoadBalancer awsLoadBalancer) {
        for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : cloudLoadBalancer.getPortToTargetGroupMapping().entrySet()) {
            AwsListener listener = awsLoadBalancer.getOrCreateListener(entry.getKey().getTrafficPort(), entry.getKey().getHealthCheckPort());
            List<CloudResource> lbTargetInstances = instances.stream()
                .filter(instance -> entry.getValue().stream().anyMatch(tg -> tg.getName().equals(instance.getGroup())))
                .collect(Collectors.toList());
            Set<String> instanceIds = lbTargetInstances.stream().map(CloudResource::getInstanceId).collect(Collectors.toSet());
            listener.addInstancesToTargetGroup(instanceIds);
        }
    }

    private ListStackResourcesResult updateCloudFormationStack(AuthenticatedContext ac, CloudStack stack, ModelContext modelContext) {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);

        String cfTemplate = cloudFormationTemplateBuilder.build(modelContext);
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
        cfClient.updateStack(awsStackRequestHelper.createUpdateStackRequest(ac, stack, cFStackName, cfTemplate));

        Waiter<DescribeStacksRequest> updateWaiter = cfClient.waiters().stackUpdateComplete();
        StackCancellationCheck stackCancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        run(updateWaiter, describeStacksRequest, stackCancellationCheck, String.format("CloudFormation stack %s update failed.", cFStackName),
            () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.UPDATE_FAILED));

        return cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
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

    private List<CloudResource> saveGeneratedSubnet(AuthenticatedContext ac, CloudStack stack, String cFStackName, AmazonCloudFormationClient client,
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
        } catch (AmazonAutoscalingFailed amazonAutoscalingFailed) {
            LOGGER.info("Amazon autoscaling failed", amazonAutoscalingFailed);
            throw new CloudConnectorException(amazonAutoscalingFailed);
        }
        awsAutoScalingService.suspendAutoScaling(ac, stack);
    }

    private List<String> getPrefixListIds(AmazonEc2Client amazonEC2Client, String regionName, OutboundInternetTraffic outboundInternetTraffic) {
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
