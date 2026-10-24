package com.sequenceiq.it.cloudbreak.action.v4.environment;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDeleteTagsRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentDeleteTagsByNameAction extends AbstractEnvironmentAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeleteTagsByNameAction.class);

    private final Set<String> tagKeys;

    public EnvironmentDeleteTagsByNameAction(Set<String> tagKeys) {
        this.tagKeys = tagKeys;
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        EnvironmentDeleteTagsRequest request = new EnvironmentDeleteTagsRequest();
        request.setTagKeys(tagKeys);
        DetailedEnvironmentResponse response = client.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .deleteTagsByName(testDto.getResponse().getName(), request);
        testDto.setResponse(response);
        Log.when(LOGGER, "Environment delete tags action posted for tag keys: " + tagKeys);
        return testDto;
    }
}
