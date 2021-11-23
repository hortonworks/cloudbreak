package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartupscale;


import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartupscale.StopStartUpscaleEvent.STOPSTART_UPSCALE_FINALIZED_EVENT;
import static java.util.stream.Collectors.toList;

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
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartUpscaleCommissionViaCMResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartUpscaleStartInstancesResult;
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
    InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

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
                LOGGER.info("ZZZ: AllInstances: count={}, instances={}", instanceMetaDataList.size(), instanceMetaDataList);
                List<InstanceMetaData> instanceMetaDataForHg = instanceMetaDataList.stream().filter(x -> x.getInstanceGroupName().equals(context.getHostGroupName())).collect(Collectors.toList());
                LOGGER.info("ZZZ: The following instnaces were found in the required hostGroup: {}", instanceMetaDataForHg);
                // TODO CB-14929: Handle scenarios where CB / cloud-provider state is potentially different. Also for error handling, consider nodes which are not in the correct state on the CM side (e.g. decommissioned despite RUNNING)
                List<InstanceMetaData> stoppedInstancesInHg = instanceMetaDataForHg.stream()
                        .filter(s -> s.getInstanceStatus() == com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED)
                        .collect(Collectors.toList());

                List<CloudInstance> stoppedCloudInstancesForHg = instanceMetaDataToCloudInstanceConverter.convert(stoppedInstancesInHg, stack.getEnvironmentCrn(), stack.getStackAuthentication());

                return new StopStartUpscaleStartInstancesRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(), stoppedCloudInstancesForHg, context.getAdjustment());
            }
        };
    }

    @Bean(name = "STOPSTART_UPSCALE_HOSTS_COMMISSION_STATE")
    public Action<?, ?> cmCommissionAction() {
        return new AbstractStopStartUpscaleActions<>(StopStartUpscaleStartInstancesResult.class) {

            @Override
            protected void doExecute(StopStartUpscaleContext context, StopStartUpscaleStartInstancesResult payload, Map<Object, Object> variables) throws Exception {


                List<CloudVmInstanceStatus> cloudVmInstanceStatusList = payload.getStartedInstances();
                Set<String> cloudInstanceIds = cloudVmInstanceStatusList.stream().map(x -> x.getCloudInstance().getInstanceId()).collect(Collectors.toUnmodifiableSet());

                // TODO CB-14929: Move this into a utility function. | Translation because 'stack' handlers, when living in 'cloud-reactor' don't have access to InstanceMetadata.
                //  Also referenced in StopStartDownscaleActions
                List<InstanceMetaData> instanceMetaDatas = context.getStack().getInstanceGroups()
                        .stream().filter(ig -> ig.getGroupName().equals(context.getHostGroupName()))
                        .flatMap(instanceGroup -> instanceGroup.getInstanceMetaDataSet().stream())
                        .filter(im -> cloudInstanceIds.contains(im.getInstanceId()))
                        .collect(toList());

                clusterUpscaleFlowService.instancesStarted(context, context.getStack().getId(), instanceMetaDatas);

                List<String> instanceIds = payload.getStartedInstances().stream().map(x -> x.getCloudInstance().getInstanceId()).collect(Collectors.toList());
                clusterUpscaleFlowService.upscaleCommissionNewNodes(context.getStack().getId(), context.getHostGroupName(), instanceIds);

                // TODO CB-14929: Handle database updates for state.
//            Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(context.getStack(), instanceCountToCreate,
//                    context.getInstanceGroupName(), true, context.getHostNames(), context.isRepair());

                StopStartUpscaleCommissionViaCMRequest commissionRequest = new StopStartUpscaleCommissionViaCMRequest(context.getStack(), context.getHostGroupName(), instanceMetaDatas);
                sendEvent(context, commissionRequest);

            }
        };
    }


    @Bean(name = "STOPSTART_UPSCALE_FINALIZE_STATE")
    public Action<?, ?> upscaleFinishedAction() {
        return new AbstractStopStartUpscaleActions<>(StopStartUpscaleCommissionViaCMResult.class) {

            @Override
            protected void doExecute(StopStartUpscaleContext context, StopStartUpscaleCommissionViaCMResult payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("ZZZ: Marking upscale as finished.");
                // TODO CB-14929: Does this need to force a CM health-sync, to make sure states show up properly on the UI. May need to force a CM sync
                clusterUpscaleFlowService.clusterUpscaleFinished(context.getStackView(), context.getHostGroupName(), payload.getRequest().getInstancesToCommission());
                getMetricService().incrementMetricCounter(MetricType.CLUSTER_UPSCALE_SUCCESSFUL, context.getStack());
                sendEvent(context, STOPSTART_UPSCALE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Selectable createRequest(StopStartUpscaleContext context) {
                return null;
            }
        };
    }


    @Bean(name = "STOPSTART_UPSCALE_FAILED_STATE")
    public Action<?, ?> clusterUpscaleFailedAction() {
        return new AbstractStackFailureAction<StopStartUpscaleState, StopStartUpscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                // TODO CB-14929: Implement actual error handling. i.e. reverting whatever is possible. Updating DB state etc.
                clusterUpscaleFlowService.clusterUpscaleFailed(context.getStackView().getId(), payload.getException());
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

        // TODO CB-14929: Clean-up unused fields like Repair. Also in the events used to populate this information.
        static final String REPAIR = "REPAIR";

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
            MDCBuilder.buildMdcContext(stack.getCluster()); // TODO CB-14929: Is this OK to do? In a stack operation.
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

            return new StopStartUpscaleContext(flowParameters, stack, stackService.getViewByIdWithoutAuth(stack.getId()), cloudContext, cloudCredential, cloudStack,
                    getHostgroupName(variables), getAdjustment(variables),
                    isSinglePrimaryGateway(variables), getClusterManagerType(variables), isRestartServices(variables));
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
