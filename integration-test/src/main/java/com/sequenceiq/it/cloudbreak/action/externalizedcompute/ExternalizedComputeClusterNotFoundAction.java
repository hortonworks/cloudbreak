package com.sequenceiq.it.cloudbreak.action.externalizedcompute;

import jakarta.ws.rs.NotFoundException;

import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

public class ExternalizedComputeClusterNotFoundAction implements
        Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> {

    @Override
    public ExternalizedComputeClusterTestDto action(TestContext testContext, ExternalizedComputeClusterTestDto testDto,
            ExternalizedComputeClusterClient client) throws Exception {
        Log.when("Describe externalized compute cluster: " + testDto.getResponse().getName());
        try {
            ExternalizedComputeClusterResponse computeCluster =
                    client.getDefaultClient(testContext).externalizedComputeClusterEndpoint().describe(testDto.getEnvironmentCrn(),
                            testDto.getResponse().getName());
            Log.whenJson("Compute cluster should be deleted", computeCluster);
            throw new TestFailException("Compute cluster should be deleted: " + testDto.getResponse().getName());
        } catch (NotFoundException notFoundException) {
            Log.when("Compute cluster was not found: " + testDto.getResponse().getName());
        }
        return testDto;
    }
}
