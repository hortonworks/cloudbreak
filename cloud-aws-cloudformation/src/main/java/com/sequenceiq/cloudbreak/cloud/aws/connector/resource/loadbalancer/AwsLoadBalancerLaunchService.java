package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.loadbalancer;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration.cancellableWaiterConfiguration;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.handleWaiterError;
import static com.sequenceiq.common.api.type.ResourceType.ELASTIC_LOAD_BALANCER;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
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
import com.sequenceiq.common.api.type.LoadBalancerTypeAttribute;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.ValidateTemplateResponse;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;

@Service
public class AwsLoadBalancerLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLoadBalancerLaunchService.class);

    private static final String VALIDATION_ERROR = "ValidationError";

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
            AmazonElasticLoadBalancingClient loadBalancingClient = awsClient.createElasticLoadBalancingClient(credentialView, regionName);
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

            ListStackResourcesResponse result;
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

            ListStackResourcesResponse finalResult = result;
            awsLoadBalancers.forEach(lb -> statuses.add(createLoadBalancerStatus(ac, lb, finalResult)));
            for (AwsLoadBalancer loadBalancer : modelContext.getLoadBalancers()) {
                loadBalancerCommonService.modifyLoadBalancerAttributes(loadBalancingClient, loadBalancer.getArn());
                loadBalancerCommonService.modifyTargetGroupAttributes(
                        loadBalancingClient,
                        loadBalancer.getArn(),
                        loadBalancer.isUseStickySessionForTargetGroup());
            }
        } else {
            LOGGER.debug("No load balancers in stack");
        }

        return statuses;
    }

    @VisibleForTesting
    void setLoadBalancerMetadata(List<AwsLoadBalancer> awsLoadBalancers, ListStackResourcesResponse result) {
        List<StackResourceSummary> summaries = result.stackResourceSummaries();
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            LOGGER.debug("Processing load balancer {}", loadBalancer.getName());
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Processing listener {} and target group {}", listener.getName(), listener.getTargetGroup().getName());
                Optional<StackResourceSummary> targetGroupSummary = summaries.stream()
                        .filter(stackResourceSummary -> listener.getTargetGroup().getName().equals(stackResourceSummary.logicalResourceId()))
                        .findFirst();
                if (targetGroupSummary.isEmpty()) {
                    throw new CloudConnectorException(String.format("Could not create load balancer listeners: target group %s not found.",
                            listener.getTargetGroup().getName()));
                }
                if (StringUtils.isEmpty(targetGroupSummary.get().physicalResourceId())) {
                    throw new CloudConnectorException(String.format("Could not create load balancer listeners: target group %s arn not found.",
                            listener.getTargetGroup().getName()));
                }
                listener.getTargetGroup().setArn(targetGroupSummary.get().physicalResourceId());
                LOGGER.debug("Found arn {} for target group {}", listener.getTargetGroup().getArn(), listener.getTargetGroup().getName());
            }
            Optional<StackResourceSummary> loadBalancerSummary = summaries.stream()
                    .filter(stackResourceSummary -> loadBalancer.getName().equals(stackResourceSummary.logicalResourceId()))
                    .findFirst();
            if (loadBalancerSummary.isEmpty()) {
                throw new CloudConnectorException(String.format("Could not create load balancer listeners: load balancer %s not found.",
                        loadBalancer.getName()));
            }
            if (StringUtils.isEmpty(loadBalancerSummary.get().physicalResourceId())) {
                throw new CloudConnectorException(String.format("Could not create load balancer listeners: load balancer %s arn not found.",
                        loadBalancer.getName()));
            }
            loadBalancer.setArn(loadBalancerSummary.get().physicalResourceId());
            loadBalancer.validateListenerConfigIsSet();
            LOGGER.debug("Found arn {} for load balancer {}", loadBalancer.getArn(), loadBalancer.getName());
        }
    }

    private ListStackResourcesResponse updateCloudFormationStack(AuthenticatedContext ac, CloudStack stack, ModelContext modelContext) {
        String cFStackName = cfStackUtil.getCfStackName(ac);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(cFStackName).build();

        String cfTemplate = cloudFormationTemplateBuilder.build(modelContext);
        LOGGER.debug("CloudFormationTemplate: {}", cfTemplate);
        try {
            cfClient.updateStack(awsStackRequestHelper.createUpdateStackRequest(ac, stack, cFStackName, cfTemplate));
        } catch (AwsServiceException e) {
            LOGGER.info("CloudFormation template update failed. ", e);
            logTemplateValidationResultIfPossible(cfClient, cfTemplate, e);
            throw e;
        }
        StackCancellationCheck cancellationCheck = new StackCancellationCheck(ac.getCloudContext().getId());
        try (CloudFormationWaiter waiter = cfClient.waiters()) {
            LOGGER.debug("Waiting for CloudFormation stack {} update", cFStackName);
            waiter.waitUntilStackUpdateComplete(describeStacksRequest, cancellableWaiterConfiguration(cancellationCheck));
        } catch (Exception e) {
            handleWaiterError(String.format("CloudFormation stack %s update failed.", cFStackName),
                    () -> awsCloudFormationErrorMessageProvider.getErrorReason(ac, cFStackName, ResourceStatus.UPDATE_FAILED, ResourceStatus.CREATE_FAILED), e);
        }
        return cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));
    }

    private void logTemplateValidationResultIfPossible(AmazonCloudFormationClient cfClient, String cfTemplate, AwsServiceException exception) {
        try {
            if (VALIDATION_ERROR.equalsIgnoreCase(exception.awsErrorDetails().errorCode())) {
                ValidateTemplateResponse result = cfClient.validateTemplate(awsStackRequestHelper.createValidateTemplateRequest(cfTemplate));
                LOGGER.debug("Validation result for the CloudFormationTemplate: {}", result);
            }
        } catch (AwsServiceException | SdkClientException e) {
            LOGGER.info("Validating CloudFormationTemplate with the provider was not successful. ", e);
        }
    }

    private CloudResourceStatus createLoadBalancerStatus(AuthenticatedContext ac, AwsLoadBalancer loadBalancer, ListStackResourcesResponse result) {
        LOGGER.debug(String.format("Checking final status of AWS load balancer %s", loadBalancer.getName()));
        List<StackResourceSummary> summaries = result.stackResourceSummaries();
        boolean createSuccess = isResourceStatusGood(summaries, loadBalancer.getName());

        for (AwsListener listener : loadBalancer.getListeners()) {
            LOGGER.debug(String.format("Checking final status of AWS listener %s", listener.getName()));
            createSuccess = createSuccess && isResourceStatusGood(summaries, listener.getName());
            LOGGER.debug(String.format("Checking final status of AWS target group %s", listener.getTargetGroup().getName()));
            createSuccess = createSuccess && isResourceStatusGood(summaries, listener.getTargetGroup().getName());
        }
        Map<String, Object> params = Map.of(CloudResource.ATTRIBUTES,
                Enum.valueOf(LoadBalancerTypeAttribute.class, loadBalancer.getScheme().getLoadBalancerType().name()));
        CloudResource.Builder cloudResource = CloudResource.builder()
                .withStatus(createSuccess ? CommonStatus.CREATED : CommonStatus.FAILED)
                .withType(ELASTIC_LOAD_BALANCER)
                .withAvailabilityZone(ac.getCloudContext().getLocation().getAvailabilityZone().value())
                .withParameters(params)
                .withName(loadBalancer.getName());

        return new CloudResourceStatus(cloudResource.build(),
                createSuccess ? com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.CREATED :
                        com.sequenceiq.cloudbreak.cloud.model.ResourceStatus.FAILED);
    }

    private boolean isResourceStatusGood(List<StackResourceSummary> summaries, String name) {
        Optional<StackResourceSummary> summary = summaries.stream()
                .filter(stackResourceSummary -> name.equals(stackResourceSummary.logicalResourceId()))
                .findFirst();

        boolean success = true;
        if (summary.isEmpty()) {
            LOGGER.error(String.format("Could not fetch summary for AWS resource with name %s", name));
            success = false;
        } else if (isFailedStatus(summary.get().resourceStatus())) {
            LOGGER.error(String.format("Resource %s creation failed. Reason: %s",
                    name, summary.get().resourceStatusReason()));
            success = false;
        }
        return success;
    }

    private boolean isFailedStatus(ResourceStatus status) {
        return ResourceStatus.CREATE_FAILED == status || ResourceStatus.UPDATE_FAILED == status;
    }

    @VisibleForTesting
    boolean checkForLoadBalancerAndTargetGroupResources(AmazonCloudFormationClient cfClient, String cFStackName,
        List<AwsLoadBalancer> awsLoadBalancers) {
        ListStackResourcesResponse result = cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));

        boolean resourcesFound = true;
        List<StackResourceSummary> summaries = result.stackResourceSummaries();
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            LOGGER.debug("Checking to see if load balancer resource {} already exists", loadBalancer.getName());
            resourcesFound = resourcesFound && summaries.stream()
                    .anyMatch(stackResourceSummary -> loadBalancer.getName().equals(stackResourceSummary.logicalResourceId()));
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Checking to see if target group resource {} already exists", listener.getTargetGroup().getName());
                resourcesFound = resourcesFound && summaries.stream()
                        .anyMatch(stackResourceSummary -> listener.getTargetGroup().getName().equals(stackResourceSummary.logicalResourceId()));
            }
        }

        return resourcesFound;
    }

    @VisibleForTesting
    boolean checkForListenerResources(AmazonCloudFormationClient cfClient, String cFStackName,
            List<AwsLoadBalancer> awsLoadBalancers) {
        ListStackResourcesResponse result = cfClient.listStackResources(awsStackRequestHelper.createListStackResourcesRequest(cFStackName));

        boolean resourcesFound = true;
        List<StackResourceSummary> summaries = result.stackResourceSummaries();
        for (AwsLoadBalancer loadBalancer : awsLoadBalancers) {
            for (AwsListener listener : loadBalancer.getListeners()) {
                LOGGER.debug("Checking to see if listener resource {} already exists", listener.getName());
                resourcesFound = resourcesFound && summaries.stream()
                        .anyMatch(stackResourceSummary -> listener.getName().equals(stackResourceSummary.logicalResourceId()));
            }
        }

        return resourcesFound;
    }
}
