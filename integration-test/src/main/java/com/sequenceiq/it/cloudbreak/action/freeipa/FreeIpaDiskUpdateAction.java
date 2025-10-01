package com.sequenceiq.it.cloudbreak.action.freeipa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DiskUpdateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.UpdateRootVolumeResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class FreeIpaDiskUpdateAction extends AbstractFreeIpaAction<FreeIpaTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDiskUpdateAction.class);

    private int size;

    private String volumeType;

    public FreeIpaDiskUpdateAction(int size, String volumeType) {
        this.size = size;
        this.volumeType = volumeType;
    }

    @Override
    public FreeIpaTestDto freeIpaAction(TestContext testContext, FreeIpaTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, " FreeIpa endpoint: %s" + client.getDefaultClient().getFreeIpaV1Endpoint() + ", FreeIpa's environment: "
                + testDto.getRequest().getName());
        Log.whenJson(LOGGER, " FreeIpa disk update request: ", testDto.getRequest());
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setVolumeType(volumeType);
        diskUpdateRequest.setSize(size);
        Log.whenJson(LOGGER, "FreeIpa DiskUpdate Request: ", diskUpdateRequest);
        String environmentCrn = testContext.given(EnvironmentTestDto.class).getCrn();
        UpdateRootVolumeResponse updateRootVolumeResponse = client.getDefaultClient()
                .getFreeIpaV1Endpoint()
                .updateRootVolumeByCrn(environmentCrn, diskUpdateRequest);
        testDto.setFlow("FreeIpaDiskUpdateFlow", updateRootVolumeResponse.getFlowIdentifier());
        Log.whenJson(LOGGER, "FreeIpaDiskUpdateFlow: ", updateRootVolumeResponse.getFlowIdentifier());
        return testDto;
    }
}
