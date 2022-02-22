package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.base.ScalingStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXScaleStopInstancesAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXScaleStopInstancesAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, String.format(" Stopping instances [%s] for distrox '%s' Compute scaling... ", testDto.getName(),
                testDto.getRemovableInstanceIds()));
        Log.whenJson(LOGGER, " Distrox Compute scale request: ", testDto.getRequest());
        client.getDefaultClient()
                .autoscaleEndpoint()
                .stopInstancesForClusterName(testDto.getName(), testDto.getRemovableInstanceIds(), false, ScalingStrategy.STOPSTART);
        StackV4Response stackV4Response = client.getDefaultClient()
                .distroXV1Endpoint()
                .getByName(testDto.getName(), new HashSet<>(Arrays.asList("hardware_info", "events")));
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Distrox Compute scale response: ", stackV4Response);
        LOGGER.info("Hardware info for stack after stop instances for Compute scale: {}", stackV4Response.getHardwareInfoGroups());
        return testDto;
    }
}
