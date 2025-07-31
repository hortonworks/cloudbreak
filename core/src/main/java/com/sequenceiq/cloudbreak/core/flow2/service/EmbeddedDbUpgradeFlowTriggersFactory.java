package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.embedded.UpgradeEmbeddedDBPreparationEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb.UpgradeEmbeddedDBPreparationTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class EmbeddedDbUpgradeFlowTriggersFactory {

    public static final String DEFAULT_DB_VERSION = "10";

    private static final Logger LOGGER = getLogger(EmbeddedDbUpgradeFlowTriggersFactory.class);

    @Inject
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    public List<Selectable> createFlowTriggers(Long stackid, boolean upgradeRequested) {
        StackDto stackDto = stackDtoService.getById(stackid);
        return createFlowTriggers(stackDto, upgradeRequested);
    }

    public List<Selectable> createFlowTriggers(StackDto stackDto, boolean upgradeRequested) {
        String targetVersion = databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(stackDto.getStackVersion(), null);
        TargetMajorVersion targetMajorVersion = isEmbeddedDBUpgradeNeeded(stackDto, upgradeRequested, targetVersion);
        if (targetMajorVersion != null) {
            LOGGER.debug("Cluster repair flowchain is extended with upgrade embedded db flows as embedded db upgrade is needed");
            ArrayList<Selectable> flowTriggers = new ArrayList<>(2);
            flowTriggers.add(new UpgradeEmbeddedDBPreparationTriggerRequest(UpgradeEmbeddedDBPreparationEvent.UPGRADE_EMBEDDEDDB_PREPARATION_EVENT.event(),
                    stackDto.getId(), targetMajorVersion));
            flowTriggers.add(new UpgradeRdsTriggerRequest(UpgradeRdsEvent.UPGRADE_RDS_EVENT.event(), stackDto.getId(), targetMajorVersion, null, null));
            return flowTriggers;
        } else {
            LOGGER.debug("Cluster flowchain is not extended with upgrade embedded db upgrade flows");
            return List.of();
        }
    }

    private TargetMajorVersion isEmbeddedDBUpgradeNeeded(StackDto stackDto, boolean upgradeRequested, String targetVersion) {
        boolean embeddedDBOnAttachedDisk = embeddedDatabaseService.isAttachedDiskForEmbeddedDatabaseCreated(stackDto);
        String currentDbVersion = StringUtils.isNotEmpty(stackDto.getExternalDatabaseEngineVersion())
                ? stackDto.getExternalDatabaseEngineVersion() : DEFAULT_DB_VERSION;
        boolean versionsAreDifferent = !targetVersion.equals(currentDbVersion);
        TargetMajorVersion targetMajorVersion = TargetMajorVersion.fromVersion(targetVersion);
        if (upgradeRequested && embeddedDBOnAttachedDisk && versionsAreDifferent && targetMajorVersion != null) {
            LOGGER.debug("Embedded db upgrade is possible and needed.");
            return targetMajorVersion;
        } else {
            LOGGER.debug("Check of embedded db upgrade necessity has evaluated to False. At least one of the following conditions is false:"
                    + ", os upgrade requested: " + upgradeRequested
                    + ", embedded database is on attached disk: " + embeddedDBOnAttachedDisk
                    + ", target db version is different: " + versionsAreDifferent
                    + ", targetMajorVersion is valid: " + (targetMajorVersion != null));
            return null;
        }
    }
}
