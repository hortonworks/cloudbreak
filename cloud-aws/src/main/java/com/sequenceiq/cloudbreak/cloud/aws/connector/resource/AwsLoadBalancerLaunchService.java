package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.ListStackResourcesResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.StackResourceSummary;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.waiters.Waiter;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.AwsSubnetIgwExplorer;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.loadbalancer.AwsLoadBalancerScheme;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsLoadBalancerLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLoadBalancerLaunchService.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsModelService awsModelService;

    @Inject
    private AwsSubnetIgwExplorer awsSubnetIgwExplorer;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    public List<CloudResourceStatus> updateCloudformationWithLoadBalancers(AuthenticatedContext ac, CloudStack stack,
            PersistenceNotifier resourceNotifier) {
        return updateCloudformationWithLoadBalancers(ac, stack, resourceNotifier, null);
    }

    public List<CloudResourceStatus> updateCloudformationWithLoadBalancers(AuthenticatedContext ac, CloudStack stack,
            PersistenceNotifier resourceNotifier, ModelContext modelContext) {
        List<CloudResourceStatus> statuses = new ArrayList<>();
        List<CloudLoadBalancer> cloudLoadBalancers = stack.getLoadBalancers();
        String cFStackName = cfStackUtil.getCfStackName(ac);
        if (!cloudLoadBalancers.isEmpty()) {
            LOGGER.debug("Creating load balancers of types " + cloudLoadBalancers.stream().map(CloudLoadBalancer::getType));

            AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
            String regionName = ac.getCloudContext().getLocation().getRegion().value();
            AmazonCloudFormationClient cfRetryClient = awsClient.createCloudFormationClient(credentialView, regionName);
            AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, regionName);
            List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cfRetryClient, amazonASClient, stack.getGroups());
            AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView, regionName);
            Network network = stack.getNetwork();
            AwsNetworkView awsNetworkView = new AwsNetworkView(network);

            if (modelContext == null) {
                LOGGER.debug("Creating default model context");
                modelContext = awsModelService.buildDefaultModelContext(ac, stack, resourceNotifier);
            }

            List<AwsLoadBalancer> awsLoadBalancers = getAwsLoadBalancers(cloudLoadBalancers, instances, awsNetworkView, amazonEC2Client);

            modelContext.withLoadBalancers(awsLoadBalancers);
            LOGGER.debug("Starting CloudFormation update to create load balancer and target groups.");

            ListStackResourcesResult result;
            if (checkForLoadBalancerAndTargetGroupResources(cfRetryClient, cFStackName, awsLoadBalancers)) {
                LOGGER.debug("Load balancer and target group resources already exist, skipping creation");
                result = cfRetryClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
            } else {
                result = updateCloudFormationStack(ac, stack, modelContext, awsLoadBalancers);
            }

            setLoadBalancerMetadata(awsLoadBalancers, result);

            LOGGER.debug("Starting CloudFormation update to create listeners.");
            if (checkForListenerResources(cfRetryClient, cFStackName, awsLoadBalancers)) {
                LOGGER.debug("Listener resources already exist, skipping creation");
                result = cfRetryClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
            } else {
                result = updateCloudFormationStack(ac, stack, modelContext, awsLoadBalancers);
            }

            ListStackResourcesResult finalResult = result;
            awsLoadBalancers.forEach(lb -> statuses.add(createLoadBalancerStatus(lb, finalResult)));
        } else {
            LOGGER.debug("No load balancers in stack");
        }

        return statuses;
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
        List<StackResourceSummary> summaries = result.getStackResourceSummaries();
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            LOGGER.debug("Processing load balancer {}", loadBalancer.getName());
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Processing listener {} and target group {}", listener.getName(), listener.getTargetGroup().getName());
                Optional<StackResourceSummary> targetGroupSummary = summaries.stream()
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
            Optional<StackResourceSummary> loadBalancerSummary = summaries.stream()
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
        LOGGER.debug("Configuring target instances for listeners.");
        for (Map.Entry<TargetGroupPortPair, Set<Group>> entry : cloudLoadBalancer.getPortToTargetGroupMapping().entrySet()) {
            AwsListener listener = awsLoadBalancer.getOrCreateListener(entry.getKey().getTrafficPort(), entry.getKey().getHealthCheckPort());
            List<CloudResource> lbTargetInstances = instances.stream()
                .filter(instance -> entry.getValue().stream().anyMatch(tg -> tg.getName().equals(instance.getGroup())))
                .collect(Collectors.toList());
            Set<String> instanceIds = lbTargetInstances.stream().map(CloudResource::getInstanceId).collect(Collectors.toSet());
            LOGGER.debug(String.format("Adding instances %s to listener %s", instanceIds, listener.getName()));
            listener.addInstancesToTargetGroup(instanceIds);
        }
    }

    private ListStackResourcesResult updateCloudFormationStack(AuthenticatedContext ac, CloudStack stack, ModelContext modelContext,
            List<AwsLoadBalancer> awsLoadBalancers) {
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

    private CloudResourceStatus createLoadBalancerStatus(AwsLoadBalancer loadBalancer, ListStackResourcesResult result) {
        LOGGER.debug(String.format("Checking final status of AWS load balancer %s", loadBalancer.getName()));
        List<StackResourceSummary> summaries = result.getStackResourceSummaries();
        boolean createSuccess = isResourceStatusGood(summaries, loadBalancer.getName());

        for (AwsListener listener : loadBalancer.getListeners()) {
            LOGGER.debug(String.format("Checking final status of AWS listener %s", listener.getName()));
            createSuccess = createSuccess && isResourceStatusGood(summaries, listener.getName());
            LOGGER.debug(String.format("Checking final status of AWS target group %s", listener.getTargetGroup().getName()));
            createSuccess = createSuccess && isResourceStatusGood(summaries, listener.getTargetGroup().getName());
        }

        CloudResource.Builder cloudResource = new CloudResource.Builder()
            .type(ResourceType.ELASTIC_LOAD_BALANCER)
            .status(createSuccess ? CommonStatus.CREATED : CommonStatus.FAILED)
            .name(loadBalancer.getName());

        return new CloudResourceStatus(cloudResource.build(),
            createSuccess ? com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED :
                com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED);
    }

    private boolean isResourceStatusGood(List<StackResourceSummary> summaries, String name) {
        Optional<StackResourceSummary> summary = summaries.stream()
            .filter(stackResourceSummary -> name.equals(stackResourceSummary.getLogicalResourceId()))
            .findFirst();

        boolean success = true;
        if (summary.isEmpty()) {
            LOGGER.error(String.format("Could not fetch summary for AWS resource with name %s", name));
            success = false;
        } else if (isFailedStatus(summary.get().getResourceStatus())) {
            LOGGER.error(String.format("Resource %s creation failed. Reason: %s",
                name, summary.get().getResourceStatusReason()));
            success = false;
        }
        return success;
    }

    private boolean isFailedStatus(String status) {
        return "CREATE_FAILED".equals(status) || "UPDATE_FAILED".equals(status);
    }

    @VisibleForTesting
    boolean checkForLoadBalancerAndTargetGroupResources(AmazonCloudFormationClient cfClient, String cFStackName,
            List<AwsLoadBalancer> awsLoadBalancers) {
        ListStackResourcesResult result = cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));

        boolean resourcesFound = true;
        List<StackResourceSummary> summaries = result.getStackResourceSummaries();
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            LOGGER.debug("Checking to see if load balancer resource {} already exists", loadBalancer.getName());
            resourcesFound = resourcesFound && summaries.stream()
                .anyMatch(stackResourceSummary -> loadBalancer.getName().equals(stackResourceSummary.getLogicalResourceId()));
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Checking to see if target group resource {} already exists", listener.getTargetGroup().getName());
                resourcesFound = resourcesFound && summaries.stream()
                    .anyMatch(stackResourceSummary -> listener.getTargetGroup().getName().equals(stackResourceSummary.getLogicalResourceId()));
            }
        }

        return resourcesFound;
    }

    @VisibleForTesting
    boolean checkForListenerResources(AmazonCloudFormationClient cfClient, String cFStackName,
            List<AwsLoadBalancer> awsLoadBalancers) {
        ListStackResourcesResult result = cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));

        boolean resourcesFound = true;
        List<StackResourceSummary> summaries = result.getStackResourceSummaries();
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Checking to see if listener resource {} already exists", listener.getName());
                resourcesFound = resourcesFound && summaries.stream()
                    .anyMatch(stackResourceSummary -> listener.getName().equals(stackResourceSummary.getLogicalResourceId()));
            }
        }

        return resourcesFound;
    }
}
