package com.sequenceiq.datalake.flow.upgrade.event;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOption;
import com.sequenceiq.datalake.flow.SdxEvent;

public class SdxUpgradeStartEvent extends SdxEvent {

    private UpgradeOption upgradeOption;

    public SdxUpgradeStartEvent(String selector, Long sdxId, String userId, String requestId, UpgradeOption upgradeOption) {
        super(selector, sdxId, userId, requestId);
        this.upgradeOption = upgradeOption;
    }

    public UpgradeOption getUpgradeOption() {
        return upgradeOption;
    }

    @Override
    public String selector() {
        return "SdxUpgradeStartEvent";
    }
}
