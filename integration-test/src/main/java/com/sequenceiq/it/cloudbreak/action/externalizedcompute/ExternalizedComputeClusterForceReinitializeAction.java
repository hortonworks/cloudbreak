package com.sequenceiq.it.cloudbreak.action.externalizedcompute;

import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

public class ExternalizedComputeClusterForceReinitializeAction implements
        Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> {

    @Override
    public ExternalizedComputeClusterTestDto action(TestContext testContext, ExternalizedComputeClusterTestDto testDto,
            ExternalizedComputeClusterClient client) throws Exception {
        String name = testDto.getResponse().getName();
        Log.when("Reinitialize externalized compute cluster: " + name);
        ExternalizedComputeClusterRequest request = new ExternalizedComputeClusterRequest();
        request.setName(name);
        request.setEnvironmentCrn(testDto.getEnvironmentCrn());
        FlowIdentifier flowIdentifier = client.getInternalClient(testContext).externalizedComputeClusterInternalEndpoint()
                .reInitialize(request, testContext.getActingUserCrn().toString(), true);
        Log.whenJson("Reinitialize externalized compute cluster: ", flowIdentifier);
        return testDto;
    }
}
