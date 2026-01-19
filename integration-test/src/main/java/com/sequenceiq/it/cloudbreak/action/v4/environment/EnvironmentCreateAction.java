package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.environment.api.v1.environment.model.response.CreateEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentCreateAction extends AbstractEnvironmentAction {

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        if (!StringUtils.equals(testContext.getExistingResourceNames().get(EnvironmentTestDto.class), testDto.getName())) {
            Log.whenJson("Environment post request: ", testDto.getRequest());
            CreateEnvironmentResponse createEnvironmentResponse = client.getDefaultClient(testContext).environmentV1Endpoint().post(testDto.getRequest());
            testDto.setResponse(createEnvironmentResponse);
            testDto.setFlow("environmentCreateFlow", createEnvironmentResponse.getFlowIdentifier());
        } else {
            DetailedEnvironmentResponse detailedEnvironmentResponse = client.getDefaultClient(testContext).environmentV1Endpoint().getByName(testDto.getName());
            if (detailedEnvironmentResponse != null) {
                FlowLogResponse flow = client.getDefaultClient(testContext).flowEndpoint().getLastFlowByResourceCrn(detailedEnvironmentResponse.getCrn());
                testDto.setResponse(detailedEnvironmentResponse);
                testDto.setFlow("environmentCreateFlow",
                        new FlowIdentifier(
                                StringUtils.isNoneBlank(flow.getFlowId()) ? FlowType.FLOW : FlowType.FLOW_CHAIN,
                                StringUtils.isNoneBlank(flow.getFlowId()) ? flow.getFlowId() : flow.getFlowChainId()
                        )
                );
            }
        }
        Log.whenJson("Environment response: ", testDto.getResponse());
        return testDto;
    }
}