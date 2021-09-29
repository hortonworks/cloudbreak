package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class FreeIpaUpgradeAction implements Action<FreeIpaTestDto, FreeIpaClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaUpgradeAction.class);

    public FreeIpaTestDto action(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, format(" FreeIPA post request:%n"), testDto.getRequest());
        FreeIpaUpgradeRequest request = new FreeIpaUpgradeRequest();
        request.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        FreeIpaUpgradeResponse response = client.getDefaultClient()
                .getFreeIpaUpgradeV1Endpoint()
                .upgradeFreeIpa(request);
        testDto.setFlow("FreeIPA upgrade", response.getFlowIdentifier());
        testDto.setOperationId(response.getOperationId());
        Log.whenJson(LOGGER, format(" FreeIPA upgrade started: %n"), response);
        return testDto;
    }
}
