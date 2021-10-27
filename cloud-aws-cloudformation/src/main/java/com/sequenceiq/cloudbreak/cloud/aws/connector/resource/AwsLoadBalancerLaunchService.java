package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.run;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.amazonaws.waiters.Waiter;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsLoadBalancerCommonService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsModelService;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsListener;
import com.sequenceiq.cloudbreak.cloud.aws.common.loadbalancer.AwsLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class AwsLoadBalancerLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLoadBalancerLaunchService.class);

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsCloudFormationClient awsClient;

    @Inject
    private AwsModelService awsModelService;

    @Inject
    private AwsStackRequestHelper awsStackRequestHelper;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    @Inject
    private AwsLoadBalancerCommonService loadBalancerCommonService;

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
            LOGGER.debug("Creating load balancers of types " + cloudLoadBalancers.stream().map(CloudLoadBalancer::getType)
                    .map(LoadBalancerType::name).collect(Collectors.joining(",")));

            AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
            String regionName = ac.getCloudContext().getLocation().getRegion().value();
            AmazonCloudFormationClient cfRetryClient = awsClient.createCloudFormationClient(credentialView, regionName);
            AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(credentialView, regionName);
            List<CloudResource> instances = cfStackUtil.getInstanceCloudResources(ac, cfRetryClient, amazonASClient, stack.getGroups());
            Network network = stack.getNetwork();
            AwsNetworkView awsNetworkView = new AwsNetworkView(network);

            if (modelContext == null) {
                LOGGER.debug("Creating default model context");
                modelContext = awsModelService.buildDefaultModelContext(ac, stack, resourceNotifier);
            }

            Map<String, List<String>> instanceIdsByGroupName = instances.stream()
                    .collect(Collectors.groupingBy(CloudResource::getGroup, mapping(CloudResource::getInstanceId, toList())));
            List<AwsLoadBalancer> awsLoadBalancers = loadBalancerCommonService.getAwsLoadBalancers(cloudLoadBalancers, instanceIdsByGroupName, awsNetworkView);

            modelContext.withLoadBalancers(awsLoadBalancers);
            LOGGER.debug("Starting CloudFormation update to create load balancer and target groups.");

            ListStackResourcesResult result;
            if (checkForLoadBalancerAndTargetGroupResources(cfRetryClient, cFStackName, awsLoadBalancers)) {
                LOGGER.debug("Load balancer and target group resources already exist, skipping creation");
                result = cfRetryClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
            } else {
                result = updateCloudFormationStack(ac, stack, modelContext);
            }

            setLoadBalancerMetadata(awsLoadBalancers, result);

            LOGGER.debug("Starting CloudFormation update to create listeners.");
            if (checkForListenerResources(cfRetryClient, cFStackName, awsLoadBalancers)) {
                LOGGER.debug("Listener resources already exist, skipping creation");
                result = cfRetryClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
            } else {
                result = updateCloudFormationStack(ac, stack, modelContext);
            }

            ListStackResourcesResult finalResult = result;
            awsLoadBalancers.forEach(lb -> statuses.add(createLoadBalancerStatus(ac, lb, finalResult)));
        } else {
            LOGGER.debug("No load balancers in stack");
        }

        return statuses;
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
                () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.UPDATE_FAILED, ResourceStatus.CREATE_FAILED));

        return cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
    }

    private CloudResourceStatus createLoadBalancerStatus(AuthenticatedContext ac, AwsLoadBalancer loadBalancer, ListStackResourcesResult result) {
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
            .status(createSuccess ? CommonStatus.CREATED : CommonStatus.FAILED)
            .type(ELASTIC_LOAD_BALANCER)
            .availabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
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
