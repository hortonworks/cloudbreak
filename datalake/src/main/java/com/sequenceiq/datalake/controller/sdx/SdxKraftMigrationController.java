package com.sequenceiq.datalake.controller.sdx;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.MIGRATE_ZOOKEEPER_TO_KRAFT_DATALAKE;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxKraftMigrationService;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.endpoint.SdxKraftMigrationEndpoint;

@Controller
@AccountEntityType(SdxCluster.class)
public class SdxKraftMigrationController implements SdxKraftMigrationEndpoint {

    @Inject
    private SdxKraftMigrationService sdxKraftMigrationService;

    @Override
    @CheckPermissionByResourceCrn(action = MIGRATE_ZOOKEEPER_TO_KRAFT_DATALAKE)
    public FlowIdentifier migrateFromZookeeperToKraftByCrn(@ResourceCrn String crn) {
        return sdxKraftMigrationService.migrateFromZookeeperToKraft(ThreadBasedUserCrnProvider.getUserCrn(), crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = MIGRATE_ZOOKEEPER_TO_KRAFT_DATALAKE)
    public FlowIdentifier finalizeMigrationFromZookeeperToKraftByCrn(@ResourceCrn String crn) {
        return sdxKraftMigrationService.finalizeMigrationFromZookeeperToKraft(ThreadBasedUserCrnProvider.getUserCrn(), crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = MIGRATE_ZOOKEEPER_TO_KRAFT_DATALAKE)
    public FlowIdentifier rollbackMigrationFromZookeeperToKraftByCrn(@ResourceCrn String crn) {
        return sdxKraftMigrationService.rollbackMigrationFromZookeeperToKraft(ThreadBasedUserCrnProvider.getUserCrn(), crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATALAKE)
    public KraftMigrationStatusResponse zookeeperToKraftMigrationStatusByCrn(@ResourceCrn String crn) {
        return sdxKraftMigrationService.getZookeeperToKraftMigrationStatus(ThreadBasedUserCrnProvider.getUserCrn(), crn);
    }
}
