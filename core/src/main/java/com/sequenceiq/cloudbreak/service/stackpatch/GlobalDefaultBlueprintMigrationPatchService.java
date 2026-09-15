package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.GLOBAL_DEFAULT_BLUEPRINT_MIGRATION;
import static com.sequenceiq.cloudbreak.service.blueprint.CrnGeneratorService.GLOBAL_DEFAULT_ACCOUNT;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@Component
public class GlobalDefaultBlueprintMigrationPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalDefaultBlueprintMigrationPatchService.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private ClusterService clusterService;

    @Override
    public StackPatchType getStackPatchType() {
        return GLOBAL_DEFAULT_BLUEPRINT_MIGRATION;
    }

    @Override
    public boolean isAffected(Stack stack) {
        Blueprint blueprint = stack.getBlueprint();
        if (blueprint == null || blueprint.getResourceCrn() == null || blueprint.getStatus() == null || blueprint.getStatus().isNonDefault()) {
            return false;
        }
        return !GLOBAL_DEFAULT_ACCOUNT.equals(Crn.safeFromString(blueprint.getResourceCrn()).getAccountId());
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        Blueprint blueprint = stack.getBlueprint();
        Optional<Blueprint> globalDefaultBlueprint = blueprintService.getGlobalDefaultBlueprintByName(blueprint.getName());
        if (globalDefaultBlueprint.isPresent()) {
            ResourceStatus blueprintStatus = globalDefaultBlueprint.get().getStatus();
            if (blueprintStatus.isDefault()) {
                LOGGER.info("Migrating blueprint '{}' to the global default blueprint for stack.", blueprint.getName());
                clusterService.updateBlueprint(stack.getCluster().getId(), globalDefaultBlueprint.get());
            } else {
                LOGGER.info("Found global blueprint {} but status is {} therefore migration is skipped.", blueprint.getName(), blueprintStatus);
            }
            return true;
        } else {
            LOGGER.debug("Found no global default blueprint with name '{}', skipping migration for stack.", blueprint.getName());
            return false;
        }
    }

    @Override
    protected boolean shouldCheckForFailedRetryableFlow() {
        return false;
    }
}
