package com.sequenceiq.it.cloudbreak.action.externalizedcompute;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

public class ExternalizedComputeClusterDeleteAction implements
        Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> {

    @Override
    public ExternalizedComputeClusterTestDto action(TestContext testContext, ExternalizedComputeClusterTestDto testDto,
            ExternalizedComputeClusterClient client) throws Exception {
        String name = testDto.getResponse().getName();
        Log.when("Delete externalized compute cluster: " + name);
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext).externalizedComputeClusterEndpoint()
                .delete(testDto.getEnvironmentCrn(), name, false);
        Log.whenJson("Delete externalized compute cluster: ", flowIdentifier);
        return testDto;
    }
}
