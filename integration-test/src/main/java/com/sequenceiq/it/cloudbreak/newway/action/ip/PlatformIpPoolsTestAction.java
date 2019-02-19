package com.sequenceiq.it.cloudbreak.newway.action.ip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ip.PlatformIpPoolsTestDto;

public class PlatformIpPoolsTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformIpPoolsTestAction.class);

    private PlatformIpPoolsTestAction() {
    }

    public static PlatformIpPoolsTestDto getIpPools(TestContext testContext, PlatformIpPoolsTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining ip pools by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getIpPoolsCredentialId(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
