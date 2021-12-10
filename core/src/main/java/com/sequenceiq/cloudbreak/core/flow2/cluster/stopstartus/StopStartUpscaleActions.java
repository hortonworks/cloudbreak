package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus;


import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_FINALIZED_EVENT;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.converter.CloudInstanceIdToInstanceMetaDataConverter;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartUpscaleCommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StopStartUpscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleActions.class);

    @Inject
    private StopStartUpscaleFlowService clusterUpscaleFlowService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private CloudInstanceIdToInstanceMetaDataConverter cloudInstanceIdToInstanceMetaDataConverter;

    @Bean(name = "STOPSTART_UPSCALE_START_INSTANCE_STATE")
    public Action<?, ?> startInstancesAction() {
        return new AbstractStopStartUpscaleActions<>(StopStartUpscaleTriggerEvent.class) {

            @Override
            protected void prepareExecution(StopStartUpscaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTGROUPNAME, payload.getHostGroupName());
                variables.put(ADJUSTMENT, payload.getAdjustment());
                variables.put(SINGLE_PRIMARY_GATEWAY, payload.isSinglePrimaryGateway());
                variables.put(CLUSTER_MANAGER_TYPE, payload.getClusterManagerType());
                variables.put(RESTART_SERVICES, payload.isRestartServices());
            }

            @Override
            protected void doExecute(StopStartUpscaleContext context, StopStartUpscaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                clusterUpscaleFlowService.startingInstances(context.getStack().getId(), payload.getHostGroupName(), payload.getAdjustment());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StopStartUpscaleContext context) {
                Stack stack = context.getStack();
                List<InstanceMetaData> instanceMetaDataList = stack.getNotDeletedInstanceMetaDataList();

                List<InstanceMetaData> instanceMetaDataForHg = instanceMetaDataList.stream().filter(
                        x -> x.getInstanceGroupName().equals(context.getHostGroupName())).collect(Collectors.toList());

                List<InstanceMetaData> stoppedInstancesInHg = instanceMetaDataForHg.stream()
                        .filter(s -> s.getInstanceStatus() == com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED)
                        .collect(Collectors.toList());

                LOGGER.info("NotDeletedInstanceMetadata totalCount={}. count for hostGroup: {}={}, stoppedInstancesInHgCount={}",
                        instanceMetaDataList.size(), context.getHostGroupName(), instanceMetaDataList.size(), stoppedInstancesInHg.size());

                List<CloudInstance> stoppedCloudInstancesForHg = instanceMetaDataToCloudInstanceConverter.convert(stoppedInstancesInHg,
                        stack.getEnvironmentCrn(), stack.getStackAuthentication());
                List<CloudInstance> allCloudInstancesForHg = instanceMetaDataToCloudInstanceConverter.convert(instanceMetaDataForHg,
                        stack.getEnvironmentCrn(), stack.getStackAuthentication());

                return new StopStartUpscaleStartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        context.getHostGroupName(), stoppedCloudInstancesForHg, allCloudInstancesForHg, Collections.emptyList(), context.getAdjustment());
            }
        };
    }

    @Bean(name = "STOPSTART_UPSCALE_HOSTS_COMMISSION_STATE")
    public Action<?, ?> cmCommissionAction() {
        return new AbstractStopStartUpscaleActions<>(StopStartUpscaleStartInstancesResult.class) {

            @Override
            protected void doExecute(StopStartUpscaleContext context, StopStartUpscaleStartInstancesResult payload,
                    Map<Object, Object> variables) throws Exception {

                // Update instance metadata for successful nodes before handling / logging info about failures.
                List<CloudVmInstanceStatus> cloudVmInstanceStatusList = payload.getAffectedInstanceStatuses();
                Set<String> cloudInstanceIdsStarted = cloudVmInstanceStatusList.stream()
                        .filter(x -> x.getStatus() == InstanceStatus.STARTED)
                        .map(x -> x.getCloudInstance().getInstanceId())
                        .collect(Collectors.toUnmodifiableSet());
                List<InstanceMetaData> startedInstancesMetaData = cloudInstanceIdToInstanceMetaDataConverter.getNotDeletedInstances(
                        context.getStack(), context.getHostGroupName(), cloudInstanceIdsStarted);
                clusterUpscaleFlowService.instancesStarted(context.getStack().getId(), startedInstancesMetaData);

                handleInstanceUnsuccessfulStart(context, cloudVmInstanceStatusList);

                // This list is currently empty. It could be populated later in another flow-step by querying CM to get service health.
                // Meant to be a mechanism which detects cloud instances which are RUNNING, but not being utilized (likely due to previous failures)
                List<CloudInstance> instancesWithServicesNotRunning = payload.getStartInstanceRequest().getStartedInstancesWithServicesNotRunning();
                List<InstanceMetaData> metaDataWithServicesNotRunning = cloudInstanceIdToInstanceMetaDataConverter.getNotDeletedInstances(
                        context.getStack(),
                        context.getHostGroupName(),
                        instancesWithServicesNotRunning.stream().map(i -> i.getInstanceId()).collect(Collectors.toUnmodifiableSet()));

                LOGGER.info("StartedInstancesCount={}, StartedInstancesMetadataCount={}," +
                        " instancesWithServicesNotRunningCount={}, instancesWithServicesNotRunningMetadataCount={}",
                        cloudInstanceIdsStarted.size(), startedInstancesMetaData.size(),
                        instancesWithServicesNotRunning.size(), metaDataWithServicesNotRunning.size());

                int toCommissionNodeCount = metaDataWithServicesNotRunning.size() + startedInstancesMetaData.size();
                if (toCommissionNodeCount < context.getAdjustment()) {
                    LOGGER.warn("Not enough nodes found to commission. DesiredCount={}, availableCount={}", context.getAdjustment(), toCommissionNodeCount);
                    clusterUpscaleFlowService.warnNotEnoughInstances(context.getStack().getId(), context.getHostGroupName(),
                            context.getAdjustment(), toCommissionNodeCount);
                }
                clusterUpscaleFlowService.upscaleCommissioningNodes(context.getStack().getId(), context.getHostGroupName(),
                        startedInstancesMetaData, metaDataWithServicesNotRunning);

                StopStartUpscaleCommissionViaCMRequest commissionRequest = new StopStartUpscaleCommissionViaCMRequest(context.getStack(),
                        context.getHostGroupName(), startedInstancesMetaData, metaDataWithServicesNotRunning);
                sendEvent(context, commissionRequest);

            }

            private void handleInstanceUnsuccessfulStart(StopStartUpscaleContext context, List<CloudVmInstanceStatus> cloudVmInstanceStatusList) {
                List<CloudVmInstanceStatus> instancesNotInDesiredState = cloudVmInstanceStatusList.stream()
                        .filter(i -> i.getStatus() != InstanceStatus.STARTED).collect(Collectors.toList());
                if (instancesNotInDesiredState.size() > 0) {
                    // Not updating the status of these instances in the DB. Instead letting the regular syncer threads take care of this.
                    // This is in case there is additional logic in the syncers while processing Instance state changes.

                    clusterUpscaleFlowService.logInstancesFailedToStart(context.getStack().getId(), instancesNotInDesiredState);
                    LOGGER.warn("Some instances could not be started: count={}, instances={}", instancesNotInDesiredState.size(), instancesNotInDesiredState);

                    // TODO CB-15132: Eventually, we may want to take some corrective action.
                }
            }
        };
    }

    @Bean(name = "STOPSTART_UPSCALE_FINALIZE_STATE")
    public Action<?, ?> upscaleFinishedAction() {
        return new AbstractStopStartUpscaleActions<>(StopStartUpscaleCommissionViaCMResult.class) {

            @Override
            protected void doExecute(StopStartUpscaleContext context, StopStartUpscaleCommissionViaCMResult payload,
                    Map<Object, Object> variables) throws Exception {
                LOGGER.debug("STOPSTART_UPSCALE_FINALIZE_STATE - finalizing upscale via start");
                // TODO CB-14929: Does this need to force a CM health-sync, to make sure states show up properly on the UI. May need to force a CM sync

                logInstancesNotCommissioned(context, payload.getNotRecommissionedFqdns());

                List<InstanceMetaData> instancesCommissioned = context.getStack().getNotDeletedInstanceMetaDataList().stream()
                        .filter(i -> payload.getSuccessfullyCommissionedFqdns().contains(i.getDiscoveryFQDN()))
                        .collect(Collectors.toList());

                clusterUpscaleFlowService.clusterUpscaleFinished(context.getStackView(), context.getHostGroupName(),
                        instancesCommissioned);

                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL, context.getStack());
                sendEvent(context, STOPSTART_UPSCALE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Selectable createRequest(StopStartUpscaleContext context) {
                return null;
            }

            private void logInstancesNotCommissioned(StopStartUpscaleContext context, List<String> notCommissionedFqdns) {
                // Not updating the status of these instances in the DB. Instead letting the regular syncer threads take care of this.
                // This is in case there is additional logic in the syncers while processing Instance state changes.
                if (notCommissionedFqdns.size() > 0) {
                    clusterUpscaleFlowService.logInstancesFailedToCommission(context.getStack().getId(), notCommissionedFqdns);
                }
                // TODO CB-15132: Eventually, we may want to take some corrective action.
            }
        };
    }

    @Bean(name = "STOPSTART_UPSCALE_FAILED_STATE")
    public Action<?, ?> clusterUpscaleFailedAction() {
        return new AbstractStackFailureAction<StopStartUpscaleState, StopStartUpscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                // TODO CB-15132: Implement actual error handling. i.e. reverting whatever is possible. Updating DB state etc.
                clusterUpscaleFlowService.clusterUpscaleFailed(context.getStackView().getId(),  payload.getException());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED, context.getStackView(), payload.getException());
                ClusterUpscaleFailedConclusionRequest request = new ClusterUpscaleFailedConclusionRequest(context.getStackView().getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    private abstract static class AbstractStopStartUpscaleActions<P extends Payload>
            extends AbstractStackAction<StopStartUpscaleState, StopStartUpscaleEvent, StopStartUpscaleContext, P> {
        static final String HOSTGROUPNAME = "HOSTGROUPNAME";

        static final String ADJUSTMENT = "ADJUSTMENT";

        static final String SINGLE_PRIMARY_GATEWAY = "SINGLE_PRIMARY_GATEWAY";

        static final String INSTALLED_COMPONENTS = "INSTALLED_COMPONENTS";

        static final String CLUSTER_MANAGER_TYPE = "CLUSTER_MANAGER_TYPE";

        static final String RESTART_SERVICES = "RESTART_SERVICES";

        @Inject
        private StackService stackService;

        @Inject
        private ResourceService resourceService;

        @Inject
        private StackUtil stackUtil;

        @Inject
        private StackToCloudStackConverter cloudStackConverter;

        AbstractStopStartUpscaleActions(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StopStartUpscaleContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }

        @Override
        protected StopStartUpscaleContext createFlowContext(FlowParameters flowParameters, StateContext<StopStartUpscaleState,
                StopStartUpscaleEvent> stateContext, P payload) {
            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
            stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));
            // TODO CB-14929: Is this OK to do? In a stack operation.
            MDCBuilder.buildMdcContext(stack.getCluster());
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));



            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformVariant())
                    .withLocation(location)
                    .withWorkspaceId(stack.getWorkspace().getId())
                    .withAccountId(stack.getTenant().getId())
                    .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack);
            CloudStack cloudStack = cloudStackConverter.convert(stack);

            return new StopStartUpscaleContext(flowParameters, stack, stackService.getViewByIdWithoutAuth(stack.getId()),
                    cloudContext, cloudCredential, cloudStack,
                    getHostgroupName(variables), getAdjustment(variables),
                    isSinglePrimaryGateway(variables), getClusterManagerType(variables));
        }

        private String getHostgroupName(Map<Object, Object> variables) {
            return (String) variables.get(HOSTGROUPNAME);
        }

        private Integer getAdjustment(Map<Object, Object> variables) {
            return (Integer) variables.get(ADJUSTMENT);
        }

        private Boolean isSinglePrimaryGateway(Map<Object, Object> variables) {
            return (Boolean) variables.get(SINGLE_PRIMARY_GATEWAY);
        }

        Map<String, String> getInstalledComponents(Map<Object, Object> variables) {
            return (Map<String, String>) variables.get(INSTALLED_COMPONENTS);
        }

        ClusterManagerType getClusterManagerType(Map<Object, Object> variables) {
            return (ClusterManagerType) variables.get(CLUSTER_MANAGER_TYPE);
        }
    }
}
