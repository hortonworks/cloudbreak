package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaDetachChildEnvironmentAction implements Action<FreeIpaChildEnvironmentTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDetachChildEnvironmentAction.class);

    @Override
    public FreeIpaChildEnvironmentTestDto action(TestContext testContext, FreeIpaChildEnvironmentTestDto testDto, FreeIpaClient client) throws Exception {
        DetachChildEnvironmentRequest request = convertRequest(testDto.getRequest());
        Log.whenJson(LOGGER, format(" FreeIPA detach child environment:%n"), request);
        client.getDefaultClient(testContext)
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
