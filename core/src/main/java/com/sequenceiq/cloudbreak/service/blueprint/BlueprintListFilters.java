package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;

@Component
public class BlueprintListFilters {

    private static final String SHARED_SERVICES_READY = "shared_services_ready";

    private static final String LAKEHOUSE_OPTIMIZER_STACK_NAME = "cloudera_lakehouse_optimizer";

    @Inject
    private SupportedRuntimes supportedRuntimes;

    public boolean isDistroXDisplayed(BlueprintView blueprintView) {
        if (isDatalakeBlueprint(blueprintView)) {
            return true;
        }
        if (ResourceStatus.USER_MANAGED.equals(blueprintView.getStatus())) {
            return true;
        }
        return supportedRuntimes.isSupported(blueprintView.getStackVersion());
    }

    public boolean isDatalakeBlueprint(BlueprintView blueprintView) {
        if (blueprintView == null) {
            return false;
        }
        return isDatalakeBlueprint(blueprintView.getTags());
    }

    public boolean isDatalakeBlueprint(Blueprint blueprint) {
        if (blueprint == null) {
            return false;
        }
        return isDatalakeBlueprint(blueprint.getTags());
    }

    private boolean isDatalakeBlueprint(Json tags) {
        if (tags == null || tags.getMap() == null) {
            return false;
        }
        return Optional.ofNullable((Boolean) tags.getMap().get(SHARED_SERVICES_READY)).orElse(false);
    }

    public boolean isLakehouseOptimizer(BlueprintFile blueprintFile) {
        return LAKEHOUSE_OPTIMIZER_STACK_NAME.equals(blueprintFile.getStackName());
    }
}
