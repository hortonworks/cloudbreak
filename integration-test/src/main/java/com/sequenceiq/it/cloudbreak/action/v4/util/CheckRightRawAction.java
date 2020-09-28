package com.sequenceiq.it.cloudbreak.action.v4.util;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.RawCloudbreakTestDto;

public class CheckRightRawAction implements Action<RawCloudbreakTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRightRawAction.class);

    @Override
    public RawCloudbreakTestDto action(TestContext testContext, RawCloudbreakTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Response response = cloudbreakClient.getRawClient().path("v4/utils/check_right").request()
                .post(Entity.entity(testDto.getRequestJson(), MediaType.APPLICATION_JSON_TYPE));
        testDto.setResponse(response.readEntity(String.class));
        return testDto;
    }
}
