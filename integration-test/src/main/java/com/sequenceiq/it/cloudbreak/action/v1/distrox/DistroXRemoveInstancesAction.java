package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXRemoveInstancesAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRemoveInstancesAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        List<String> removableInstanceIds = testDto.getRemovableInstanceIds();
        if (!removableInstanceIds.isEmpty()) {
            MultipleInstanceDeleteRequest instanceDeleteRequest = new MultipleInstanceDeleteRequest();
            instanceDeleteRequest.setInstances(removableInstanceIds);
            Log.when(LOGGER, String.format("Removing instances [%s] from Distrox...", instanceDeleteRequest.getInstances()));
            client.getDefaultClient()
                    .distroXV1Endpoint()
                    .deleteInstancesByCrn(testDto.getCrn(), removableInstanceIds, instanceDeleteRequest, false);
            StackV4Response stackV4Response = client.getDefaultClient()
                    .distroXV1Endpoint()
                    .getByName(testDto.getName(), new HashSet<>(Arrays.asList("hardware_info", "events")));
            testDto.setResponse(stackV4Response);
            Log.whenJson(LOGGER, " Distrox remove instances response: ", stackV4Response);
            LOGGER.info("Hardware info for stack after remove instances: {}", stackV4Response.getHardwareInfoGroups());
            return testDto;
        } else {
            throw new TestFailException("There is no instance id set for removal");
        }
    }
}
