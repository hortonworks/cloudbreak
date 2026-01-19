package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXUpgradeInternalAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeInternalAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXUpgradeV1Request upgradeRequest = testDto.getDistroXUpgradeRequest();
        Log.when(LOGGER, format(" Starting DistroX upgrade internal: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " DistroX upgrade internal request: ", upgradeRequest);
        DistroXUpgradeV1Response response = client.getInternalClient(testContext)
                .distroXUpgradeV1Endpoint()
                .upgradeClusterByCrnInternal(testDto.getCrn(), upgradeRequest, testContext.getActingUserCrn().toString(), false);
        testDto.setFlow("DistroX upgrade internal flow identifier", response.flowIdentifier());
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " DistroX upgrade internal response: ", stackV4Response);
        return testDto;
    }

}
