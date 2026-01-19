package com.sequenceiq.it.cloudbreak.action.v1.distrox;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.distrox.api.v1.distrox.model.DistroXVerticalScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroXVerticalScaleAction implements Action<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXVerticalScaleAction.class);

    private final String verticalScaleKey;

    public DistroXVerticalScaleAction(String verticalScaleKey) {
        this.verticalScaleKey = verticalScaleKey;
    }

    @Override
    public DistroXTestDto action(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        if (testContext.getCloudProvider().verticalScalingSupported()) {
            FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                    .distroXV1Endpoint()
                    .verticalScalingByCrn(
                            testDto.getResponse().getCrn(),
                            convertVerticalScaleTestDtoToStackV4VerticalScaleRequest(testContext));
            testDto.setFlow("DistroX put vertical scaling", flowIdentifier);
            Log.whenJson(LOGGER, format("DistroX put vertical scaling: %n"), testDto.getCrn());
        } else {
            Log.when(LOGGER, "DataHub vertical scale is not supported by now for the following cloud provider: " + testContext.getCloudPlatform());
        }
        return testDto;
    }

    private DistroXVerticalScaleV1Request convertVerticalScaleTestDtoToStackV4VerticalScaleRequest(TestContext testContext) {
        DistroXVerticalScaleV1Request verticalScaleRequest = new DistroXVerticalScaleV1Request();

        VerticalScalingTestDto verticalScalingTestDto = testContext.get(verticalScaleKey);
        InstanceTemplateV1Request instanceTemplateRequest = new InstanceTemplateV1Request();
        instanceTemplateRequest.setInstanceType(verticalScalingTestDto.getInstanceType());

        verticalScaleRequest.setGroup(verticalScalingTestDto.getGroupName());
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        return verticalScaleRequest;
    }

}
