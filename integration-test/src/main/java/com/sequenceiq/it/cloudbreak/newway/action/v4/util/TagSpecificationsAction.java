package com.sequenceiq.it.cloudbreak.newway.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.util.TagSpecificationsTestDto;

public class TagSpecificationsAction implements Action<TagSpecificationsTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagSpecificationsAction.class);

    @Override
    public TagSpecificationsTestDto action(TestContext testContext, TagSpecificationsTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        String logInitMessage = "Obtaining tag specifications";
        LOGGER.info("{}", logInitMessage);
        entity.setResponse(cloudbreakClient.getCloudbreakClient().connectorV4Endpoint().getTagSpecifications(cloudbreakClient.getWorkspaceId()));
        LOGGER.info("{} was successful", logInitMessage);
        return entity;
    }
}
