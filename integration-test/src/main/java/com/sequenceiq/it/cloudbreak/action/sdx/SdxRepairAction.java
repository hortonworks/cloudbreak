package com.sequenceiq.it.cloudbreak.action.sdx;

import static java.lang.String.format;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

public class SdxRepairAction implements Action<SdxTestDto, SdxClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRepairAction.class);

    private final String[] hostGroups;

    public SdxRepairAction(String[] hostGroups) {
        this.hostGroups = hostGroups;
    }

    @Override
    public SdxTestDto action(TestContext testContext, SdxTestDto testDto, SdxClient client) throws Exception {
        Log.when(LOGGER, format(" Starting repair on SDX: %s ", testDto.getName()));
        Log.whenJson(LOGGER, " SDX repair request: ", testDto.getSdxRepairRequest(hostGroups));
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .repairCluster(testDto.getName(), testDto.getSdxRepairRequest(hostGroups));
        testDto.setFlow("SDX repair", flowIdentifier);
        SdxClusterDetailResponse detailedResponse = client.getDefaultClient(testContext)
                .sdxEndpoint()
                .getDetail(testDto.getName(), Collections.emptySet());
        testDto.setResponse(detailedResponse);
        Log.whenJson(LOGGER, " SDX repair response: ", detailedResponse);
        return testDto;
    }
}
