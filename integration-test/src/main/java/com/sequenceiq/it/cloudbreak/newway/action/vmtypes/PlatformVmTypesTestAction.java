package com.sequenceiq.it.cloudbreak.newway.action.vmtypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.vmtypes.PlatformVmTypesTestDto;

public class PlatformVmTypesTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVmTypesTestAction.class);

    private PlatformVmTypesTestAction() {
    }

    public static PlatformVmTypesTestDto getPlatformVmtypes(TestContext testContext, PlatformVmTypesTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining vm types by credential";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(
                client.getCloudbreakClient().connectorV4Endpoint().getVmTypesByCredential(
                        client.getWorkspaceId(), entity.getCredentialName(), entity.getRegion(), entity.getPlatformVariant(), entity.getAvailabilityZone())
        );
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
