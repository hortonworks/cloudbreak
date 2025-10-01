package com.sequenceiq.it.cloudbreak.action.freeipa;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Request;
import com.sequenceiq.freeipa.api.v2.freeipa.model.rebuild.RebuildV2Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaRebuildv2Action extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRebuildv2Action.class);

    public FreeIpaRebuildv2Action() {
    }

    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        String ipaserver1Fqdn = testDto.getResponse().getInstanceGroups().getFirst().getMetaData()
                .stream()
                .map(InstanceMetaDataResponse::getDiscoveryFQDN)
                .filter(fqdn -> fqdn.startsWith("ipaserver1"))
                .findFirst()
                .orElseThrow(() -> new TestFailException("Failed to determine 'ipaserver1' fqdn for rebuild."));
        Log.when(LOGGER, format(" FreeIPA CRN: %s", environmentCrn));
        RebuildV2Request request = new RebuildV2Request();
        request.setEnvironmentCrn(environmentCrn);
        request.setResourceCrn(testDto.getCrn());
        request.setInstanceToRestoreFqdn(ipaserver1Fqdn);
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
