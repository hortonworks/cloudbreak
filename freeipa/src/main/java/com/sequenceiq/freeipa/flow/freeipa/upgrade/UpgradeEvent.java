package com.sequenceiq.freeipa.flow.freeipa.upgrade;

import java.util.Set;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeEvent extends StackEvent {

    private final Set<String> instanceIds;

    private final String primareGwInstanceId;

    private final String operationId;

    private final ImageSettingsRequest imageSettingsRequest;

    private final boolean backupSet;

    public UpgradeEvent(String selector, Long stackId, Set<String> instanceIds, String primareGwInstanceId, String operationId,
            ImageSettingsRequest imageSettingsRequest, boolean backupSet) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
        this.primareGwInstanceId = primareGwInstanceId;
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
        this.backupSet = backupSet;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public String getPrimareGwInstanceId() {
        return primareGwInstanceId;
    }

    public String getOperationId() {
        return operationId;
    }

    public ImageSettingsRequest getImageSettingsRequest() {
        return imageSettingsRequest;
    }

    public boolean isBackupSet() {
        return backupSet;
    }
}
