package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class DistroXVerticalScaleAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXVerticalScaleAction.class);

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        if (testContext.getCloudProvider().verticalScalingSupported()) {
            FlowIdentifier flowIdentifier = client.getDefaultClient()
                    .distroXV1Endpoint()
                    .putVerticalScalingByCrn(
                            testDto.getResponse().getCrn(),
                            testContext.getCloudProvider().getDistroXVerticalScalingTestDto().getRequest());
            testDto.setFlow("DistroX put vertical scaling", flowIdentifier);
            Log.whenJson(LOGGER, format("DistroX put vertical scaling: %n"), testDto.getCrn());
        }
        return testDto;
    }
}
