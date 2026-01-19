package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairNodesV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRepairInstancesAction implements Action<DistroXTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRepairInstancesAction.class);

    public DistroXRepairInstancesAction() {
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroXRepairV1Request distroXRepairV1Request = createRepairRequest(testDto.getRepairableInstanceIds().orElse(Collections.emptyList()));
        Log.when(LOGGER, format("Starting repair instances on DistroX: %s ", testDto.getName()));
        Log.whenJson(LOGGER, "DistroX instance id based repair request: ", distroXRepairV1Request);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .repairClusterByName(testDto.getName(), distroXRepairV1Request);
        testDto.setFlow("DistroX instance id based repair flow identifier", flowIdentifier);
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(testDto.getName(), Collections.emptySet());
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " DistroX instance id based repair response: ", stackV4Response);
        return testDto;
    }

    private DistroXRepairV1Request createRepairRequest(List<String> instanceIds) {
        DistroXRepairV1Request distroXRepairV1Request = new DistroXRepairV1Request();
        DistroXRepairNodesV1Request nodes = new DistroXRepairNodesV1Request();
        nodes.setIds(instanceIds);
        distroXRepairV1Request.setNodes(nodes);
        return distroXRepairV1Request;
    }
}
