package com.sequenceiq.it.cloudbreak.newway.action.v4.info;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.info.responses.CloudbreakInfoResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.info.CloudbreakInfoTestDto;

public class CloudbreakInfoGetAction implements Action<CloudbreakInfoTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakInfoGetAction.class);

    public CloudbreakInfoGetAction() {
    }

    @Override
    public CloudbreakInfoTestDto action(TestContext testContext, CloudbreakInfoTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Get Info: {}", testDto.getRequest());
        try {
            CloudbreakInfoResponse info = cloudbreakClient.getCloudbreakClient().cloudbreakInfoV4Endpoint().info();
            testDto.setResponse(info);
            logJSON(LOGGER, "info has been fetched successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.warn("Cannot get info : {}", testDto.getRequest());
            throw e;
        }
        return testDto;
    }
}
