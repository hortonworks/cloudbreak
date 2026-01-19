package com.sequenceiq.it.cloudbreak.action.v4.customconfigs;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.customconfigs.CustomConfigurationsTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CustomConfigurationsGetAction implements Action<CustomConfigurationsTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomConfigurationsGetAction.class);

    @Override
    public CustomConfigurationsTestDto action(TestContext testContext, CustomConfigurationsTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format("CustomConfigurations get request: %n"), testDto.getRequest());
        testDto.setResponse(client.getDefaultClient(testContext).customConfigurationsV4Endpoint().getByName(testDto.getName()));
        Log.whenJson(LOGGER, format("CustomConfigurations retrieved successfully: %n"), testDto.getResponse());
        return testDto;
    }
}
