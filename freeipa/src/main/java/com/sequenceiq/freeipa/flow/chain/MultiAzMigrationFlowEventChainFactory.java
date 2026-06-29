package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.FREEIPA_MULTI_AZ_MIGRATION_FAILED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowEventContext;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeState;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationInitState;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.freeipa.flow.stack.migration.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@Component
public class MultiAzMigrationFlowEventChainFactory implements FlowEventChainFactory<MultiAzMigrationEvent>, FreeIpaUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationFlowEventChainFactory.class);

    private static final int MAX_NODE_COUNT_FOR_UPSCALE = AvailabilityType.HA.getInstanceCount() + 1;

    private static final int MAX_NODE_COUNT_FOR_DOWNSCALE = AvailabilityType.HA.getInstanceCount();

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private EventSenderService eventSenderService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private OperationService operationService;

    @Inject
    private FreeipaJobService freeipaJobService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.MULTI_AZ_MIGRATION_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(MultiAzMigrationEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();

        flowEventChain.addAll(createInitFlow(event));
        flowEventChain.addAll(createMigrationInitFlow(event));
        flowEventChain.addAll(createScaleEventsAndChangePrimaryGatewayFlowIfNeeded(event));
        flowEventChain.addAll(createScaleEventsForNonPrimaryGatewayFlowIfNeeded(event));
        flowEventChain.addAll(createMigrationFinalizeFlow(event));
        flowEventChain.addAll(createFinalizeFlow(event));

        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    private List<Selectable> createInitFlow(MultiAzMigrationEvent event) {
        return List.of(
                new FlowChainInitPayload(
                        getName(),
                        event.getResourceId(),
                        event.accepted()
                )
        );
    }

    private List<Selectable> createMigrationInitFlow(MultiAzMigrationEvent event) {
        return List.of(
                new MultiAzMigrationInitTriggerEvent(
                        MultiAzMigrationInitFlowEvent.MULTI_AZ_MIGRATION_INIT_EVENT.event(),
                        event.getResourceId(),
                        event.getOperationId())
        );
    }

    private List<Selectable> createScaleEventsAndChangePrimaryGatewayFlowIfNeeded(MultiAzMigrationEvent event) {
        int instanceCountForUpscale = instanceCountForUpscale(event);
        int instanceCountForDownscale = instanceCountForDownscale(event);
        Set<String> groupNames = groupNames(event);
        ArrayList<String> instanceIds = Lists.newArrayList(event.getPrimaryGwInstanceId());
        LOGGER.debug("Add events for primary gateway with id: [{}]", event.getPrimaryGwInstanceId());

        List<Selectable> events = new ArrayList<>();
        if (event.shouldRecreatePrimaryGw()) {
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "Resource create flow"));
            events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(), event.getResourceId(), forceAzRecalculation(), instanceCountForUpscale,
                    Boolean.FALSE, true, false, event.getOperationId(), event.getTargetVariant().getValue(), Set.of(event.getPrimaryGwInstanceId())));
            events.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                    new ArrayList<String>(event.getInstanceIds()), Boolean.FALSE, event.getOperationId()));
            events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), event.getResourceId(), instanceIds, instanceCountForDownscale, false,
                    true, false, event.getOperationId()));
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "CloudFormation cleanup"));
        } else {
            LOGGER.info("Primary gateway does not need to be recreated.");
        }
        return events;
    }

    private List<Selectable> createScaleEventsForNonPrimaryGatewayFlowIfNeeded(MultiAzMigrationEvent event) {
        int instanceCountForUpscale = instanceCountForUpscale(event);
        int instanceCountForDownscale = instanceCountForDownscale(event);
        Set<String> groupNames = groupNames(event);
        Set<String> nonPrimaryGwInstanceIdsToRecreate = event.getNonPrimaryGwInstanceIdsToRecreate();
        LOGGER.debug("Add scale events for non primary gateway instances. upscale count: [{}] downscale count: [{}]",
                instanceCountForUpscale, instanceCountForDownscale);

        List<Selectable> events = new ArrayList<>();
        if (!nonPrimaryGwInstanceIdsToRecreate.isEmpty()) {
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "Non PGW instances found, migration flow"));
            for (String instanceId : nonPrimaryGwInstanceIdsToRecreate) {
                LOGGER.debug("Add upscale and downscale event for [{}]", instanceId);
                ArrayList<String> instanceIdToDownscale = Lists.newArrayList(instanceId);
                events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(), event.getResourceId(), forceAzRecalculation(), instanceCountForUpscale,
                        Boolean.FALSE, true, false, event.getOperationId(), event.getTargetVariant().getValue(), Set.of(instanceId)));
                events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                        event.getResourceId(), instanceIdToDownscale, instanceCountForDownscale, false, true, false, event.getOperationId()));
            }
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "Non PGW instances found, cloudFormation cleanup"));
        }
        return events;
    }

    private List<AwsVariantMigrationTriggerEvent> createMigrationFlowIfNeeded(MultiAzMigrationEvent event, Set<String> groupNames, String flow) {
        if (event.variantMigrationNeeded()) {
            LOGGER.debug("{} flow added to FreeIPA multi-AZ migration.", flow);
            return groupNames.stream()
                    .map(g -> new AwsVariantMigrationTriggerEvent(AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(), event.getResourceId(), g))
                    .toList();
        } else {
            return List.of();
        }
    }

    private List<Selectable> createMigrationFinalizeFlow(MultiAzMigrationEvent event) {
        return List.of(
                new MultiAzMigrationFinalizeTriggerEvent(
                        MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_FINALIZE_EVENT.event(),
                        event.getResourceId(),
                        event.getOperationId())
        );
    }

    private List<Selectable> createFinalizeFlow(MultiAzMigrationEvent event) {
        return List.of(
                new FlowChainFinalizePayload(
                        getName(),
                        event.getResourceId(),
                        event.accepted()
                )
        );
    }

    private Set<String> groupNames(MultiAzMigrationEvent event) {
        return instanceGroupService.findGroupNamesByStackId(event.getResourceId());
    }

    private int instanceCountForDownscale(MultiAzMigrationEvent event) {
        int target = Math.min(getDesiredNodeCount(event), MAX_NODE_COUNT_FOR_DOWNSCALE);
        LOGGER.info("Multi-AZ migration: planned downscale target node count is [{}].", target);
        return target;
    }

    private int instanceCountForUpscale(MultiAzMigrationEvent event) {
        int target = Math.min(getDesiredNodeCount(event) + 1, MAX_NODE_COUNT_FOR_UPSCALE);
        LOGGER.info("Multi-AZ migration: planned upscale target node count is [{}].", target);
        return target;
    }

    /**
     * Returns the canonical node count for the stack's FreeIPA shape, derived by snapping the current
     * instance set size to {@link AvailabilityType} (NON_HA=1, TWO_NODE_BASED=2, HA=3).
     */
    private int getDesiredNodeCount(MultiAzMigrationEvent event) {
        int currentCount = Optional.ofNullable(event.getInstanceIds()).map(Set::size).orElse(0);
        return AvailabilityType.getByInstanceCount(currentCount).getInstanceCount();
    }

    /**
     * The upscale flow treats an empty instanceIds list as the signal to recalculate AZ distribution from
     * scratch. Each call returns a fresh mutable list because the consuming flow may mutate it.
     */
    @SuppressWarnings("IllegalType")
    private static ArrayList<String> forceAzRecalculation() {
        return new ArrayList<>();
    }

    @Override
    public void onFlowChainFailure(FlowEventContext flowEventContext) {
        Stack stack = stackService.getStackById(flowEventContext.getResourceId());
        stackUpdater.updateStackStatus(stack, DetailedStackStatus.MULTI_AZ_MIGRATION_FAILED,
                "FreeIPA multi-AZ migration failed in one of the intermediate flows.");
        eventSenderService.sendEventAndNotification(stack, flowEventContext.getFlowTriggerUserCrn(), FREEIPA_MULTI_AZ_MIGRATION_FAILED,
                List.of("FreeIPA multi-AZ migration failed in one of the intermediate flows."));
        try {
            Operation operation = operationService.getLatestOperationForEnvironmentCrnAndOperationType(
                    stack.getEnvironmentCrn(), OperationType.MIGRATE_TO_MULTI_AZ);
            if (OperationState.RUNNING.equals(operation.getStatus())) {
                operationService.failOperation(stack.getAccountId(), operation.getOperationId(),
                        "FreeIPA multi-AZ migration failed in one of the intermediate flows.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to fail operation for multi-AZ migration chain failure on stack {}.", flowEventContext.getResourceId(), e);
        }
        LOGGER.info("Re-enabling status checker for stack {} after multi-AZ migration chain failure.", flowEventContext.getResourceId());
        freeipaJobService.schedule(stack.getId());
    }

    @Override
    public UsageProto.CDPFreeIPAStatus.Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        return switch (flowState) {
            case FlowChainInitState s when s == FlowChainInitState.INIT_STATE ->
                    UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_STARTED;
            case FlowChainFinalizeState s when s == FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE ->
                    UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_FINISHED;
            case MultiAzMigrationInitState s when s == MultiAzMigrationInitState.MULTI_AZ_MIGRATION_INIT_FAILED_STATE ->
                    UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_FAILED;
            case MultiAzMigrationFinalizeState s when s == MultiAzMigrationFinalizeState.MULTI_AZ_MIGRATION_FINALIZE_FAILED_STATE ->
                    UsageProto.CDPFreeIPAStatus.Value.MULTI_AZ_MIGRATION_FAILED;
            default -> UsageProto.CDPFreeIPAStatus.Value.UNSET;
        };
    }
}
