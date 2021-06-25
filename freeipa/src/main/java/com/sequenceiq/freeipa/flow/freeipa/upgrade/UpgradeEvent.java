package com.sequenceiq.freeipa.flow.freeipa.upgrade;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class UpgradeEvent extends StackEvent {

    private final Set<String> instanceIds;

    private final String primareGwInstanceId;

    private final String operationId;

    private final ImageSettingsRequest imageSettingsRequest;

    public UpgradeEvent(Long stackId, Set<String> instanceIds, String primareGwInstanceId, String operationId, ImageSettingsRequest imageSettingsRequest) {
        super(stackId);
        this.instanceIds = instanceIds;
        this.primareGwInstanceId = primareGwInstanceId;
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
    }

    public UpgradeEvent(String selector, Long stackId, Set<String> instanceIds, String primareGwInstanceId, String operationId,
            ImageSettingsRequest imageSettingsRequest) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
        this.primareGwInstanceId = primareGwInstanceId;
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
    }

    public UpgradeEvent(String selector, Long stackId, Promise<AcceptResult> accepted, Set<String> instanceIds, String primareGwInstanceId, String operationId,
            ImageSettingsRequest imageSettingsRequest) {
        super(selector, stackId, accepted);
        this.instanceIds = instanceIds;
        this.primareGwInstanceId = primareGwInstanceId;
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
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
}
