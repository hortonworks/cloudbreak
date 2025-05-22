package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.REPAIR_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.REPAIR_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.REPAIR_STARTED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.UNSET;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationFlowEvent.SKU_MIGRATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.CoreVerticalScaleEvent.STACK_VERTICALSCALE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleEvent.STACK_DOWNSCALE_EVENT;
import static com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent.RepairType.ALL_AT_ONCE;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleState;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration.SkuMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.AwsVariantMigrationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterAndStackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.CoreVerticalScalingTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.ImageValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StopStartUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.EmbeddedDbUpgradeFlowTriggersFactory;
import com.sequenceiq.cloudbreak.core.flow2.stack.migration.AwsVariantMigrationEvent;
import com.sequenceiq.cloudbreak.core.flow2.validate.image.config.ImageValidationEvent;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterRepairTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RescheduleStatusCheckTriggerEvent;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseAware;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.chain.FlowEventChainFactory;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.core.chain.finalize.config.FlowChainFinalizeState;
import com.sequenceiq.flow.core.chain.finalize.flowevents.FlowChainFinalizePayload;
import com.sequenceiq.flow.core.chain.init.config.FlowChainInitState;
import com.sequenceiq.flow.core.chain.init.flowevents.FlowChainInitPayload;

@Component
public class ClusterRepairFlowEventChainFactory implements FlowEventChainFactory<ClusterRepairTriggerEvent>, ClusterUseCaseAware {

    private static final Logger LOGGER = getLogger(ClusterRepairFlowEventChainFactory.class);

    @Value("${cb.root.disk.repair.migration.enabled:true}")
    private boolean rootDiskRepairMigrationEnabled;

    @Inject
    private StackService stackService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private EmbeddedDbUpgradeFlowTriggersFactory embeddedDbUpgradeFlowTriggersFactory;

    @Inject
    private StackUpgradeService stackUpgradeService;

    @Inject
    private ScalingHardLimitsService scalingHardLimitsService;

    @Inject
    private DefaultRootVolumeSizeProvider rootVolumeSizeProvider;

    @Inject
    private SkuMigrationService skuMigrationService;

    @Override
    public String initEvent() {
        return FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;
    }

    @Override
    public FlowTriggerEventQueue createFlowTriggerEventQueue(ClusterRepairTriggerEvent event) {
        LOGGER.debug("Creating repair flow chain with stack id: '{}'", event.getStackId());
        StackDto stackDto = stackDtoService.getById(event.getStackId());
        RepairConfig repairConfig = createRepairConfig(event, stackDto.getCluster());
        Queue<Selectable> flowTriggers = createFlowTriggers(event, repairConfig, stackDto);
        return new FlowTriggerEventQueue(getName(), event, flowTriggers);
    }

    @Override
    public CDPClusterStatus.Value getUseCaseForFlowState(Enum flowState) {
        if (FlowChainInitState.INIT_STATE.equals(flowState)) {
            return REPAIR_STARTED;
        } else if (FlowChainFinalizeState.FLOWCHAIN_FINALIZE_FINISHED_STATE.equals(flowState)) {
            return REPAIR_FINISHED;
        } else if (flowState.toString().endsWith("FAILED_STATE") &&
                !ClusterDownscaleState.DECOMISSION_FAILED_STATE.equals(flowState) &&
                !ClusterDownscaleState.REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE.equals(flowState)) {
            return REPAIR_FAILED;
        } else {
            return UNSET;
        }
    }

