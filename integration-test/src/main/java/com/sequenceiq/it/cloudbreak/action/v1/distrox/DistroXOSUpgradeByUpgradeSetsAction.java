package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXOSUpgradeByUpgradeSetsAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXOSUpgradeByUpgradeSetsAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXUpgradeV1Request upgradeRequest = testDto.getDistroXUpgradeRequest();
        Log.when(LOGGER, format(" Starting DistroX upgrade: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " DistroX upgrade request: ", upgradeRequest);
        OrderedOSUpgradeSetRequest numberedOsUpgradeSetRequest = new OrderedOSUpgradeSetRequest();
        numberedOsUpgradeSetRequest.setOrderedOsUpgradeSets(testDto.getOsUpgradeByUpgradeSets());
        FlowIdentifier flowIdentifier = client.getInternalClient(testContext)
                .distroXUpgradeV1Endpoint()
                .osUpgradeByUpgradeSetsInternal(testDto.getCrn(), numberedOsUpgradeSetRequest);
        testDto.setFlow("DistroX upgrade flow identifier", flowIdentifier);
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " DistroX upgrade response: ", stackV4Response);
        return testDto;
    }

}
