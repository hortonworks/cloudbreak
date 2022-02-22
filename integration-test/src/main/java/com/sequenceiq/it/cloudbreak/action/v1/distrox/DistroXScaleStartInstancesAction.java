package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import java.util.Arrays;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXScaleStartInstancesAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXScaleStartInstancesAction.class);

    private final Integer count;

    private final String hostGroup;

    public DistroXScaleStartInstancesAction(String hostGroup, Integer count) {
        this.count = count;
        this.hostGroup = hostGroup;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentRequest = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentRequest.setInstanceGroup(hostGroup);
        instanceGroupAdjustmentRequest.setScalingAdjustment(count);
        UpdateStackV4Request updateStackRequest = new UpdateStackV4Request();
        updateStackRequest.setInstanceGroupAdjustment(instanceGroupAdjustmentRequest);
        updateStackRequest.setWithClusterEvent(true);
        Log.when(LOGGER, String.format(" Starting instances [%s] for distrox '%s' Compute scaling... ", testDto.getName(),
                testDto.getRemovableInstanceIds()));
        Log.whenJson(LOGGER, " Distrox Compute scale request: ", testDto.getRequest());
        client.getDefaultClient()
                .autoscaleEndpoint()
                .putStackStartInstancesByName(testDto.getName(), updateStackRequest);
        StackV4Response stackV4Response = client.getDefaultClient()
                .distroXV1Endpoint()
                .getByName(testDto.getName(), new HashSet<>(Arrays.asList("hardware_info", "events")));
        testDto.setResponse(stackV4Response);
        Log.whenJson(LOGGER, " Distrox Compute scale response: ", stackV4Response);
        LOGGER.info("Hardware info for stack after start instances for Compute scale: {}", stackV4Response.getHardwareInfoGroups());
        return testDto;
    }
}
