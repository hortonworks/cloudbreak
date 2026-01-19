package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;

public class SdxVerticalScaleAction implements Action<SdxInternalTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxVerticalScaleAction.class);

    private final String verticalScaleKey;

    public SdxVerticalScaleAction(String verticalScaleKey) {
        this.verticalScaleKey = verticalScaleKey;
    }

    @Override
    public SdxInternalTestDto action(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) throws Exception {
        Log.whenJson(LOGGER, format(" SDX vertical scale for crn %n"), testDto.getCrn());
        if (testContext.getCloudProvider().verticalScalingSupported()) {
            StackVerticalScaleV4Request verticalScaleRequest = convertVerticalScaleTestDtoToStackV4VerticalScaleRequest(testContext);
            FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).sdxEndpoint().verticalScalingByCrn(testDto.getCrn(), verticalScaleRequest);
            testDto.setFlow("SDX vertical scale", flowIdentifier);
            Log.whenJson(LOGGER, format(" SDX vertical scale started: %n"), testDto.getCrn());
        } else {
            Log.when(LOGGER, "SDX vertical scale is not supported by now for the following cloud provider: " + testContext.getCloudPlatform());
        }
        return testDto;
    }

    private StackVerticalScaleV4Request convertVerticalScaleTestDtoToStackV4VerticalScaleRequest(TestContext testContext) {
        StackVerticalScaleV4Request verticalScaleRequest = new StackVerticalScaleV4Request();

        VerticalScalingTestDto verticalScalingTestDto = testContext.get(verticalScaleKey);
        InstanceTemplateV4Request instanceTemplateRequest = new InstanceTemplateV4Request();
        instanceTemplateRequest.setInstanceType(verticalScalingTestDto.getInstanceType());

        verticalScaleRequest.setGroup(verticalScalingTestDto.getGroupName());
        verticalScaleRequest.setTemplate(instanceTemplateRequest);
        return verticalScaleRequest;
    }
}