    private RepairConfig createRepairConfig(ClusterRepairTriggerEvent event, ClusterView clusterView) {
        RepairConfig repairConfig = new RepairConfig();
        for (Entry<String, List<String>> failedNodes : event.getFailedNodesMap().entrySet()) {
            String hostGroupName = failedNodes.getKey();
            List<String> hostNames = failedNodes.getValue();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(clusterView.getId(), hostGroupName).orElseThrow();
            InstanceGroup instanceGroup = hostGroup.getInstanceGroup();
            if (InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType())) {
                Optional<String> primaryGatewayHostName = instanceMetaDataService.getPrimaryGatewayDiscoveryFQDNByInstanceGroup(event.getStackId(),
                        instanceGroup.getId());
                boolean primaryGatewayRepairable = primaryGatewayHostName.isPresent() && hostNames.contains(primaryGatewayHostName.get());
                if (primaryGatewayRepairable) {
                    repairConfig.setSinglePrimaryGateway(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
                } else {
                    repairConfig.addRepairs(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
                }
            } else {
                repairConfig.addRepairs(new Repair(instanceGroup.getGroupName(), hostGroup.getName(), hostNames));
            }
        }
        return repairConfig;
    }

    private Queue<Selectable> createFlowTriggers(ClusterRepairTriggerEvent event, RepairConfig repairConfig, StackDto stackDto) {
        StackView stackView = stackDto.getStack();

        String hostGroup = "";
        List<String> stoppedInstances = new ArrayList<>();
        List<String> selectInstances = new ArrayList<>();
        for (List<String> instances : event.getFailedNodesMap().values()) {
            selectInstances.addAll(instances);
        }
        for (InstanceMetadataView instanceMetadataView : stackDto.getNotTerminatedInstanceMetaData()) {
            if (instanceMetadataView.getInstanceStatus().equals(InstanceStatus.STOPPED) && !selectInstances.contains(instanceMetadataView.getDiscoveryFQDN())) {
                stoppedInstances.add(instanceMetadataView.getInstanceId());
                hostGroup = instanceMetadataView.getInstanceGroupName();
            }
        }

        Queue<Selectable> flowTriggers = new ConcurrentLinkedDeque<>();
        flowTriggers.add(new FlowChainInitPayload(getName(), event.getResourceId(), event.accepted()));
        flowTriggers.add(new ImageValidationTriggerEvent(ImageValidationEvent.IMAGE_VALIDATION_EVENT.event(), event.getResourceId()));
        if (!stoppedInstances.isEmpty() && stackView.getType().equals(StackType.WORKLOAD)) {
            addClusterScaleTriggerEventIfNeeded(stackView, flowTriggers, stoppedInstances, hostGroup);
        }
        Map<String, Set<String>> repairableGroupsWithHostNames = new HashMap<>();
        boolean singlePrimaryGW = fillRepairableGroupsWithHostNames(repairConfig, repairableGroupsWithHostNames);
        LOGGER.info("Repairable groups with host names: {}", repairableGroupsWithHostNames);
        flowTriggers.addAll(embeddedDbUpgradeFlowTriggersFactory.createFlowTriggers(stackDto, event.isUpgrade()));
        if (rootDiskRepairMigrationEnabled) {
            addRootDiskUpdateIfNecessary(event, stackDto, repairableGroupsWithHostNames, flowTriggers);
        }
        addSkuMigrationIfNecessary(stackDto, flowTriggers);
        addDownscaleAndUpscaleEvents(event, flowTriggers, repairableGroupsWithHostNames, singlePrimaryGW, stackDto);
        flowTriggers.add(rescheduleStatusCheckEvent(event));
        flowTriggers.add(new FlowChainFinalizePayload(getName(), event.getResourceId(), event.accepted()));
        return flowTriggers;
    }

    private void addSkuMigrationIfNecessary(StackDto stackDto, Queue<Selectable> flowTriggers) {
        if (skuMigrationService.isRepairSkuMigrationEnabled()) {
            if (skuMigrationService.isMigrationNecessary(stackDto)) {
                LOGGER.info("Lets do the BASIC to STANDARD SKU migration before repair");
                SkuMigrationTriggerEvent skuMigrationTriggerEvent =
                        new SkuMigrationTriggerEvent(SKU_MIGRATION_EVENT.event(), stackDto.getId(), false);
                flowTriggers.add(skuMigrationTriggerEvent);
            }
        }
    }

    private void addClusterScaleTriggerEventIfNeeded(StackView stackView, Queue<Selectable> flowEventChain, List<String> stoppedInstances, String hostGroup) {
        String selector = FlowChainTriggers.STOPSTART_UPSCALE_CHAIN_TRIGGER_EVENT;
        boolean stopStartFailureRecoveryEnabled = stackUtil.stopStartScalingFailureRecoveryEnabled(stackView);
        flowEventChain.add(
                new StopStartUpscaleTriggerEvent(
                        StopStartUpscaleEvent.STOPSTART_UPSCALE_TRIGGER_EVENT.event(),
                        stackView.getId(),
                        hostGroup,
                        stoppedInstances.size(),
                        ClusterManagerType.CLOUDERA_MANAGER,
                        stopStartFailureRecoveryEnabled));
    }

    private void addRootDiskUpdateIfNecessary(ClusterRepairTriggerEvent event, StackDto stackDto, Map<String, Set<String>> repairableGroupsWithHostNames,
            Queue<Selectable> flowTriggers) {
        for (String group : repairableGroupsWithHostNames.keySet()) {
            stackDto.getInstanceGroupDtos().stream()
                    .filter(instanceGroupDto -> group.equals(instanceGroupDto.getInstanceGroup().getGroupName()))
                    .findFirst().ifPresent(instanceGroupDto -> {
                        Template groupTemplate = instanceGroupDto.getInstanceGroup().getTemplate();
                        int defaultRootVolumeSize = rootVolumeSizeProvider.getDefaultRootVolumeForPlatform(stackDto.getCloudPlatform(),
                                InstanceGroupType.isGateway(instanceGroupDto.getInstanceGroup().getInstanceGroupType()));
                        if (groupTemplate.getRootVolumeSize() < defaultRootVolumeSize) {
                            StackVerticalScaleV4Request stackVerticalScaleV4Request =
                                    createStackVerticalScaleV4Request(event, group, defaultRootVolumeSize, groupTemplate.getInstanceType());
                            CoreVerticalScalingTriggerEvent coreVerticalScalingTriggerEvent =
                                    new CoreVerticalScalingTriggerEvent(STACK_VERTICALSCALE_EVENT.event(), event.getStackId(), stackVerticalScaleV4Request);
                            flowTriggers.add(coreVerticalScalingTriggerEvent);
                        }
                    });
        }
    }

    private StackVerticalScaleV4Request createStackVerticalScaleV4Request(ClusterRepairTriggerEvent event, String group,
            int defaultRootVolumeSize, String instanceType) {
        StackVerticalScaleV4Request stackVerticalScaleV4Request = new StackVerticalScaleV4Request();
        stackVerticalScaleV4Request.setGroup(group);
        stackVerticalScaleV4Request.setStackId(event.getStackId());
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(defaultRootVolumeSize);
        template.setRootVolume(rootVolume);
        template.setInstanceType(instanceType);
        stackVerticalScaleV4Request.setTemplate(template);
        return stackVerticalScaleV4Request;
    }

    private boolean fillRepairableGroupsWithHostNames(RepairConfig repairConfig, Map<String, Set<String>> repairableGroupsWithHostNames) {
        boolean singlePrimaryGW = addGatewayGroupWithHostNames(repairConfig, repairableGroupsWithHostNames);
        repairableGroupsWithHostNames.putAll(repairConfig.getRepairs().stream().collect(Collectors.toMap(Repair::hostGroupName,
                repair -> new HashSet<>(repair.hostNames()))));
        return singlePrimaryGW;
    }

    private void addDownscaleAndUpscaleEvents(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers, Map<String,
            Set<String>> repairableGroupsWithHostNames, boolean singlePrimaryGW, StackDto stack) {
        if (ALL_AT_ONCE.equals(event.getRepairType())) {
            LOGGER.info("Upgrading all the nodes by groups, upgrading all the nodes within a group at the same time, for stack: '{}'", event.getStackId());
            addRepairFlows(event, flowTriggers, repairableGroupsWithHostNames, singlePrimaryGW, stack.getStack());
        } else {
            LOGGER.info("Special repair: '{}'", event.getRepairType());
            specialRepair(event, flowTriggers, repairableGroupsWithHostNames, stack, event.getRepairType());
        }
    }

    private void specialRepair(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers,
            Map<String, Set<String>> repairableGroupsWithHostNames, StackDto stack, ClusterRepairTriggerEvent.RepairType repairType) {
        Optional<String> primaryGwFQDN = stack.getPrimaryGatewayFQDN();
        Set<String> secondaryGwFQDNs = stack.getSecondaryGatewayFQDNs();
        HashMultimap<String, String> repairableGroupsWithHostNameMultimap = HashMultimap.create();
        repairableGroupsWithHostNames.forEach(repairableGroupsWithHostNameMultimap::putAll);
        LinkedHashMultimap<String, String> hostsByHostGroupAndSortedByPgwAndGw =
                collectHostsByHostGroupAndSortByPGwGwAndName(primaryGwFQDN, secondaryGwFQDNs, repairableGroupsWithHostNameMultimap);
        switch (repairType) {
            case ONE_BY_ONE -> addRepairFlowsForEachNode(event, flowTriggers, hostsByHostGroupAndSortedByPgwAndGw, primaryGwFQDN, stack.getStack());
            case BATCH -> addBatchedRepairFlows(event, flowTriggers, hostsByHostGroupAndSortedByPgwAndGw, primaryGwFQDN, stack.getStack());
            default -> throw new IllegalStateException("Unknown repair type:" + repairType);
        }
    }

    private void addRepairFlowsForEachNode(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers,
            LinkedHashMultimap<String, String> hostsByHostGroupAndSortedByPgw, Optional<String> primaryGwFQDN, StackView stackView) {
        hostsByHostGroupAndSortedByPgw.asMap().forEach((hostGroupName, instances) -> {
            LinkedHashMultimap<String, String> hostGroup = LinkedHashMultimap.create();
            hostGroup.putAll(hostGroupName, List.copyOf(instances));
            addRepairFlowsForEachGroupsWithOneNode(event, flowTriggers, hostGroup, primaryGwFQDN, stackView);
        });
    }

    private LinkedHashMultimap<String, String> collectHostsByHostGroupAndSortByPGwGwAndName(Optional<String> primaryGwFQDN, Set<String> secondaryGwFQDNs,
            HashMultimap<String, String> repairableGroupsWithHostNameMultimap) {
        return repairableGroupsWithHostNameMultimap.entries().stream()
                .sorted(Entry.comparingByValue((o1, o2) -> {

                    // primary gw first
                    if (primaryGwFQDN.filter(o1::equals).isPresent()) {
                        return -1;
                    } else if (primaryGwFQDN.filter(o2::equals).isPresent()) {
                        return +1;
                    }

                    // secondary gateways come after primary but before others (lexicographic between secondaries)
                    boolean o1IsSecondary = secondaryGwFQDNs.contains(o1);
                    boolean o2IsSecondary = secondaryGwFQDNs.contains(o2);

                    if (o1IsSecondary && o2IsSecondary) {
                        return o1.compareTo(o2);
                    } else if (o1IsSecondary) {
                        return -1;
                    } else if (o2IsSecondary) {
                        return +1;
                    }

                    // fallback
                    if (o1.length() > o2.length()) {
                        return +1;
                    } else if (o2.length() > o1.length()) {
                        return -1;
                    } else {
                        return o1.compareTo(o2);
                    }
                }))
                .collect(Multimaps.toMultimap(Entry::getKey, Entry::getValue, LinkedHashMultimap::create));
    }

    private void addBatchedRepairFlows(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers,
            LinkedHashMultimap<String, String> orderedHostMultimap, Optional<String> primaryGwFQDNOptional, StackView stackView) {
        int batchSize = scalingHardLimitsService.getMaxUpscaleStepInNodeCount();
        LOGGER.info("Batch repair with batch size: {}", batchSize);
        while (!orderedHostMultimap.values().isEmpty()) {
            Map<String, Set<String>> repairableGroups = new HashMap<>();
            List<Entry<String, String>> hostsToRepairInOneBatch = orderedHostMultimap.entries().stream().limit(batchSize).toList();
            for (Entry<String, String> hostToRepair : hostsToRepairInOneBatch) {
                repairableGroups.computeIfAbsent(hostToRepair.getKey(), k -> new HashSet<>()).add(hostToRepair.getValue());
                orderedHostMultimap.values().remove(hostToRepair.getValue());
            }
            Set<String> repairedHosts = repairableGroups.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            addRepairFlows(event, flowTriggers, repairableGroups, isPrimaryGWInHosts(primaryGwFQDNOptional, repairedHosts), stackView);
        }
    }

    private void addRepairFlowsForEachGroupsWithOneNode(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers,
            LinkedHashMultimap<String, String> orderedHostMultimap, Optional<String> primaryGwFQDNOptional, StackView stackView) {
        LOGGER.info("Rolling upgrade, repairing one node from each host group at one time, for stack: '{}'", event.getStackId());
        while (!orderedHostMultimap.values().isEmpty()) {
            Map<String, Set<String>> repairableGroupsWithOneHostName = new HashMap<>();
            for (String hostGroup : new HashSet<>(orderedHostMultimap.keySet())) {
                orderedHostMultimap.get(hostGroup).stream().findFirst().ifPresent(hostName -> {
                    repairableGroupsWithOneHostName.put(hostGroup, Collections.singleton(hostName));
                    orderedHostMultimap.values().remove(hostName);
                });
            }
            Set<String> repairedHosts = repairableGroupsWithOneHostName.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            addRepairFlows(event, flowTriggers, repairableGroupsWithOneHostName, isPrimaryGWInHosts(primaryGwFQDNOptional, repairedHosts), stackView);
        }
    }

    private boolean isPrimaryGWInHosts(Optional<String> primaryGwFQDN, Collection<String> hostNames) {
        return hostNames.stream().anyMatch(fqdn -> primaryGwFQDN.filter(fqdn::equals).isPresent());
    }

    private void addRepairFlows(ClusterRepairTriggerEvent event, Queue<Selectable> flowTriggers, Map<String, Set<String>> repairableGroupsWithHostNames,
            boolean singlePrimaryGW, StackView stackView) {
        if (!repairableGroupsWithHostNames.isEmpty()) {
            flowTriggers.add(downscaleEvent(singlePrimaryGW, event, repairableGroupsWithHostNames));
            LOGGER.info("Downscale event added for: {}", repairableGroupsWithHostNames);
            for (Entry<String, Set<String>> groupWithHostNames : repairableGroupsWithHostNames.entrySet()) {
                addAwsNativeEventMigrationIfNeeded(flowTriggers, event, groupWithHostNames.getKey(), stackView);
            }
            flowTriggers.add(fullUpscaleEvent(event, repairableGroupsWithHostNames, singlePrimaryGW,
                    event.isRestartServices(), isKerberosSecured(stackView), event.isRollingRestartEnabled()));
            LOGGER.info("Upscale event added for: {}", repairableGroupsWithHostNames);
        }
    }

    private boolean addGatewayGroupWithHostNames(RepairConfig repairConfig, Map<String, Set<String>> groupsWithHostNames) {
        if (repairConfig.getSinglePrimaryGateway().isPresent()) {
            LOGGER.info("Single primary GW flag true");
            Repair repair = repairConfig.getSinglePrimaryGateway().get();
            Map<String, Set<String>> gatewayGroupWithHostNames = Collections.singletonMap(repair.hostGroupName(), new HashSet<>(repair.hostNames()));
            groupsWithHostNames.putAll(gatewayGroupWithHostNames);
            LOGGER.info("GW group with hostnames are added: {}", gatewayGroupWithHostNames);
            return true;
        } else {
            return false;
        }
    }

    void addAwsNativeEventMigrationIfNeeded(Queue<Selectable> flowTriggers, ClusterRepairTriggerEvent event, String groupName, StackView stackView) {
        String triggeredVariant = event.getTriggeredStackVariant();
        if (event.isUpgrade()) {
            String originalPlatformVariant = stackView.getPlatformVariant();
            LOGGER.debug("Upgrade flow, checking that the variant migration is triggerable from original: '{}' to new: '{}', groupName: '{}'",
                    originalPlatformVariant, triggeredVariant, groupName);
            if (stackUpgradeService.awsVariantMigrationIsFeasible(stackView, triggeredVariant)) {
                LOGGER.info("Migration variant is needed from '{}' to: '{}', groupName: '{}'", originalPlatformVariant, triggeredVariant, groupName);
                flowTriggers.add(awsVariantMigrationTriggerEvent(event.getResourceId(), groupName));
            }
        } else {
            LOGGER.debug("Don't need to migrate the stack, variant: {}, groupName: {}", triggeredVariant, groupName);
        }
    }

    private AwsVariantMigrationTriggerEvent awsVariantMigrationTriggerEvent(Long resourceId, String groupName) {
        return new AwsVariantMigrationTriggerEvent(AwsVariantMigrationEvent.CREATE_RESOURCES_EVENT.event(), resourceId, groupName);
    }

    private StackEvent downscaleEvent(boolean primaryGatewaySelected, ClusterRepairTriggerEvent event, Map<String, Set<String>> groupsWithHostNames) {
        Set<InstanceMetaData> instanceMetaData = instanceMetaDataService.getAllInstanceMetadataWithoutInstanceGroupByStackId(event.getStackId());
        Map<String, Set<Long>> groupsWithPrivateIds = new HashMap<>();
        Map<String, Integer> groupsWithAdjustment = new HashMap<>();
        for (Entry<String, Set<String>> groupWithHostNames : groupsWithHostNames.entrySet()) {
            Set<String> hostNames = groupWithHostNames.getValue();
            String group = groupWithHostNames.getKey();
            Set<Long> privateIdsForHostNames = stackService.getPrivateIdsForHostNames(instanceMetaData, hostNames);
            groupsWithPrivateIds.put(group, privateIdsForHostNames);
            int size = hostNames != null ? hostNames.size() : 0;
            groupsWithAdjustment.put(group, size);
        }
        LOGGER.info("Downscale groups with adjustments: {}", groupsWithAdjustment);
        LOGGER.info("Downscale groups with privateIds: {}", groupsWithPrivateIds);
        if (!primaryGatewaySelected || (event.isUpgrade() && !event.isRollingRestartEnabled())) {
            LOGGER.info("Full downscale for the following: {}", groupsWithHostNames);
            return new ClusterAndStackDownscaleTriggerEvent(FlowChainTriggers.FULL_DOWNSCALE_TRIGGER_EVENT, event.getResourceId(), groupsWithAdjustment,
                    groupsWithPrivateIds, groupsWithHostNames, ScalingType.DOWNSCALE_TOGETHER, event.accepted(),
                    new ClusterDownscaleDetails(true, true, false));
        } else {
            LOGGER.info("Stack downscale for the following: {}", groupsWithHostNames);
            return new StackDownscaleTriggerEvent(STACK_DOWNSCALE_EVENT.event(), event.getResourceId(), groupsWithAdjustment, groupsWithPrivateIds,
                    groupsWithHostNames, event.getTriggeredStackVariant(), event.accepted()).setRepair();
        }
    }

    private RescheduleStatusCheckTriggerEvent rescheduleStatusCheckEvent(ClusterRepairTriggerEvent event) {
        return new RescheduleStatusCheckTriggerEvent(FlowChainTriggers.RESCHEDULE_STATUS_CHECK_TRIGGER_EVENT,
                event.getResourceId(), event.accepted());
    }

    private StackAndClusterUpscaleTriggerEvent fullUpscaleEvent(ClusterRepairTriggerEvent event, Map<String, Set<String>> groupsWithHostNames,
            boolean singlePrimaryGateway, boolean restartServices, boolean kerberosSecured, boolean rollingRestartEnabled) {
        Set<InstanceGroupView> instanceGroupViews = instanceGroupService.findViewByStackId(event.getStackId());
        boolean singleNodeCluster = isSingleNode(instanceGroupViews);
        Integer adjustmentSize = groupsWithHostNames.values().stream().map(Set::size).reduce(0, Integer::sum);
        AdjustmentTypeWithThreshold adjustmentTypeWithThreshold = new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, (long) adjustmentSize);
        Map<String, Integer> hostGroupAdjustments = groupsWithHostNames.entrySet().stream().collect(Collectors.toMap(Entry::getKey, o -> o.getValue().size()));
        LOGGER.info("Full upscale with host groups and adjustments: {}", hostGroupAdjustments);
        LOGGER.info("Full upscale with host groups and host names: {}", groupsWithHostNames);
        return new StackAndClusterUpscaleTriggerEvent(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.getResourceId(),
                hostGroupAdjustments, null, groupsWithHostNames, ScalingType.UPSCALE_TOGETHER, singlePrimaryGateway,
                kerberosSecured, event.accepted(), singleNodeCluster, restartServices, ClusterManagerType.CLOUDERA_MANAGER, adjustmentTypeWithThreshold,
                event.getTriggeredStackVariant(), rollingRestartEnabled).setRepair();
    }

    public boolean isSingleNode(Set<InstanceGroupView> instanceGroupViews) {
        int nodeCount = 0;
        for (InstanceGroupView ig : instanceGroupViews) {
            nodeCount += ig.getNodeCount();
        }
        return nodeCount == 1;
    }

    private boolean isKerberosSecured(StackView stackView) {
        return kerberosConfigService.isKerberosConfigExistsForEnvironment(stackView.getEnvironmentCrn(), stackView.getName());
    }

    private static class RepairConfig {

        private Optional<Repair> singlePrimaryGateway;

        private List<Repair> repairs;

        RepairConfig() {
            singlePrimaryGateway = Optional.empty();
            repairs = new ArrayList<>();
        }

        public Optional<Repair> getSinglePrimaryGateway() {
            return singlePrimaryGateway;
        }

        public void setSinglePrimaryGateway(Repair singlePrimaryGateway) {
            this.singlePrimaryGateway = Optional.of(singlePrimaryGateway);
        }

        public List<Repair> getRepairs() {
            return repairs;
        }

        public void addRepairs(Repair repair) {
            repairs.add(repair);
        }
    }

    private record Repair(String instanceGroupName, String hostGroupName, List<String> hostNames) {
    }
}
