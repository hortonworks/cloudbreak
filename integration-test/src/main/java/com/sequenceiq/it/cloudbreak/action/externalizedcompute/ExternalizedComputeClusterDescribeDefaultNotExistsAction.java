package com.sequenceiq.it.cloudbreak.action.externalizedcompute;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

public class ExternalizedComputeClusterDescribeDefaultNotExistsAction implements
        Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> {

    @Override
    public ExternalizedComputeClusterTestDto action(TestContext testContext, ExternalizedComputeClusterTestDto testDto,
            ExternalizedComputeClusterClient client) throws Exception {
        Log.when("Describe externalized compute clusters by environment crn: " + testDto.getEnvironmentCrn());
        List<ExternalizedComputeClusterResponse> computeClusters =
                client.getDefaultClient(testContext).externalizedComputeClusterEndpoint().list(testDto.getEnvironmentCrn());
        Log.whenJson("Describe externalized compute clusters by environment crn: ", computeClusters);
        if (!CollectionUtils.isEmpty(computeClusters)) {
            throw new TestFailException("There are " + computeClusters.size() + " external compute clusters");
        }
        return testDto;
    }
}
