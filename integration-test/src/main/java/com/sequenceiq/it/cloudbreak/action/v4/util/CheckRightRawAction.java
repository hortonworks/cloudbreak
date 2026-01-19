package com.sequenceiq.it.cloudbreak.action.v4.util;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.RawCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CheckRightRawAction implements Action<RawCloudbreakTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRightRawAction.class);

    @Override
    public RawCloudbreakTestDto action(TestContext testContext, RawCloudbreakTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        Response response = cloudbreakClient.getRawClient(testContext).path("v4/utils/check_right").request()
                .post(Entity.entity(testDto.getRequestJson(), MediaType.APPLICATION_JSON_TYPE));
        testDto.setResponse(response.readEntity(String.class));
        return testDto;
    }
}
