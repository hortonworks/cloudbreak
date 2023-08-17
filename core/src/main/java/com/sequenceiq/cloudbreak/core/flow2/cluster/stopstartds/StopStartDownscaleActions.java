package com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.ClusterStatus.Value.SCALE_DOWN;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FAILURE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FINALIZED_EVENT;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleGetRecoveryCandidatesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleGetRecoveryCandidatesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopStartDownscaleStopInstancesResult;
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
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.metering.MeteringService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.core.FlowParameters;

@Configuration
public class StopStartDownscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleActions.class);

    @Inject
    private StopStartDownscaleFlowService stopStartDownscaleFlowService;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Inject
    private CloudInstanceIdToInstanceMetaDataConverter cloudInstanceIdToInstanceMetaDataConverter;

    @Inject
    private MeteringService meteringService;

    @Bean(name = "STOPSTART_DOWNSCALE_GET_RECOVERY_CANDIDATES_STATE")
    public Action<?, ?> getRecoveryCandidatesAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleTriggerEvent.class) {

            @Override
            protected void prepareExecution(StopStartDownscaleTriggerEvent payload, Map<Object, Object> variables) {
                variables.put(HOSTGROUPNAME, payload.getHostGroup());
                variables.put(HOSTS_TO_REMOVE, payload.getHostIds());
            }

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                StackDtoDelegate stack = context.getStack();
                stopStartDownscaleFlowService.initScaleDown(stack.getId(), payload.getHostGroup());

                List<CloudInstance> allInstancesInHostGroup = Collections.emptyList();
                if (payload.isFailureRecoveryEnabled()) {
                    LOGGER.info("Identifying recovery candidates for downscale");
                    List<InstanceMetadataView> allInstanceMetadataInHostGroup =
                            instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId())
                                    .stream().filter(imv -> payload.getHostGroup().equals(imv.getInstanceGroupName())).collect(Collectors.toList());
                    allInstancesInHostGroup = instanceMetaDataToCloudInstanceConverter.convert(allInstanceMetadataInHostGroup,
                            stack.getStack());
                }
                StopStartDownscaleGetRecoveryCandidatesRequest request = new StopStartDownscaleGetRecoveryCandidatesRequest(context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack(), context.getHostGroupName(), allInstancesInHostGroup, payload.getHostIds(),
                        payload.isFailureRecoveryEnabled());
                sendEvent(context, request);
            }
        };
    }

    // TODO CB-15132: Potential pre-flight checks. YARN / appropriate service masters available. CM master available.
    @Bean(name = "STOPSTART_DOWNSCALE_HOSTS_DECOMMISSION_STATE")
    public Action<?, ?> decommissionViaCmAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleGetRecoveryCandidatesResult.class) {

            @Override
            protected void doExecute(
                    StopStartDownscaleContext context, StopStartDownscaleGetRecoveryCandidatesResult payload, Map<Object, Object> variables) throws Exception {
                stopStartDownscaleFlowService.clusterDownscaleStarted(context.getStack().getId(), payload.getHostGroupName(), payload.getHostIds(),
                        payload.getStartedInstancesWithServicesNotRunning().stream().map(CloudInstance::getInstanceId).collect(Collectors.toSet()));
                StopStartDownscaleDecommissionViaCMRequest request = new StopStartDownscaleDecommissionViaCMRequest(context.getStack().getId(),
                        context.getHostGroupName(), context.getHostIdsToRemove(), payload.getStartedInstancesWithServicesNotRunning());
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_STOP_INSTANCE_STATE")
    public Action<?, ?> stopInstancesAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleDecommissionViaCMResult.class) {

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleDecommissionViaCMResult payload,
                    Map<Object, Object> variables) throws Exception {
                StackDtoDelegate stack = context.getStack();

                if (payload.getNotDecommissionedHostFqdns().size() > 0) {
                    stopStartDownscaleFlowService.logCouldNotDecommission(stack.getId(), payload.getNotDecommissionedHostFqdns());
                }

                Set<String> decommissionedFqdns = payload.getDecommissionedHostFqdns();
                stopStartDownscaleFlowService.clusterDownscalingStoppingInstances(stack.getId(), context.getHostGroupName(), decommissionedFqdns);


                List<InstanceMetadataView> instanceMetaDataList = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId());
                Set<Long> instancesToStop = stackService.getPrivateIdsForHostNames(instanceMetaDataList, payload.getDecommissionedHostFqdns());

                List<InstanceMetadataView> instanceMetaDataForHg = instanceMetaDataList.stream().filter(
                        x -> x.getInstanceGroupName().equals(context.getHostGroupName())).collect(Collectors.toList());

                LOGGER.debug("InstanceInfoPreStop. hostGroup={}, allInstanceCount={}, hgInstanceCount={}. AllNotDeletedInstances=[{}]",
                        context.getHostGroupName(), instanceMetaDataList.size(), instanceMetaDataForHg.size(),
                        instanceMetaDataList.stream().map(InstanceMetadataView::getInstanceId).collect(Collectors.toList()));

                List<InstanceMetadataView> toStopInstanceMetadataList = new LinkedList<>();
                for (InstanceMetadataView instanceMetaData : instanceMetaDataForHg) {
                    if (instancesToStop.contains(instanceMetaData.getPrivateId())) {
                        toStopInstanceMetadataList.add(instanceMetaData);
                    }
                }
                LOGGER.debug("toStopInstanceMetadata: count={}, metadata=[{}]", toStopInstanceMetadataList.size(),
                        toStopInstanceMetadataList
                                .stream()
                                .map(InstanceMetadataView::getInstanceId)
                                .collect(Collectors.toSet()));

                List<CloudInstance> cloudInstancesToStop = instanceMetaDataToCloudInstanceConverter.convert(toStopInstanceMetadataList,
                        context.getStack().getStack());

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
                LOGGER.debug("STOPSTART_DOWNSCALE_FINALIZE_STATE - finalizing downscale via stop");

                // Update instance metadata for successful nodes before handling / logging info about failures.
                List<CloudVmInstanceStatus> cloudVmInstanceStatusList = payload.getAffectedInstanceStatuses();
                Set<String> cloudInstanceIdsStopped = cloudVmInstanceStatusList.stream()
                        .filter(x -> x.getStatus() == InstanceStatus.STOPPED)
                        .map(x -> x.getCloudInstance().getInstanceId())
                        .collect(Collectors.toUnmodifiableSet());
                StackDtoDelegate stack = context.getStack();
                List<InstanceMetadataView> instanceMetadataViews = instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(stack.getId());
                List<InstanceMetadataView> stoppedInstanceMetadata = cloudInstanceIdToInstanceMetaDataConverter.getNotDeletedAndNotZombieInstances(
                        instanceMetadataViews, context.getHostGroupName(), cloudInstanceIdsStopped);
                stopStartDownscaleFlowService.instancesStopped(stack.getId(), stoppedInstanceMetadata);

                handleNotStartedInstances(context, payload.getAffectedInstanceStatuses());

                stopStartDownscaleFlowService.clusterDownscaleFinished(stack.getId(), context.getHostGroupName(), stoppedInstanceMetadata);

                meteringService.sendMeteringStatusChangeEventForStack(stack, SCALE_DOWN);

                sendEvent(context, STOPSTART_DOWNSCALE_FINALIZED_EVENT.event(), payload);
            }

            private void handleNotStartedInstances(StopStartDownscaleContext context, List<CloudVmInstanceStatus> cloudVmInstanceStatusList) {
                try {
                    List<CloudVmInstanceStatus> instancesNotInDesiredState = cloudVmInstanceStatusList.stream()
                            .filter(i -> i.getStatus() != InstanceStatus.STOPPED).collect(Collectors.toList());
                    if (instancesNotInDesiredState.size() > 0) {
                        // Not updating the status of these instances in the DB. Instead letting the regular syncer threads take care of this.
                        // This is in case there is additional logic in the syncers while processing Instance state changes.
                        LOGGER.warn("Some instances could not be stopped: count={}, instances={}",
                                instancesNotInDesiredState.size(), instancesNotInDesiredState);
                        stopStartDownscaleFlowService.logInstancesFailedToStop(context.getStack().getId(), instancesNotInDesiredState);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed while attempting to log info about instances which did not stop. Ignoring, and letting flow proceed", e);
                }
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_DECOMMISSION_VIA_CM_FAILED_STATE")
    public Action<?, ?> decommissionViaCmFailedAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleDecommissionViaCMResult.class) {

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleDecommissionViaCMResult payload, Map<Object, Object> variables)
                    throws Exception {
                LOGGER.warn("Failure during the decommissionViaCm step");
                // TODO CB-14929. Should the nodes be put into an ORCHESTRATOR_FAILED state? What are the manual recovery steps from this state.
                Set<String> hostnames = getHostNamesForPrivateIds(payload.getRequest().getInstanceIdsToDecommission(), context.getStack());
                stopStartDownscaleFlowService.decommissionViaCmFailed(payload.getResourceId(), hostnames);
                sendEvent(context, STOPSTART_DOWNSCALE_FAILURE_EVENT.event(), new StackFailureEvent(payload.getResourceId(), payload.getErrorDetails()));
            }

            private Set<String> getHostNamesForPrivateIds(Set<Long> hostIdsToRemove, StackDtoDelegate stack) {
                return hostIdsToRemove.stream().map(privateId -> {
                    List<InstanceMetadataView> instanceMetaDataList = stack.getNotTerminatedInstanceMetaData();
                    Optional<InstanceMetadataView> instanceMetadata = stackService.getInstanceMetadata(instanceMetaDataList, privateId);
                    return instanceMetadata.map(InstanceMetadataView::getDiscoveryFQDN).orElse(null);
                }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_STOP_INSTANCES_FAILED_STATE")
    public Action<?, ?> stopInstancesFailedAction() {
        return new AbstractStopStartDownscaleActions<>(StopStartDownscaleStopInstancesResult.class) {

            @Override
            protected void doExecute(StopStartDownscaleContext context, StopStartDownscaleStopInstancesResult payload, Map<Object, Object> variables)
                    throws Exception {
                LOGGER.warn("Failure during the stopInstancesOnCloudProvider step");
                // TODO CB-14929. Should the nodes be put into an ORCHESTRATOR_FAILED state? What are the manual recovery steps from this state.
                stopStartDownscaleFlowService.stopInstancesFailed(payload.getResourceId(), payload.getStopInstancesRequest().getCloudInstancesToStop());
                sendEvent(context, STOPSTART_DOWNSCALE_FAILURE_EVENT.event(), new StackFailureEvent(payload.getResourceId(), payload.getErrorDetails()));
            }
        };
    }

    @Bean(name = "STOPSTART_DOWNSCALE_FAILED_STATE")
    public Action<?, ?> clusterDownscaleFailedAction() {
        return new AbstractStackFailureAction<StopStartDownscaleState, StopStartDownscaleEvent>() {

            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Handling a failure from Downscale via Instance Stop");
                stopStartDownscaleFlowService.handleClusterDownscaleFailure(context.getStackId(), payload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FAIL_HANDLED_EVENT.event(), context.getStackId());
            }
        };
    }

    @VisibleForTesting
    abstract static class AbstractStopStartDownscaleActions<P extends Payload>
            extends AbstractStackAction<StopStartDownscaleState, StopStartDownscaleEvent, StopStartDownscaleContext, P> {

        static final String HOSTGROUPNAME = "HOSTGROUPNAME";

        static final String HOSTS_TO_REMOVE = "HOSTS_TO_REMOVE";

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

            MDCBuilder.buildMdcContext(stack.getCluster());
            Location location = location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone()));


            CloudContext cloudContext = CloudContext.Builder.builder()
                    .withId(stack.getId())
                    .withName(stack.getName())
                    .withCrn(stack.getResourceCrn())
                    .withPlatform(stack.getCloudPlatform())
                    .withVariant(stack.getPlatformVariant())
                    .withLocation(location)
                    .withWorkspaceId(stack.getWorkspaceId())
                    .withAccountId(Crn.safeFromString(stack.getResourceCrn()).getAccountId())
                    .withTenantId(stack.getTenant().getId())
                    .build();
            CloudCredential cloudCredential = stackUtil.getCloudCredential(stack.getEnvironmentCrn());
            CloudStack cloudStack = cloudStackConverter.convert(stack);

            return new StopStartDownscaleContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack,
                    getHostgroupName(variables), getHostsToRemove(variables), ClusterManagerType.CLOUDERA_MANAGER);
        }

        private String getHostgroupName(Map<Object, Object> variables) {
            return (String) variables.get(HOSTGROUPNAME);
        }

        Set<Long> getHostsToRemove(Map<Object, Object> variables) {
            return (Set<Long>) variables.get(HOSTS_TO_REMOVE);
        }
    }
}
