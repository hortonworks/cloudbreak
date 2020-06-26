package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPAChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIPADetachChildEnvironmentAction implements Action<FreeIPAChildEnvironmentTestDto, FreeIPAClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIPADetachChildEnvironmentAction.class);

    @Override
    public FreeIPAChildEnvironmentTestDto action(TestContext testContext, FreeIPAChildEnvironmentTestDto testDto, FreeIPAClient client) throws Exception {
        DetachChildEnvironmentRequest request = convertRequest(testDto.getRequest());
        Log.whenJson(LOGGER, format(" FreeIPA detach child environment:%n"), request);
        client.getFreeIpaClient()
                .getFreeIpaV1Endpoint()
                .detachChildEnvironment(request);
        Log.when(LOGGER, " FreeIPA detached child environment successfully.");
        return testDto;
    }

    private DetachChildEnvironmentRequest convertRequest(AttachChildEnvironmentRequest attachChildEnvironmentRequest) {
        DetachChildEnvironmentRequest detachChildEnvironmentRequest = new DetachChildEnvironmentRequest();
        detachChildEnvironmentRequest.setParentEnvironmentCrn(attachChildEnvironmentRequest.getParentEnvironmentCrn());
        detachChildEnvironmentRequest.setChildEnvironmentCrn(attachChildEnvironmentRequest.getChildEnvironmentCrn());
        return detachChildEnvironmentRequest;
    }
}
