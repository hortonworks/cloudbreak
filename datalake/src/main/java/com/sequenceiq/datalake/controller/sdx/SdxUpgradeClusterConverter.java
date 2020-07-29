package com.sequenceiq.datalake.controller.sdx;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.common.model.UpgradeShowAvailableImages;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Service
public class SdxUpgradeClusterConverter {

    public SdxUpgradeResponse upgradeResponseToSdxUpgradeResponse(UpgradeV4Response upgradeV4Response) {
        return new SdxUpgradeResponse(
                upgradeV4Response.getCurrent(),
                upgradeV4Response.getUpgradeCandidates(),
                upgradeV4Response.getReason(),
                upgradeV4Response.getFlowIdentifier());
    }

    public UpgradeV4Request sdxUpgradeRequestToUpgradeV4Request(SdxUpgradeRequest sdxUpgradeRequest) {
        UpgradeV4Request upgradeV4Request = new UpgradeV4Request();
        upgradeV4Request.setImageId(sdxUpgradeRequest.getImageId());
        upgradeV4Request.setRuntime(sdxUpgradeRequest.getRuntime());
        upgradeV4Request.setDryRun(sdxUpgradeRequest.isDryRun());
        upgradeV4Request.setLockComponents(sdxUpgradeRequest.getLockComponents());
        if (Objects.nonNull(sdxUpgradeRequest.getShowAvailableImages())) {
            upgradeV4Request.setShowAvailableImages(UpgradeShowAvailableImages.valueOf(sdxUpgradeRequest.getShowAvailableImages().name()));
        }
        return upgradeV4Request;
    }
}
