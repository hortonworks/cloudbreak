package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXSetDefaultJavaVersionAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXSetDefaultJavaVersionAction.class);

    private final String javaVersion;

    private final boolean restartServices;

    private final boolean restartCM;

    private final boolean rollingRestart;

    public DistroXSetDefaultJavaVersionAction(String javaVersion, boolean restartServices, boolean restartCM, boolean rollingRestart) {
        this.javaVersion = javaVersion;
        this.restartServices = restartServices;
        this.restartCM = restartCM;
        this.rollingRestart = rollingRestart;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "Set default Java version on DistroX cluster: " + testDto.getName());
        SetDefaultJavaVersionRequest setDefaultJavaVersionRequest = new SetDefaultJavaVersionRequest();
        setDefaultJavaVersionRequest.setDefaultJavaVersion(javaVersion);
        setDefaultJavaVersionRequest.setRestartServices(restartServices);
        setDefaultJavaVersionRequest.setRestartCM(restartCM);
        setDefaultJavaVersionRequest.setRollingRestart(rollingRestart);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).distroXV1Endpoint()
                .setDefaultJavaVersionByName(testDto.getName(), setDefaultJavaVersionRequest);
        testDto.setFlow("Set default Java version on DistroX cluster", flowIdentifier);
        Log.when(LOGGER, "Set default Java version on DistroX flow: " + flowIdentifier);
        return testDto;
    }
}
