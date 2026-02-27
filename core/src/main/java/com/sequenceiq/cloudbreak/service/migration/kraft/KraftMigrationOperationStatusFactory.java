package com.sequenceiq.cloudbreak.service.migration.kraft;

import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.NOT_APPLICABLE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
import static com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus.ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.status.KraftMigrationStatus;
import com.sequenceiq.cloudbreak.core.flow2.chain.MigrateZookeeperToKraftFlowEventChainFactory;
import com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftConfigurationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftFinalizationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftMigrationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftRollbackFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.downscale.StackDownscaleConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.sync.StackSyncFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.kraft.KraftMigrationOperationStatus;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowChainLog;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Component
public class KraftMigrationOperationStatusFactory {
    private static final List<ClassValue> RELATED_FLOW_CONFIGS = List.of(
            ClassValue.of(MigrateZookeeperToKraftFinalizationFlowConfig.class),
            ClassValue.of(MigrateZookeeperToKraftRollbackFlowConfig.class));

    private static final List<ClassValue> UPSCALE_RELATED_FLOW_CONFIGS = List.of(
            ClassValue.of(StackSyncFlowConfig.class),
            ClassValue.of(StackUpscaleConfig.class),
            ClassValue.of(ClusterUpscaleFlowConfig.class),
            ClassValue.of(ClusterDownscaleFlowConfig.class),
            ClassValue.of(StackDownscaleConfig.class));

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(KraftMigrationOperationStatusFactory.class);

    private static final int NUMBER_OF_LOGS_TO_CHECK_FOR_FAILURE = 3;

    @Inject
    private FlowLogDBService flowLogDBService;

    public Optional<KraftMigrationOperationStatus> getStatusFromFlowInformation(StackDto stack) {
        Optional<KraftMigrationOperationStatus> migrationStatus = Optional.empty();
        List<FlowLog> lastKraftFlowLogs = getMostRecentKraftFlowLastLogs(stack);
        Optional<FlowLog> lastKraftFlowLog = lastKraftFlowLogs.stream().max(Comparator.comparingLong(FlowLog::getCreated));
        if (CollectionUtils.isNotEmpty(lastKraftFlowLogs) && lastKraftFlowLog.isPresent()) {
            FlowLog lastKnownFlowLog = lastKraftFlowLog.get();
            LOGGER.info("The last flow has been found for Kraft migration calculating status based on that: {}", lastKnownFlowLog);
            migrationStatus = Optional.of(getStatusFromFlowInformation(lastKnownFlowLog, lastKraftFlowLogs));
        }
        return migrationStatus;
    }

    private List<FlowLog> getMostRecentKraftFlowLastLogs(StackDto stackDto) {
        Long stackId = stackDto.getId();
        List<FlowLog> kraftFlowLogs = new ArrayList<>(flowLogDBService.findAllByResourceIdAndFlowTypeInOrderByCreatedDesc(stackId, RELATED_FLOW_CONFIGS));
        if (CollectionUtils.isEmpty(kraftFlowLogs)) {
            List<FlowLog> latestFlowLogsByCrnInFlowChain = flowLogDBService.getLatestFlowLogsByCrnInFlowChain(stackDto.getResourceCrn());
            boolean kraftMigrationFlowChain = latestFlowLogsByCrnInFlowChain.stream()
                    .findFirst()
                    .map(FlowLog::getFlowChainId)
                    .flatMap(flowChainId -> flowLogDBService.findFirstByFlowChainIdOrderByCreatedDesc(flowChainId))
                    .map(FlowChainLog::getFlowChainType)
                    .filter(flowChainType -> flowChainType.contains(MigrateZookeeperToKraftFlowEventChainFactory.class.getSimpleName()))
                    .isPresent();

            if (kraftMigrationFlowChain) {
                LOGGER.debug("The Kraft migration flow chain has been found, adding the relevant '{}' flow logs", latestFlowLogsByCrnInFlowChain.size());
                kraftFlowLogs.addAll(latestFlowLogsByCrnInFlowChain);
            }
        }

        return kraftFlowLogs.stream()
                .sorted(Comparator.comparingLong(FlowLog::getCreated).reversed())
                .limit(NUMBER_OF_LOGS_TO_CHECK_FOR_FAILURE)
                .toList();
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private KraftMigrationOperationStatus getStatusFromFlowInformation(FlowLog lastKnownFlowLog, List<FlowLog> mostRecentKraftFlowLogs) {
        Class<?> flowType = Optional.ofNullable(lastKnownFlowLog.getFlowType()).map(ClassValue::getClassValue).orElse(null);
        boolean lastFlowLogFinalized = Optional.ofNullable(lastKnownFlowLog.getFinalized()).orElse(false);
        boolean lastFlowFailed = mostRecentKraftFlowLogs.stream().anyMatch(log -> StateStatus.FAILED.equals(log.getStateStatus()));
        return switch (flowType) {
            case null -> NOT_APPLICABLE;

            case Class<?> c when c ==
                    MigrateZookeeperToKraftConfigurationFlowConfig.class && lastFlowFailed -> ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftConfigurationFlowConfig.class && lastFlowLogFinalized -> ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftConfigurationFlowConfig.class -> ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;

            case Class<?> c when UPSCALE_RELATED_FLOW_CONFIGS.contains(ClassValue.of(c)) && lastFlowFailed -> ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
            case Class<?> c when UPSCALE_RELATED_FLOW_CONFIGS.contains(ClassValue.of(c)) && lastFlowLogFinalized -> ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
            case Class<?> c when UPSCALE_RELATED_FLOW_CONFIGS.contains(ClassValue.of(c)) -> INSTALLING_KRAFT_SERVICE_FOR_MIGRATION_IN_PROGRESS;

            case Class<?> c when c ==
                    MigrateZookeeperToKraftMigrationFlowConfig.class && lastFlowFailed -> ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftMigrationFlowConfig.class && lastFlowLogFinalized -> ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftMigrationFlowConfig.class -> ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;

            case Class<?> c when c ==
                    MigrateZookeeperToKraftFinalizationFlowConfig.class && lastFlowFailed -> FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftFinalizationFlowConfig.class && lastFlowLogFinalized -> FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftFinalizationFlowConfig.class -> FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;

            case Class<?> c when c ==
                    MigrateZookeeperToKraftRollbackFlowConfig.class && lastFlowFailed -> ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_FAILED;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftRollbackFlowConfig.class && lastFlowLogFinalized -> ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            case Class<?> c when c ==
                    MigrateZookeeperToKraftRollbackFlowConfig.class -> ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;

            default -> NOT_APPLICABLE;
        };
    }

    public KraftMigrationOperationStatus getStatusFromClusterKRaftMigrationStatus(KraftMigrationStatus kraftMigrationStatus) {
        return switch (kraftMigrationStatus) {
            case ZOOKEEPER_INSTALLED -> ZOOKEEPER_TO_KRAFT_MIGRATION_TRIGGERABLE;
            case PRE_MIGRATION, BROKERS_IN_MIGRATION -> ZOOKEEPER_TO_KRAFT_MIGRATION_IN_PROGRESS;
            case KRAFT_INSTALLED -> FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            case BROKERS_IN_KRAFT -> ZOOKEEPER_TO_KRAFT_MIGRATION_COMPLETE;
            default -> NOT_APPLICABLE;
        };
    }
}
