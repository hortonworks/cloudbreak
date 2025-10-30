package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.MIGRATE_ZOOKEEPER_TO_KRAFT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftConfigurationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftFinalizationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftMigrationFlowConfig;
import com.sequenceiq.cloudbreak.core.flow2.cluster.migration.kraft.config.MigrateZookeeperToKraftRollbackFlowConfig;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXKraftMigrationV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.service.FlowService;

@Controller
public class DistroXKraftMigrationV1Controller implements DistroXKraftMigrationV1Endpoint {

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private FlowService flowService;

    @Override
    @CheckPermissionByResourceCrn(action = MIGRATE_ZOOKEEPER_TO_KRAFT)
    public FlowIdentifier migrateFromZookeeperToKraftByCrn(@ResourceCrn String crn) {
        return stackOperationService.triggerZookeeperToKraftMigration(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = FINALIZE_ZOOKEEPER_TO_KRAFT_MIGRATION)
    public FlowIdentifier finalizeMigrationFromZookeeperToKraftByCrn(@ResourceCrn String crn) {
        return stackOperationService.triggerZookeeperToKraftMigrationFinalization(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = ROLLBACK_ZOOKEEPER_TO_KRAFT_MIGRATION)
    public FlowIdentifier rollbackMigrationFromZookeeperToKraftByCrn(@ResourceCrn String crn) {
        return stackOperationService.triggerZookeeperToKraftMigrationRollback(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public KraftMigrationStatusResponse zookeeperToKraftMigrationStatusByCrn(@ResourceCrn String crn) {
        List<FlowLogResponse> kraftFlowLogResponseList = flowService.getAllFlowLogsByResourceCrnAndFlowTypes(crn,
                List.of(ClassValue.of(MigrateZookeeperToKraftConfigurationFlowConfig.class),
                        ClassValue.of(MigrateZookeeperToKraftMigrationFlowConfig.class),
                        ClassValue.of(MigrateZookeeperToKraftFinalizationFlowConfig.class),
                        ClassValue.of(MigrateZookeeperToKraftRollbackFlowConfig.class)));
        return stackOperationService.getKraftMigrationStatus(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), kraftFlowLogResponseList);
    }
}
