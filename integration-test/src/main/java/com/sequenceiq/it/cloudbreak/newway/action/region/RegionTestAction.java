package com.sequenceiq.it.cloudbreak.newway.action.region;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.region.RegionTestDto;

public class RegionTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionTestAction.class);

    private RegionTestAction() {
    }

    public static RegionTestDto getRegions(TestContext testContext, RegionTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining regions by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getRegionsByCredential(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
