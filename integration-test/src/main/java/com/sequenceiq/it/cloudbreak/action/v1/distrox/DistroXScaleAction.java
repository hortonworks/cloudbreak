package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.common.api.type.AdjustmentType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXScaleAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXScaleAction.class);

    private final Integer desiredCount;

    private final String hostGroup;

    private final AdjustmentType adjustmentType;

    private final Long threshold;

    public DistroXScaleAction(String hostGroup, Integer desiredCount, AdjustmentType adjustmentType, Long threshold) {
        this.desiredCount = desiredCount;
        this.hostGroup = hostGroup;
        this.adjustmentType = adjustmentType;
        this.threshold = threshold;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, String.format("Distrox scale request on: %s. Hostgroup: %s, desiredCount: %d", testDto.getName(), hostGroup, desiredCount));
        Log.whenJson(LOGGER, " Distrox scale request: ", testDto.getRequest());
        DistroXScaleV1Request scaleRequest = new DistroXScaleV1Request();
        scaleRequest.setGroup(hostGroup);
        scaleRequest.setDesiredCount(desiredCount);
        scaleRequest.setAdjustmentType(adjustmentType);
        scaleRequest.setThreshold(threshold);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .putScalingByName(testDto.getName(), scaleRequest);
        StackV4Response stackV4Response = client.getDefaultClient(testContext)
                .distroXV1Endpoint()
                .getByName(testDto.getName(), new HashSet<>(Arrays.asList("hardware_info", "events")));
        testDto.setFlow("Distrox scale", flowIdentifier);
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Distrox scale response: ", stackV4Response);
        LOGGER.info("Hardware info for stack after upscale: {}", stackV4Response.getHardwareInfoGroups());
        return testDto;
    }
}
