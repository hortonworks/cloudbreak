package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.ExternalizedComputeCreateRequest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentCreateDefaultExternalizedComputeClusterAction implements Action<EnvironmentTestDto, EnvironmentClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCreateDefaultExternalizedComputeClusterAction.class);

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        Log.when(LOGGER, "Create default externalized compute cluster for env, crn: " +  testDto.getResponse().getCrn());
        ExternalizedComputeCreateRequest request = new ExternalizedComputeCreateRequest();
        request.setCreate(true);
        client.getDefaultClient(testContext)
                .defaultComputeClusterEndpoint().createDefaultExternalizedComputeCluster(testDto.getResponse().getCrn(), request, false);
        return testDto;
    }
}
