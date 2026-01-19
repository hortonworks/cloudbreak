package com.sequenceiq.it.cloudbreak.action.externalizedcompute;

import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

public class ExternalizedComputeClusterDescribeAction implements
        Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> {

    @Override
    public ExternalizedComputeClusterTestDto action(TestContext testContext, ExternalizedComputeClusterTestDto testDto,
            ExternalizedComputeClusterClient client) throws Exception {
        Log.when("Describe externalized compute cluster: " + testDto.getResponse().getName());
        ExternalizedComputeClusterResponse computeCluster = client.getDefaultClient(testContext)
                .externalizedComputeClusterEndpoint().describe(testDto.getEnvironmentCrn(),
                testDto.getResponse().getName());
        Log.whenJson("Describe externalized compute cluster: ", computeCluster);
        testDto.setResponse(computeCluster);
        return testDto;
    }
}
