package com.sequenceiq.it.cloudbreak.action.sdx;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxSetDefaultJavaVersionAction implements Action<SdxInternalTestDto, SdxClient> {

    private final String defaultJavaVersion;

    private final boolean restartServices;

    private final boolean restartCM;

    public SdxSetDefaultJavaVersionAction(String defaultJavaVersion, boolean restartServices, boolean restartCM) {
        this.defaultJavaVersion = defaultJavaVersion;
        this.restartServices = restartServices;
        this.restartCM = restartCM;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        SetDefaultJavaVersionRequest setDefaultJavaVersionAction = new SetDefaultJavaVersionRequest();
        setDefaultJavaVersionAction.setDefaultJavaVersion(defaultJavaVersion);
        setDefaultJavaVersionAction.setRestartServices(restartServices);
        setDefaultJavaVersionAction.setRestartCM(restartCM);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().setDefaultJavaVersionByName(testDto.getName(),
                setDefaultJavaVersionAction);
        testDto.setFlow("SDX set default Java version to " + defaultJavaVersion, flowIdentifier);
        return testDto;
    }
}
