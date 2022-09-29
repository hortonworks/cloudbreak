package com.sequenceiq.redbeams.service.stack;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.MajorVersionComparator;
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

        MajorVersion currentVersion = dbStack.getMajorVersion();
        TargetMajorVersion targetVersion = upgradeDatabaseRequest.getTargetMajorVersion();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Upgrade called for: {}, with target version: {}, current version is: {}", dbStack, targetVersion, currentVersion);
        }
        if (dbStack.getStatus().isUpgradeInProgress()) {
            LOGGER.debug(String.format("DatabaseServer with crn %s is already being upgraded", crn));
            return;
        } else if (!isUpgradeNeeded(currentVersion, targetVersion)) {
            LOGGER.debug(String.format("DatabaseServer with crn %s is already on version %s, upgrade is not necessary.",
                    crn, currentVersion.getVersion()));
            return;
        }

        dbStackStatusUpdater.updateStatus(dbStack.getId(), DetailedDBStackStatus.UPGRADE_REQUESTED);
        RedbeamsStartUpgradeRequest redbeamsStartUpgradeRequest = new RedbeamsStartUpgradeRequest(dbStack.getId(),
                upgradeDatabaseRequest.getTargetMajorVersion());
        flowManager.notify(RedbeamsUpgradeEvent.REDBEAMS_START_UPGRADE_EVENT.selector(), redbeamsStartUpgradeRequest);
    }

    private boolean isUpgradeNeeded(MajorVersion currentVersion, TargetMajorVersion targetMajorVersion) {
        MajorVersionComparator majorVersionComparator = new MajorVersionComparator();
        boolean upgradeNeeded = majorVersionComparator.compare(currentVersion, targetMajorVersion.convertToMajorVersion()) < 0;
        LOGGER.debug("Comparing current and target versions. Current version is {}, and target version is {}, isUpgradeNeeded: {}", currentVersion,
                targetMajorVersion, upgradeNeeded);
        return upgradeNeeded;
    }

}
