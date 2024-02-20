package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsInstallPostgresPackagesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateVersionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum UpgradeRdsEvent implements FlowEvent {
    UPGRADE_RDS_EVENT("UPGRADE_RDS_TRIGGER_EVENT"),
    UPGRADE_RDS_STOP_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsStopServicesResult.class)),
    UPGRADE_RDS_DATA_BACKUP_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsDataBackupResult.class)),
    UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsUpgradeDatabaseServerResult.class)),
    UPGRADE_RDS_MIGRATE_DATABASE_SETTINGS_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsMigrateDatabaseSettingsResponse.class)),
    UPGRADE_RDS_DATA_RESTORE_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsDataRestoreResult.class)),
    // TODO This is for backward compatibility reason, can be removed in CB-24447
    UPGRADE_RDS_START_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsStartServicesResult.class)),
    UPGRADE_RDS_START_CM_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsStartCMResult.class)),
    UPGRADE_RDS_MIGRATE_SERVICES_DB_SETTINGS_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsMigrateServicesDBSettingsResponse.class)),
    UPGRADE_RDS_START_CMSERVICES_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsStartCMServicesResult.class)),

    UPGRADE_RDS_INSTALL_POSTGRES_PACKAGES_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsInstallPostgresPackagesResult.class)),
    UPGRADE_RDS_MIGRATE_ATTACHED_DATAHUBS_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsMigrateAttachedDatahubsResponse.class)),
    UPGRADE_RDS_VERSION_UPDATE_FINISHED_EVENT(EventSelectorUtil.selector(UpgradeRdsUpdateVersionResult.class)),
    UPGRADE_RDS_FAILED_EVENT(EventSelectorUtil.selector(UpgradeRdsFailedEvent.class)),

    FINALIZED_EVENT("UPGRADE_RDS_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("UPGRADE_RDS_FAIL_HANDLED_EVENT");

    private final String event;

    UpgradeRdsEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
