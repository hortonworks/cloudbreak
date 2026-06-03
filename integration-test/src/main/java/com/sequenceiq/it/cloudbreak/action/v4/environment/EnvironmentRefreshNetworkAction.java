package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentRefreshNetworkAction implements Action<EnvironmentTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentRefreshNetworkAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        request.setRefreshNetwork(true);
        String crn = testDto.getResponse().getCrn();
        testDto.setResponse(environmentClient.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .editByCrn(crn, request));
        FlowLogResponse flow = environmentClient.getInternalClient(testContext).flowEndpoint().getLastFlowByResourceCrn(crn);
        testDto.setFlow("environmentRefreshNetworkFlow", new FlowIdentifier(
                StringUtils.isNoneBlank(flow.getFlowId()) ? FlowType.FLOW : FlowType.FLOW_CHAIN,
                StringUtils.isNoneBlank(flow.getFlowId()) ? flow.getFlowId() : flow.getFlowChainId()));
        Log.when(LOGGER, "Environment refresh network edit action posted");
        return testDto;
    }
}
