package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXScaleAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXScaleAction.class);

    private Integer count;

    private String hostGroup;

    public DistroXScaleAction(String hostGroup, Integer count) {
        this.count = count;
        this.hostGroup = hostGroup;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.log(LOGGER, " Stack scale request on: %s", testDto.getName());
        DistroXScaleV1Request scaleRequest = new DistroXScaleV1Request();
        scaleRequest.setGroup(hostGroup);
        scaleRequest.setDesiredCount(count);
        client.getCloudbreakClient()
                .distroXV1Endpoint()
                .putScalingByName(testDto.getName(), scaleRequest);
        Log.log(LOGGER, " Stack scale request was successful on: %s.", testDto.getName());
        return testDto;
    }
}
