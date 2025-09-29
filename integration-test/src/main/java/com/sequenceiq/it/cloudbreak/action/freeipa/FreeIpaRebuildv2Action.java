package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaRebuildv2Action extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRebuildv2Action.class);

    public FreeIpaRebuildv2Action() {
    }

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getRequest().getEnvironmentCrn()));
        RebuildV2Request request = new RebuildV2Request();
        request.setEnvironmentCrn(testDto.getRequest().getEnvironmentCrn());
        request.setResourceCrn(testDto.getCrn());
        request.setInstanceToRestoreFqdn("ipaserver1.ipatest.local");
        request.setFullBackupStorageLocation(testContext.getCloudProvider().getFreeIpaRebuildFullBackup());
        request.setDataBackupStorageLocation(testContext.getCloudProvider().getFreeIpaRebuildDataBackup());
        Log.whenJson(LOGGER, format(" FreeIPA rebuild request: %n"), request);
        RebuildV2Response response = client.getDefaultClient()
                .getFreeIpaV2Endpoint()
                .rebuildv2(request);
        testDto.setOperationId(response.getOperationStatus().getOperationId());
        testDto.setLastKnownFlowId(response.getFlowIdentifier().getPollableId());
        Log.whenJson(LOGGER, format(" FreeIPA rebuilt successfully:%n"), response);
        Log.when(LOGGER, format(" FreeIPA CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }
}
