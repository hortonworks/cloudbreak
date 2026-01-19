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

public class DistroXUpgradeAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXUpgradeAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXUpgradeV1Request upgradeRequest = testDto.getDistroXUpgradeRequest();
        Log.when(LOGGER, format(" Starting DistroX upgrade: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " DistroX upgrade request: ", upgradeRequest);
        DistroXUpgradeV1Response response = client.getDefaultClient(testContext)
                .distroXUpgradeV1Endpoint()
                .upgradeClusterByName(testDto.getName(), upgradeRequest);
        testDto.setFlow("DistroX upgrade flow identifier", response.flowIdentifier());
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " DistroX upgrade response: ", stackV4Response);
        return testDto;
    }

}
