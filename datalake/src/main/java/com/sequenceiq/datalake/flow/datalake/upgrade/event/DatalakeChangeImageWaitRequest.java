package com.sequenceiq.datalake.flow.datalake.upgrade.event;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;

public class DatalakeChangeImageWaitRequest extends SdxEvent {

    private final UpgradeOptionV4Response upgradeOption;

    public DatalakeChangeImageWaitRequest(Long sdxId, String userId, UpgradeOptionV4Response upgradeOption) {
        super(sdxId, userId);
        this.upgradeOption = upgradeOption;
    }

    public DatalakeChangeImageWaitRequest(String selector, Long sdxId, String userId, UpgradeOptionV4Response upgradeOption) {
        super(selector, sdxId, userId);
        this.upgradeOption = upgradeOption;
    }

    public DatalakeChangeImageWaitRequest(SdxContext context, UpgradeOptionV4Response upgradeOption) {
        super(context);
        this.upgradeOption = upgradeOption;
    }

    public UpgradeOptionV4Response getUpgradeOption() {
        return upgradeOption;
    }
}
