package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXInternalGetAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXInternalGetAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        if (testDto.getResponse() == null) {
            throw new IllegalArgumentException("DistroX get action with internal actor requires a DistroX Stack response first.");
        }
        String distroXCrn = testDto.getResponse().getCrn();
        Log.when(LOGGER, " Internal actor CRN used for distrox get request: " + distroXCrn);
        testDto.withInternalStackResponse(
                client.getCloudbreakInternalCrnClient()
                        .withInternalCrn()
                        .distroXInternalV1Endpoint()
                        .getByCrn(distroXCrn));
        Log.whenJson(LOGGER, "Internal actor CRN used for distrox, response:", testDto.getInternalStackResponse());

        return testDto;
    }

}
