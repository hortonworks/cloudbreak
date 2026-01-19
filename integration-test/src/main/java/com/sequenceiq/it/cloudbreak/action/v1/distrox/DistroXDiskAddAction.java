package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXDiskAddAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDiskUpdateAction.class);

    private int size;

    private String volumeType;

    private String instanceGroup;

    private long numDisks;

    public DistroXDiskAddAction(int size, String volumeType, String instanceGroup, long numDisks) {
        this.size = size;
        this.volumeType = volumeType;
        this.instanceGroup = instanceGroup;
        this.numDisks = numDisks;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "DistroX endpoint: %s" + client.getDefaultClient(testContext).distroXV1Endpoint() + ", DistroX's environment: " +
                testDto.getRequest().getEnvironmentName());
        StackAddVolumesRequest stackAddVolumesRequest = new StackAddVolumesRequest();
        stackAddVolumesRequest.setInstanceGroup(instanceGroup);
        stackAddVolumesRequest.setSize(Long.valueOf(size));
        stackAddVolumesRequest.setType(volumeType);
        stackAddVolumesRequest.setNumberOfDisks(numDisks);
        stackAddVolumesRequest.setCloudVolumeUsageType(CloudVolumeUsageType.GENERAL.toString());

        Log.whenJson(LOGGER, "DistroX Disk Add request: ", stackAddVolumesRequest);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .addVolumesByStackName(testDto.getName(), stackAddVolumesRequest);
        testDto.setFlow("DistroX Disk Add Flow", flowIdentifier);
        Log.whenJson(LOGGER, "DistroX Disk Add Flow: ", flowIdentifier);
        return testDto;
    }

}