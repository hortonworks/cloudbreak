package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRotateSaltPasswordAction implements Action<DistroXTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRotateSaltPasswordAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.whenJson(LOGGER, format(" DistroX rotate salt password request for crn %n"), testDto.getCrn());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).distroXV1Endpoint().rotateSaltPasswordByCrn(testDto.getCrn());
        testDto.setFlow("DistroX rotate salt password",  flowIdentifier);
        Log.whenJson(LOGGER, format(" DistroX rotate salt password started: %n"), testDto.getCrn());
        return testDto;
    }
}
