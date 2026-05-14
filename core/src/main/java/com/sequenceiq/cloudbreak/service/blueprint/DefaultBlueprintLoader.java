package com.sequenceiq.cloudbreak.service.blueprint;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.service.LockNumber;
import com.sequenceiq.cloudbreak.common.service.LockService;
import com.sequenceiq.cloudbreak.util.Benchmark;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@Component
public class DefaultBlueprintLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintLoader.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private LockService lockService;

    @Value("${cb.blueprint.global.migration.enabled}")
    private boolean globalDefaultBlueprintMigrationEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (globalDefaultBlueprintMigrationEnabled) {
            LOGGER.info("Trying to acquire lock for blueprint migration.");
            lockService.lockAndRunIfLockWasSuccessful(this::migrateDefaultBlueprints, LockNumber.BLUEPRINT);
        }
    }

    private void migrateDefaultBlueprints() {
        try {
            LOGGER.info("Default blueprint migration is started.");
            Benchmark.measure(() -> {
                blueprintService.updateDefaultBlueprintCollection((Workspace) null);
            }, LOGGER, "Default blueprint migration completed under {} ms.");
        } catch (Exception e) {
            LOGGER.error("Error during default blueprint migration.", e);
        }
    }
}