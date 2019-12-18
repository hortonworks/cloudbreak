package com.sequenceiq.it.cloudbreak.v4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.StackRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class UpdateStackDataAction implements Action<StackRepositoryTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStackDataAction.class);

    private UpdateClusterV4Request request = new UpdateClusterV4Request();

    @Override
    public StackRepositoryTestDto action(TestContext testContext, StackRepositoryTestDto testDto, CloudbreakClient client) throws Exception {
        StackTestDto stackTestDto = testContext.get(StackTestDto.class);

        Log.whenJson(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .putCluster(client.getWorkspaceId(), stackTestDto.getName(), request);

        return testDto;
    }
}
