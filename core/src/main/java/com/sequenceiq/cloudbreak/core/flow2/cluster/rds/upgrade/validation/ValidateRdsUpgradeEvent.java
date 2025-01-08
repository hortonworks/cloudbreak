package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.validation;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeBackupValidationResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradePushSaltStatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeOnCloudProviderResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum ValidateRdsUpgradeEvent implements FlowEvent {

    VALIDATE_RDS_UPGRADE_EVENT("VALIDATE_RDS_UPGRADE_TRIGGER_EVENT"),
    VALIDATE_RDS_UPGRADE_PUSH_SALT_STATES_FINISHED_EVENT(EventSelectorUtil.selector(ValidateRdsUpgradePushSaltStatesResult.class)),
    VALIDATE_RDS_UPGRADE_BACKUP_VALIDATION_FINISHED_EVENT(EventSelectorUtil.selector(ValidateRdsUpgradeBackupValidationResult.class)),
    VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT(EventSelectorUtil.selector(ValidateRdsUpgradeOnCloudProviderResult.class)),
    WAIT_FOR_VALIDATE_RDS_UPGRADE_ON_CLOUDPROVIDER_FINISHED_EVENT(EventSelectorUtil.selector(WaitForValidateRdsUpgradeOnCloudProviderResult.class)),
    VALIDATE_RDS_UPGRADE_CONNECTION_FINISHED_EVENT(EventSelectorUtil.selector(ValidateRdsUpgradeConnectionResult.class)),
    VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT(EventSelectorUtil.selector(ValidateRdsUpgradeCleanupResult.class)),
    WAIT_FOR_VALIDATE_RDS_UPGRADE_CLEANUP_FINISHED_EVENT(EventSelectorUtil.selector(WaitForValidateRdsUpgradeCleanupResult.class)),
    VALIDATE_RDS_UPGRADE_FAILED_EVENT(EventSelectorUtil.selector(ValidateRdsUpgradeFailedEvent.class)),
    FINALIZED_EVENT("VALIDATE_RDS_UPGRADE_FINALIZED_EVENT"),
    FAIL_HANDLED_EVENT("VALIDATE_RDS_UPGRADE_FAIL_HANDLED_EVENT");

    private final String event;

    ValidateRdsUpgradeEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}