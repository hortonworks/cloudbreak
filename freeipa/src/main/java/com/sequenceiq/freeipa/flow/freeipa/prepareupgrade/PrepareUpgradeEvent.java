package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.validation.SecurityGroupValidationResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageFallbackRequiredResult;
import com.sequenceiq.cloudbreak.cloud.event.setup.PrepareImageResult;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureCleanupComplete;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbDeletionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeLbProvisionSuccess;
import com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event.PrepareUpgradeMetadataCollectionSuccess;
import com.sequenceiq.freeipa.flow.stack.provision.event.imagefallback.ImageFallbackSuccess;

public enum PrepareUpgradeEvent implements FlowEvent {
    PREPARE_UPGRADE_EVENT,
    PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINISHED_EVENT(CloudPlatformResult.selector(SecurityGroupValidationResult.class)),
    PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FAILED_EVENT(CloudPlatformResult.failureSelector(SecurityGroupValidationResult.class)),
    PREPARE_UPGRADE_SECURITY_GROUP_VALIDATION_FINALIZED_EVENT,
    PREPARE_UPGRADE_IMAGE_PREPARATION_FINISHED_EVENT(CloudPlatformResult.selector(PrepareImageResult.class)),
    PREPARE_UPGRADE_IMAGE_PREPARATION_FAILED_EVENT(CloudPlatformResult.failureSelector(PrepareImageResult.class)),
    PREPARE_UPGRADE_UPDATE_IMAGE_PARAMETER_FINISHED_EVENT,
    PREPARE_UPGRADE_IMAGE_FALLBACK_EVENT(CloudPlatformResult.selector(PrepareImageFallbackRequiredResult.class)),
    PREPARE_UPGRADE_IMAGE_FALLBACK_FINISHED_EVENT(EventSelectorUtil.selector(ImageFallbackSuccess.class)),
    PREPARE_UPGRADE_IMAGE_COPY_CHECK_EVENT,
    PREPARE_UPGRADE_IMAGE_COPY_FINISHED_EVENT,
    PREPARE_UPGRADE_LB_CONFIGURATION_FINISHED_EVENT,
    PREPARE_UPGRADE_LB_PROVISIONED_EVENT(EventSelectorUtil.selector(PrepareUpgradeLbProvisionSuccess.class)),
    PREPARE_UPGRADE_METADATA_COLLECTED_EVENT(EventSelectorUtil.selector(PrepareUpgradeMetadataCollectionSuccess.class)),
    PREPARE_UPGRADE_LB_DELETED_EVENT(EventSelectorUtil.selector(PrepareUpgradeLbDeletionSuccess.class)),
    PREPARE_UPGRADE_LB_DB_CLEANUP_FINISHED_EVENT,
    PREPARE_UPGRADE_FINISHED_EVENT,
    PREPARE_UPGRADE_FINALIZED_EVENT,
    PREPARE_UPGRADE_FAILURE_CLEANUP_FINISHED_EVENT(EventSelectorUtil.selector(PrepareUpgradeFailureCleanupComplete.class)),
    PREPARE_UPGRADE_FAILURE_EVENT(EventSelectorUtil.selector(PrepareUpgradeFailureEvent.class)),
    PREPARE_UPGRADE_FAILURE_HANDLED_EVENT;

    private final String event;

    PrepareUpgradeEvent(String event) {
        this.event = event;
    }

    PrepareUpgradeEvent() {
        this.event = name();
    }

    @Override
    public String event() {
        return event;
    }

    @Override
    public String selector() {
        return event();
    }
}
