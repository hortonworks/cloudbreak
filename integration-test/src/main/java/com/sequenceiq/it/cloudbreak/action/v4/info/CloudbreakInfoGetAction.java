package com.sequenceiq.it.cloudbreak.action.v4.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.info.responses.CloudbreakInfoResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.info.CloudbreakInfoTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class CloudbreakInfoGetAction implements Action<CloudbreakInfoTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakInfoGetAction.class);

    public CloudbreakInfoGetAction() {
    }

    @Override
    public CloudbreakInfoTestDto action(TestContext testContext, CloudbreakInfoTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Get Info: {}", testDto.getRequest());
        try {
            CloudbreakInfoResponse info = cloudbreakClient.getCloudbreakClient().cloudbreakInfoV4Endpoint().info();
            testDto.setResponse(info);
            Log.whenJson(LOGGER, "info has been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get info : {}", testDto.getRequest());
            throw e;
        }
        return testDto;
    }
}
