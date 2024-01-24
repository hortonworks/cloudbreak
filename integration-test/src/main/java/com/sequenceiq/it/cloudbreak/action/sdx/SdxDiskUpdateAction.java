package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxDiskUpdateAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDiskUpdateAction.class);

    private int size;

    private String volumeType;

    private String instanceGroup;

    public SdxDiskUpdateAction(int size, String volumeType, String instanceGroup) {
        this.size = size;
        this.volumeType = volumeType;
        this.instanceGroup = instanceGroup;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient().sdxEndpoint() + ", SDX's environment: " + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX disk update request: ", testDto.getRequest());
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setVolumeType(volumeType);
        diskUpdateRequest.setSize(size);
        diskUpdateRequest.setGroup(instanceGroup);
        FlowIdentifier flowIdentifier = client.getDefaultClient()
                .sdxEndpoint()
                .diskUpdateByName(testDto.getResponse().getStackV4Response().getName(), diskUpdateRequest);
        testDto.setFlow("SdxDiskUpdate", flowIdentifier);
        Log.whenJson(LOGGER, " SDX DiskUpdate Flow: ", client.getDefaultClient().sdxEndpoint().get(testDto.getName()));
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient()
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX DiskUpdate response: ", detailedResponse);
        return testDto;
    }
}
