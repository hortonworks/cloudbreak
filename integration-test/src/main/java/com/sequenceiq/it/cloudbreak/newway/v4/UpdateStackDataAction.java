package com.sequenceiq.it.cloudbreak.newway.v4;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.StackRepositoryEntity;

public class UpdateStackDataAction implements Action<StackRepositoryEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStackDataAction.class);

    private UpdateClusterV4Request request = new UpdateClusterV4Request();

    @Override
    public StackRepositoryEntity action(TestContext testContext, StackRepositoryEntity entity, CloudbreakClient client) throws Exception {
        StackTestDto stackTestDto = testContext.get(StackTestDto.class);
        request.setStackRepository(entity.getRequest());

        logJSON(" Enable Maintenance Mode post request:\n", request);

        client.getCloudbreakClient()
                .stackV4Endpoint()
                .putCluster(client.getWorkspaceId(), stackTestDto.getName(), request);

        return entity;
    }
}
