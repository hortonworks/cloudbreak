package com.sequenceiq.it.cloudbreak.action.sdx;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxAddDisksAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxAddDisksAction.class);

    private final Long size;

    private final Long numberOfDisks;

    private final String volumeType;

    private final String instanceGroup;

    private final CloudVolumeUsageType cloudVolumeUsageType;

    public SdxAddDisksAction(Long size, Long numberOfDisks, String volumeType, String instanceGroup, CloudVolumeUsageType cloudVolumeUsageType) {
        this.size = size;
        this.numberOfDisks = numberOfDisks;
        this.volumeType = volumeType;
        this.instanceGroup = instanceGroup;
        this.cloudVolumeUsageType = cloudVolumeUsageType;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, " SDX endpoint: %s" + client.getDefaultClient(testContext).sdxEndpoint() + ", SDX's environment: "
                + testDto.getRequest().getEnvironment());
        Log.whenJson(LOGGER, " SDX add volumes request: ", testDto.getRequest());
        StackAddVolumesRequest addVolumesRequest = new StackAddVolumesRequest();
        addVolumesRequest.setType(volumeType);
        addVolumesRequest.setSize(size);
        addVolumesRequest.setInstanceGroup(instanceGroup);
        addVolumesRequest.setNumberOfDisks(numberOfDisks);
        addVolumesRequest.setCloudVolumeUsageType(cloudVolumeUsageType.toString());
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .addVolumesByStackName(testDto.getResponse().getStackV4Response().getName(), addVolumesRequest);
        testDto.setFlow("SdxAddVolumes", flowIdentifier);
        Log.whenJson(LOGGER, " SDX Add Volumes Flow: ", client.getDefaultClient(testContext).sdxEndpoint().get(testDto.getName()));
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX Add Volumes response: ", detailedResponse);
        return testDto;
    }
}
