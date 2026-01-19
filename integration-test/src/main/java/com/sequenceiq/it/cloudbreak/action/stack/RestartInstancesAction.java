package com.sequenceiq.it.cloudbreak.action.stack;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.restart.RestartInstancesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class RestartInstancesAction implements Action<RestartInstancesTestDto, CloudbreakClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestartInstancesAction.class);

    private final List<String> instanceIds;

    public RestartInstancesAction(List<String> instanceIds) {
        this.instanceIds = instanceIds;
    }

    @Override
    public RestartInstancesTestDto action(TestContext testContext, RestartInstancesTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, format("Triggering restart instances on Cluster: %s ", testDto.getName()));
        FlowIdentifier flowIdentifier = client.getDefaultClient(testContext)
                .restartInstancesV4Endpoint()
                .restartInstancesForClusterName(testDto.getName(), instanceIds);
        testDto.setFlow("Cluster restart instances flow identifier", flowIdentifier);
        testDto.setResponse(flowIdentifier);
        return testDto;
    }
}
