package com.sequenceiq.redbeams.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.upgrade.UpgradeDatabaseRequest;
import com.sequenceiq.redbeams.flow.RedbeamsFlowManager;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;

@Service
public class RedbeamsUpgradeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsUpgradeService.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsFlowManager flowManager;

    public void upgradeDatabaseServer(String crn, UpgradeDatabaseRequest upgradeDatabaseRequest) {
        DBStack dbStack = dbStackService.getByCrn(crn);
        MDCBuilder.addEnvironmentCrn(dbStack.getEnvironmentId());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Upgrade called for: {}, with target version: {}", dbStack, upgradeDatabaseRequest.getTargetMajorVersion());
        }

        if (dbStack.getStatus().isUpgradeInProgress()) {
            String message = String.format("DatabaseServer with crn %s is already being upgraded", dbStack.getResourceCrn());
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }

        dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.UPGRADE_REQUESTED);
        RedbeamsStartUpgradeRequest redbeamsStartUpgradeRequest = new RedbeamsStartUpgradeRequest(dbStack.getId(),
                upgradeDatabaseRequest.getTargetMajorVersion());
        flowManager.notify(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), redbeamsStartUpgradeRequest);
    }

}
