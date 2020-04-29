package com.sequenceiq.datalake.controller.sdx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

@Service
public class SdxUpgradeClusterConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxUpgradeClusterConverter.class);

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
        return upgradeV4Request;
    }
}
