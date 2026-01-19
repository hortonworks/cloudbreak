package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXRemoveInstancesAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRemoveInstancesAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        List<String> removableInstanceIds = testDto.getInstanceIdsForAction();
        if (!removableInstanceIds.isEmpty()) {
            MultipleInstanceDeleteRequest instanceDeleteRequest = new MultipleInstanceDeleteRequest();
            instanceDeleteRequest.setInstances(removableInstanceIds);
            Log.when(LOGGER, String.format(" Removing instances [%s] from distrox '%s'... ", instanceDeleteRequest.getInstances(), testDto.getName()));
            FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                    .distroXV1Endpoint()
                    .deleteInstancesByCrn(testDto.getCrn(), removableInstanceIds, instanceDeleteRequest, false);
            testDto.setFlow("Instance deletion", flowIdentifier);
            StackV4Response stackV4Response = client.getDefaultClient(testContext)
                    .distroXV1Endpoint()
                    .getByName(testDto.getName(), new HashSet<>(Arrays.asList("hardware_info", "events")));
            testDto.setResponse(stackV4Response);
            Log.whenJson(LOGGER, " Distrox remove instances response: ", stackV4Response);
            LOGGER.info(String.format("Hardware info for distrox '%s' after remove instances [%s].", testDto.getName(),
                    stackV4Response.getHardwareInfoGroups()));
            return testDto;
        } else {
            throw new TestFailException(String.format("Cannot find any instance to remove from distrox '%s'!", testDto.getName()));
        }
    }
}
