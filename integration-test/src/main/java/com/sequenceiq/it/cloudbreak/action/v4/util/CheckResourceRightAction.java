package com.sequenceiq.it.cloudbreak.action.v4.util;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.ResourceRightsV4;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class CheckResourceRightAction implements Action<CheckResourceRightTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckResourceRightAction.class);

    @Override
    public CheckResourceRightTestDto action(TestContext testContext, CheckResourceRightTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        CheckResourceRightsV4Request checkRightByCrnV4Request = new CheckResourceRightsV4Request();
        checkRightByCrnV4Request.setResourceRights(testDto.getRightsToCheck().entrySet().stream().map(entry -> {
            ResourceRightsV4 resourceRightsV4 = new ResourceRightsV4();
            resourceRightsV4.setResourceCrn(entry.getKey());
            resourceRightsV4.setRights(entry.getValue());
            return resourceRightsV4;
        }).collect(Collectors.toList()));
        testDto.setResponse(cloudbreakClient.getDefaultClient(testContext).authorizationUtilEndpoint().checkRightByCrn(checkRightByCrnV4Request));
        Log.whenJson(LOGGER, "checking right on resources response:\n", testDto.getResponse());
        return testDto;
    }
}
