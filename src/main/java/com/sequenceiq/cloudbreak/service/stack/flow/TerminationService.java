package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.TerminationContext;
import com.sequenceiq.cloudbreak.domain.BillingStatus;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

import reactor.core.Reactor;

@Service
public class TerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TerminationService.class);
    private static final String DELETE_COMPLETED_MSG = "Cluster and its infrastructure were successfully terminated.";
    private static final String BILLING_STOPPED_MSG = "Billing stopped because of the termination of the cluster and its infrastructure.";
    private static final String DELIMITER = "_";

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private Reactor reactor;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @Autowired
    private ProvisionUtil provisionUtil;

    @Autowired
    private HostGroupRepository hostGroupRepository;

    @Autowired
    private CloudbreakEventService cloudbreakEventService;

    public FlowContext handleTerminationFailure(FlowContext context) {
        TerminationContext terminationContext = (TerminationContext) context;
        LOGGER.info("Stack delete failed on stack {} and set its status to {}.", terminationContext.getStackId(), Status.DELETE_FAILED);
        retryingStackUpdater.updateStackStatus(terminationContext.getStackId(), Status.DELETE_FAILED, terminationContext.getStatusReason());
        return terminationContext;
    }

    public FlowContext terminateStack(FlowContext context) {
        TerminationContext terminationContext = (TerminationContext) context;
        Long stackId = terminationContext.getStackId();
        CloudPlatform cloudPlatform = terminationContext.getCloudPlatform();
        retryingStackUpdater.updateStackStatus(stackId, Status.DELETE_IN_PROGRESS, "Termination of cluster infrastructure has started.");
        final Stack stack = stackRepository.findOneWithLists(stackId);
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_REQUEST_EVENT);
        try {
            if (!cloudPlatform.isWithTemplate()) {
                deleteStackResourcesWithoutTemplate(cloudPlatform, stack);
            } else {
                cloudPlatformConnectors.get(cloudPlatform).deleteStack(stack, stack.getCredential());
            }

            finalizeTermination(stack);
        } catch (Exception ex) {
            LOGGER.error(String.format("Stack delete failed on '%s' stack: ", stack.getId()), ex);
            String statusReason = "Termination of cluster infrastructure failed: " + ex.getMessage();
            throw new TerminationFailedException(statusReason, ex);
        }
        return context;
    }

    private void deleteStackResourcesWithoutTemplate(final CloudPlatform cloudPlatform, final Stack stack) throws Exception {
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
        final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
        List<Future<ResourceRequestResult>> futures = new ArrayList<>();
        for (int i = instanceResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
            final int index = i;
            List<Resource> resourceByType = stack.getResourcesByType(instanceResourceBuilders.get(cloudPlatform).get(i).resourceType());
            for (final Resource resource : resourceByType) {
                Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(new Callable<ResourceRequestResult>() {
                    @Override
                    public ResourceRequestResult call() throws Exception {
                        try {
                            instanceResourceBuilders.get(cloudPlatform).get(index).delete(resource, dCO, stack.getRegion());
                            retryingStackUpdater.removeStackResources(stack.getId(), Arrays.asList(resource));
                            return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                                    .withFutureResult(FutureResult.SUCCESS)
                                    .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                                    .build();
                        } catch (Exception ex) {
                            return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                                    .withException(ex)
                                    .withFutureResult(FutureResult.FAILED)
                                    .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                                    .build();
                        }
                    }
                });
                futures.add(submit);
                if (provisionUtil.isRequestFull(stack, futures.size() + 1)) {
                    Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
                    checkErrorOccurred(result);
                    futures = new ArrayList<>();
                }
            }
        }
        Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(stack.getId(), futures);
        checkErrorOccurred(result);
        for (int i = networkResourceBuilders.get(cloudPlatform).size() - 1; i >= 0; i--) {
            for (Resource resource : stack.getResourcesByType(networkResourceBuilders.get(cloudPlatform).get(i).resourceType())) {
                networkResourceBuilders.get(cloudPlatform).get(i).delete(resource, dCO, stack.getRegion());
            }
        }
    }

    private void checkErrorOccurred(Map<FutureResult, List<ResourceRequestResult>> futureResultListMap) throws Exception {
        List<ResourceRequestResult> resourceRequestResults = futureResultListMap.get(FutureResult.FAILED);
        if (!resourceRequestResults.isEmpty()) {
            throw resourceRequestResults.get(0).getException().orNull();
        }
    }

    private void finalizeTermination(Stack stack) {
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), Status.DELETE_COMPLETED.name(), DELETE_COMPLETED_MSG);
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BillingStatus.BILLING_STOPPED.name(), BILLING_STOPPED_MSG);
        updateStackFields(stack);
        retryingStackUpdater.updateStack(stack);
    }

    private void updateStackFields(Stack stack) {
        Date now = new Date();
        String terminatedName = stack.getName() + DELIMITER + now.getTime();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            cluster.setName(terminatedName);
            cluster.setBlueprint(null);
            for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(cluster.getId())) {
                hostGroup.getRecipes().clear();
                hostGroupRepository.save(hostGroup);
            }
        }
        stack.setCredential(null);
        stack.setName(terminatedName);
        stack.setStatus(Status.DELETE_COMPLETED);
        stack.setStatusReason(DELETE_COMPLETED_MSG);
        terminateMetaDataInstances(stack);
    }

    private void terminateMetaDataInstances(Stack stack) {
        for (InstanceMetaData metaData : stack.getRunningInstanceMetaData()) {
            long timeInMillis = Calendar.getInstance().getTimeInMillis();
            metaData.setTerminationDate(timeInMillis);
            metaData.setInstanceStatus(InstanceStatus.TERMINATED);
        }
    }
}
