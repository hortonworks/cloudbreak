package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXDeleteDisksAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXDeleteDisksAction.class);

    private String instanceGroup;

    public DistroXDeleteDisksAction(String instanceGroup) {
        this.instanceGroup = instanceGroup;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, "DistroX endpoint: %s" + client.getDefaultClient(testContext).distroXV1Endpoint() + ", DistroX's environment: " +
                testDto.getRequest().getEnvironmentName());
        StackDeleteVolumesRequest diskDeleteRequest = new StackDeleteVolumesRequest();
        diskDeleteRequest.setGroup(instanceGroup);
        Log.whenJson(LOGGER, "DistroX Delete request: ", diskDeleteRequest);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .deleteVolumesByStackName(testDto.getName(), diskDeleteRequest);
        testDto.setFlow("DistroX Delete Flow", flowIdentifier);
        Log.whenJson(LOGGER, "DistroX Delete Flow: ", flowIdentifier);
        return testDto;
    }

}
