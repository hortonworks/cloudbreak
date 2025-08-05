package com.sequenceiq.freeipa.flow.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UNSET;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UPGRADE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UPGRADE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value.UPGRADE_STARTED;
import static com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvents.IMAGE_CHANGE_EVENT;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_MASTER_KEY_PAIR;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.SALT_SIGN_KEY_PAIR;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAStatus.Value;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.chain.SecretRotationFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.FreeIpaUseCaseAware;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.ChangePrimaryGatewayFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event.ChangePrimaryGatewayEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.update.SaltUpdateTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.upgrade.UpgradeEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.UpscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.event.FreeIpaVerticalScalingTriggerEvent;
import com.sequenceiq.freeipa.flow.stack.image.change.event.ImageChangeEvent;
import com.sequenceiq.freeipa.flow.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.freeipa.flow.stack.migration.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@Component
public class UpgradeFlowEventChainFactory implements FlowEventChainFactory<UpgradeEvent>, FreeIpaUseCaseAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeFlowEventChainFactory.class);

    private static final int PRIMARY_GW_EVENT_COUNT = 3;

    private static final int PRIMARY_GW_INSTANCE_COUNT = 1;

    private static final int MAX_NODE_COUNT_FOR_UPSCALE = AvailabilityType.HA.getInstanceCount() + 1;

    private static final int MAX_NODE_COUNT_FOR_DOWNSCALE = AvailabilityType.HA.getInstanceCount();

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private StackService stackService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.UPGRADE_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(UpgradeEvent event) {
        Queue<Selectable> flowEventChain = new ConcurrentLinkedQueue<>();
        flowEventChain.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        getSaltSecretRotationTriggerEvent(event.getResourceId()).ifPresent(flowEventChain::add);
        flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(), event.accepted(), true, false, event.getOperationId()));
        if (event.getVerticalScaleRequest() != null) {
            flowEventChain.add(new FreeIpaVerticalScalingTriggerEvent(STACK_VERTICALSCALE_EVENT.event(), event.getResourceId(),
                    event.getVerticalScaleRequest()).withOperationId(event.getOperationId()));
        }
        flowEventChain.add(new ImageChangeEvent(IMAGE_CHANGE_EVENT.event(), event.getResourceId(), event.getImageSettingsRequest())
                .withOperationId(event.getOperationId()));

        int nonPrimaryGwInstanceCount = event.getInstanceIds().size();
        int instanceCountForUpscale = Math.min(nonPrimaryGwInstanceCount + PRIMARY_GW_INSTANCE_COUNT + 1, MAX_NODE_COUNT_FOR_UPSCALE);
        int instanceCountForDownscale = Math.min(nonPrimaryGwInstanceCount + PRIMARY_GW_INSTANCE_COUNT, MAX_NODE_COUNT_FOR_DOWNSCALE);
        Set<String> groupNames = instanceGroupService.findGroupNamesByStackId(event.getResourceId());
        flowEventChain.addAll(createScaleEventsAndChangePgw(event, instanceCountForUpscale, instanceCountForDownscale, groupNames));
        flowEventChain.addAll(createScaleEventsForNonPgwInstances(event, instanceCountForUpscale, instanceCountForDownscale, groupNames));
        flowEventChain.add(new SaltUpdateTriggerEvent(event.getResourceId(), event.accepted(), true, true, event.getOperationId()));
        flowEventChain.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return new FlowTriggerEventQueue(getName(), event, flowEventChain);
    }

    @Override
    public Value getUseCaseForFlowState(Enum<? extends FlowState> flowState) {
        if (FlowChainInitState.INIT_STATE.equals(flowState)) {
            return UPGRADE_STARTED;
        } else if (FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE.equals(flowState)) {
            return UPGRADE_FINISHED;
        } else if (flowState.toString().contains("_FAIL")) {
            return UPGRADE_FAILED;
        } else {
            return UNSET;
        }
    }

    private List<Selectable> createScaleEventsAndChangePgw(UpgradeEvent event, int instanceCountForUpscale, int instanceCountForDownscale,
            Set<String> groupNames) {
        LOGGER.debug("Add events for primary gateway with id: [{}]", event.getPrimareGwInstanceId());
        List<Selectable> events = new ArrayList<>(PRIMARY_GW_EVENT_COUNT);
        ArrayList<String> instanceIdToDownscale = Lists.newArrayList(event.getPrimareGwInstanceId());
        if (CollectionUtils.isEmpty(event.getInstancesOnOldImage()) || event.getInstancesOnOldImage().contains(event.getPrimareGwInstanceId())) {
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "Resource create flow"));
            events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(), event.getResourceId(), instanceIdToDownscale,
                    instanceCountForUpscale, Boolean.FALSE, true, false, event.getOperationId(), event.getTriggeredVariant()));
            ArrayList<String> oldInstances = new ArrayList<>(event.getInstanceIds());
            oldInstances.add(event.getPrimareGwInstanceId());
            events.add(new ChangePrimaryGatewayEvent(ChangePrimaryGatewayFlowEvent.CHANGE_PRIMARY_GATEWAY_EVENT.event(), event.getResourceId(),
                    oldInstances, Boolean.FALSE, event.getOperationId()));
            events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(), event.getResourceId(), instanceIdToDownscale,
                    instanceCountForDownscale, false, true, false, event.getOperationId()));
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "CloudFormation cleanup"));
        } else {
            LOGGER.info("Primary gateway is already upgraded");
        }
        return events;
    }

    private List<Selectable> createScaleEventsForNonPgwInstances(UpgradeEvent event, int instanceCountForUpscale, int instanceCountForDownscale,
            Set<String> groupNames) {
        LOGGER.debug("Add scale events for non primary gateway instances. upscale count: [{}] downscale count: [{}]",
                instanceCountForUpscale, instanceCountForDownscale);
        List<Selectable> events = new ArrayList<>(event.getInstanceIds().size() * 2);
        Set<String> instanceIds = CollectionUtils.isEmpty(event.getInstancesOnOldImage()) ? event.getInstanceIds()
                : event.getInstanceIds().stream().filter(instanceId -> event.getInstancesOnOldImage().contains(instanceId)).collect(Collectors.toSet());
        if (!instanceIds.isEmpty()) {
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "Non Pw instances found, migration flow"));
        }
        for (String instanceId : instanceIds) {
            LOGGER.debug("Add upscale and downscale event for [{}]", instanceId);
            ArrayList<String> instanceIdToDownscale = Lists.newArrayList(instanceId);
            events.add(new UpscaleEvent(UpscaleFlowEvent.UPSCALE_EVENT.event(), event.getResourceId(), instanceIdToDownscale, instanceCountForUpscale,
                    Boolean.FALSE, true, false, event.getOperationId(), event.getTriggeredVariant()));
            events.add(new DownscaleEvent(DownscaleFlowEvent.DOWNSCALE_EVENT.event(),
                    event.getResourceId(), instanceIdToDownscale, instanceCountForDownscale, false, true, false, event.getOperationId()));
        }
        if (!instanceIds.isEmpty()) {
            events.addAll(createMigrationFlowIfNeeded(event, groupNames, "Non Pw instances found, cloudFormation cleanup"));
        }
        return events;
    }

    private List<AwsVariantMigrationTriggerEvent> createMigrationFlowIfNeeded(UpgradeEvent event, Set<String> groupNames, String flow) {
        if (event.isNeedMigration()) {
            LOGGER.debug(flow + " flow added to FreeIPA upgrade");
            return groupNames.stream()
                    .map(g -> new AwsVariantMigrationTriggerEvent(AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(), event.getResourceId(), g))
                    .collect(Collectors.toList());
        } else {
            return List.of();
        }
    }

    public Optional<SecretRotationFlowChainTriggerEvent> getSaltSecretRotationTriggerEvent(Long stackId) {
        List<SecretType> secretTypes = new ArrayList<>();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        SaltSecurityConfig saltSecurityConfig = stack.getSecurityConfig().getSaltSecurityConfig();
        if (StringUtils.isNotEmpty(saltSecurityConfig.getLegacySaltSignPublicKey())) {
            secretTypes.add(SALT_SIGN_KEY_PAIR);
        }
        if (StringUtils.isEmpty(saltSecurityConfig.getSaltMasterPrivateKeyVault())
                || StringUtils.isNotEmpty(saltSecurityConfig.getLegacySaltMasterPublicKey())) {
            secretTypes.add(SALT_MASTER_KEY_PAIR);
        }
        if (secretTypes.isEmpty()) {
            LOGGER.info("Secret rotation is not required.");
            return Optional.empty();
        } else {
            LOGGER.info("Secret rotation flow chain trigger added with secret types: {}", secretTypes);
            return Optional.of(new SecretRotationFlowChainTriggerEvent(EventSelectorUtil.selector(SecretRotationFlowChainTriggerEvent.class),
                    stackId, stack.getEnvironmentCrn(), secretTypes, null, null));
        }
    }
}
