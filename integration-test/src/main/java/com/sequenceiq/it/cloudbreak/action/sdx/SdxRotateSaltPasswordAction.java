package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxRotateSaltPasswordAction implements Action<SdxInternalTestDto, SdxClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRotateSaltPasswordAction.class);

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, format(" SDX rotate salt password request for crn %n"), testDto.getCrn());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().rotateSaltPasswordByCrn(testDto.getCrn());
        testDto.setFlow("SDX rotate salt password",  flowIdentifier);
        Log.whenJson(LOGGER, format(" SDX rotate salt password started: %n"), testDto.getCrn());
        return testDto;
    }
}
