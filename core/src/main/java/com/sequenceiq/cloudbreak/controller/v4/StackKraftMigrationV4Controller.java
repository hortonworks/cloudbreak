package com.sequenceiq.cloudbreak.controller.v4;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackKraftMigrationV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.distrox.api.v1.distrox.model.KraftMigrationStatusResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
public class StackKraftMigrationV4Controller implements StackKraftMigrationV4Endpoint {

    @Inject
    private StackOperationService stackOperationService;

    @Override
    @InternalOnly
    public FlowIdentifier migrateFromZookeeperToKraftByCrnInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.triggerZookeeperToKraftMigration(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier finalizeMigrationFromZookeeperToKraftByCrnInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.triggerZookeeperToKraftMigrationFinalization(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier rollbackMigrationFromZookeeperToKraftByCrnInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.triggerZookeeperToKraftMigrationRollback(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public KraftMigrationStatusResponse zookeeperToKraftMigrationStatusByCrnInternal(@ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.getKraftMigrationStatus(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }
}
