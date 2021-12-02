package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent.STOPSTART_UPSCALE_FINALIZED_EVENT;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.LinkedList;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.AbstractStackAction;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StopStartDownscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleActions.class);

    @Inject
    private StopStartDownscaleFlowService clusterDownscaleFlowService;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    // TODO CB-14929: Figure out the transitions that need to be made / DB persistence of various states after each action.

    // TODO CB-14929: Potential pre-flight checks. YARN / appropriate service masters available. CM master available.

    // TODO CB-14929: Any way to have this not be a string literal, and instead be a direct reference to the defined state?
    @Bean(name = "STOPSTART_DOWNSCALE_HOSTS_DECOMMISSION_STATE")
    public Action<?, ?> decommissionViaCmAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleTriggerEvent.class) {

            @Override
            protected void prepareExecution(StopStartDownscaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTGROUPNAME, payload.getHostGroupName());
                variables.put(ADJUSTMENT, payload.getAdjustment());
                variables.put(SINGLE_PRIMARY_GATEWAY, payload.isSinglePrimaryGateway());
                variables.put(CLUSTER_MANAGER_TYPE, payload.getClusterManagerType());
                variables.put(RESTART_SERVICES, payload.isRestartServices());
                variables.put(HOSTS_TO_REMOVE, payload.getHostIds());
            }

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                clusterDownscaleFlowService.clusterDownscaleStarted(context.getStack().getId(), payload.getHostGroupName(),
                        payload.getAdjustment(), payload.getHostIds());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StopStartDownscaleContext context) {
                return new StopStartDownscaleDecommissionViaCMRequest(context.getStack(), context.getHostGroupName(), context.getHostIdsToRemove());
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_STOP_INSTANCE_STATE")
    public Action<?, ?> stopInstancesAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleDecommissionViaCMResult.class) {

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleDecommissionViaCMResult payload,
                    Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                // TODO CB-14929: The list of instances here needs to come from the previous step, depending on which instances were successfully
                //  decommissioned, or already decommissioned instances.
                clusterDownscaleFlowService.clusterDownscalingStoppingInstances(stack.getId(), context.getHostGroupName(), context.getHostIdsToRemove());

                Set<Long> instancesToRemove = stackService.getPrivateIdsForHostNames(stack.getInstanceMetaDataAsList(), payload.getDecommissionedHostFqdns());

                List<InstanceMetaData> instanceMetaDataList = stack.getNotDeletedInstanceMetaDataList();
                LOGGER.info("ZZZ: AllInstances: count={}, instances={}", instanceMetaDataList.size(), instanceMetaDataList);
                List<InstanceMetaData> instanceMetaDataForHg = instanceMetaDataList.stream().filter(
                        x -> x.getInstanceGroupName().equals(context.getHostGroupName())).collect(Collectors.toList());
                LOGGER.info("ZZZ: The following instnaces were found in the required hostGroup: {}", instanceMetaDataForHg);


                List<InstanceMetaData> toStopInstanceMetadataList = new LinkedList<>();
                for (InstanceMetaData instanceMetaData : instanceMetaDataForHg) {
                    if (instancesToRemove.contains(instanceMetaData.getPrivateId())) {
                        toStopInstanceMetadataList.add(instanceMetaData);
                    }
                }
                LOGGER.info("ZZZ: toStopInstanceMetadata: count={}, md={}", toStopInstanceMetadataList.size(), toStopInstanceMetadataList);

                List<CloudInstance> cloudInstancesToStop = instanceMetaDataToCloudInstanceConverter.convert(toStopInstanceMetadataList,
                        context.getStack().getEnvironmentCrn(), context.getStack().getStackAuthentication());

                StopStartDownscaleStopInstancesRequest request = new StopStartDownscaleStopInstancesRequest(context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack(), cloudInstancesToStop);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_FINALIZE_STATE")
    public Action<?, ?> downscaleFinishedAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleStopInstancesResult.class) {

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleStopInstancesResult payload,
                    Map<Object, Object> variables) throws Exception {
                LOGGER.info("ZZZ: Marking stopstart Downscale as finished");

                // TODO CB-14929: See notes in the actual downscale handler about handling failures, instance state in CB vs cloud-provider etc.
                List<CloudVmInstanceStatus> cloudVmInstanceStatusList = payload.getCloudVmInstanceStatusesNoCheck();
                Set<String> cloudInstanceIds = cloudVmInstanceStatusList.stream().map(
                        x -> x.getCloudInstance().getInstanceId()).collect(Collectors.toUnmodifiableSet());

                // TODO CB-14929: Move this into a utility function. | Translation because 'stack' handlers, when living in
                //  'cloud-reactor' don't have access to InstanceMetadata.
                List<InstanceMetaData> instanceMetaDatas = context.getStack().getInstanceGroups()
                        .stream().filter(ig -> ig.getGroupName().equals(context.getHostGroupName()))
                        .flatMap(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream())
                        .filter(im -> im.getInstanceId() == null ? false : cloudInstanceIds.contains(im.getInstanceId()))
                        .collect(toList());

                clusterDownscaleFlowService.clusterDownscaleFinished(context.getStack().getId(), context.getHostGroupName(), new HashSet<>(instanceMetaDatas));
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL, context.getStack());
                sendEvent(context, STOPSTART_UPSCALE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Selectable createRequest(StopStartDownscaleContext context) {
                return null;
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_FAILED_STATE")
    public Action<?, ?> clusterDownscaleFailedAction() {
        return new AbstractStackFailureAction<StopStartDownscaleState, StopStartDownscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterDownscaleFlowService.handleClusterDownscaleFailure(context.getStackView().getId(), payload.getException());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_FAILED, context.getStackView(), payload.getException());
                // TODO CB-14929: Implement properly - update states in the CB DB, revert changes on CM / cloud-provider as appropriate.
                ClusterUpscaleFailedConclusionRequest request = new ClusterUpscaleFailedConclusionRequest(context.getStackView().getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    private abstract static class AbstractStopStartDownscaleActions<P extends Payload>
            extends AbstractStackAction<StopStartDownscaleState, StopStartDownscaleEvent, StopStartDownscaleContext, P> {

        static final String HOSTGROUPNAME = "HOSTGROUPNAME";

        static final String HOSTS_TO_REMOVE = "HOSTS_TO_REMOVE";

        static final String ADJUSTMENT = "ADJUSTMENT";

        static final String SINGLE_PRIMARY_GATEWAY = "SINGLE_PRIMARY_GATEWAY";

        static final String INSTALLED_COMPONENTS = "INSTALLED_COMPONENTS";

        static final String CLUSTER_MANAGER_TYPE = "CLUSTER_MANAGER_TYPE";

        static final String RESTART_SERVICES = "RESTART_SERVICES";

        static final String REPAIR = "REPAIR";

        @Inject
        private StackService stackService;

        @Inject
        private ResourceService resourceService;

        @Inject
        private StackUtil stackUtil;

        @Inject
        private StackToCloudStackConverter cloudStackConverter;

        AbstractStopStartDownscaleActions(Class<P> payloadClass) {
            super(payloadClass);
        }

        @Override
        protected Object getFailurePayload(P payload, Optional<StopStartDownscaleContext> flowContext, Exception ex) {
            return new StackFailureEvent(payload.getResourceId(), ex);
        }

        @Override
        protected StopStartDownscaleContext createFlowContext(FlowParameters flowParameters, StateContext<StopStartDownscaleState,
                StopStartDownscaleEvent> stateContext, P payload) {

            Map<Object, Object> variables = stateContext.getExtendedState().getVariables();
            Stack stack = stackService.getByIdWithListsInTransaction(payload.getResourceId());
            stack.setResources(new HashSet<>(resourceService.getAllByStackId(payload.getResourceId())));

            // TODO CB-14929:  Is this OK to do? In a stack operation.
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

            return new StopStartDownscaleContext(flowParameters, stack, stackService.getViewByIdWithoutAuth(stack.getId()),
                    cloudContext, cloudCredential, cloudStack,
                    getHostgroupName(variables), getHostsToRemove(variables), getAdjustment(variables),
                    isSinglePrimaryGateway(variables), getClusterManagerType(variables), isRestartServices(variables));
        }

        private String getHostgroupName(Map<Object, Object> variables) {
            return (String) variables.get(HOSTGROUPNAME);
        }

        Set<Long> getHostsToRemove(Map<Object, Object> variables) {
            return (Set<Long>) variables.get(HOSTS_TO_REMOVE);
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

        Boolean isRepair(Map<Object, Object> variables) {
            return (Boolean) variables.get(REPAIR);
        }

        Boolean isRestartServices(Map<Object, Object> variables) {
            return (Boolean) variables.get(RESTART_SERVICES);
        }

        ClusterManagerType getClusterManagerType(Map<Object, Object> variables) {
            return (ClusterManagerType) variables.get(CLUSTER_MANAGER_TYPE);
        }
    }
}