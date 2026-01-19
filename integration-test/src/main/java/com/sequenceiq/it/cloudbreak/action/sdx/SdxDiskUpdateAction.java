package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
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

    private DiskType diskType;

    public SdxDiskUpdateAction(int size, String volumeType, String instanceGroup, DiskType diskType) {
        this.size = size;
        this.volumeType = volumeType;
        this.instanceGroup = instanceGroup;
        this.diskType = diskType != null ? diskType : DiskType.ADDITIONAL_DISK;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX disk update request: ", testDto.getRequest());
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setVolumeType(volumeType);
        diskUpdateRequest.setSize(size);
        diskUpdateRequest.setGroup(instanceGroup);
        diskUpdateRequest.setDiskType(diskType);
        FlowIdentifier flowIdentifier;
        if (DiskType.ADDITIONAL_DISK.equals(diskType)) {
            flowIdentifier = client.getDefaultClient(testContext)
                    .sdxEndpoint()
                    .diskUpdateByName(testDto.getResponse().getStackV4Response().getName(), diskUpdateRequest);
        } else {
            flowIdentifier = client.getDefaultClient(testContext)
                    .sdxEndpoint()
                    .updateRootVolumeByDatalakeName(testDto.getResponse().getStackV4Response().getName(), diskUpdateRequest);
        }
        testDto.setFlow("SdxDiskUpdate", flowIdentifier);
        Log.whenJson(LOGGER, " SDX DiskUpdate Flow: ", client.getDefaultClient(testContext).sdxEndpoint().get(testDto.getName()));
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX DiskUpdate response: ", detailedResponse);
        return testDto;
    }
}
