package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXDiskUpdateAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateAction.class);

    private int size;

    private String volumeType;

    private String instanceGroup;

    public DistroXDiskUpdateAction(int size, String volumeType, String instanceGroup) {
        this.size = size;
        this.volumeType = volumeType;
        this.instanceGroup = instanceGroup;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "DistroX endpoint: %s" + client.getDefaultClient().distroXV1Endpoint() + ", DistroX's environment: " +
                testDto.getRequest().getEnvironmentName());
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setGroup(instanceGroup);
        diskUpdateRequest.setSize(size);
        diskUpdateRequest.setVolumeType(volumeType);
        Log.whenJson(LOGGER, "DistroX Disk Update request: ", diskUpdateRequest);
        FlowIdentifier flowIdentifier = client.getDefaultClient()
                .distroXV1Endpoint()
                .diskUpdateByName(testDto.getName(), diskUpdateRequest);
        testDto.setFlow("DistroX Disk Update Flow", flowIdentifier);
        Log.whenJson(LOGGER, "DistroX Disk Update Flow: ", flowIdentifier);
        return testDto;
    }

}