package com.sequenceiq.it.cloudbreak.newway.action.tagspecifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.tagspecifications.TagSpecificationsTestDto;

public class TagSpecificationsTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagSpecificationsTestAction.class);

    private TagSpecificationsTestAction() {
    }

    public static TagSpecificationsTestDto getTagSpecifications(TestContext testContext, TagSpecificationsTestDto entity, CloudbreakClient client) {
        String logInitMessage = "Obtaining tag specifications";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(client.getCloudbreakClient().connectorV4Endpoint().getTagSpecifications(client.getWorkspaceId()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }

}
