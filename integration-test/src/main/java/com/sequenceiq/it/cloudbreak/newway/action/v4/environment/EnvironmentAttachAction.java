package com.sequenceiq.it.cloudbreak.newway.action.v4.environment;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.environment.EnvironmentTestDto;

public class EnvironmentAttachAction implements Action<EnvironmentTestDto> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        EnvironmentAttachV4Request environmentAttachV4Request = new EnvironmentAttachV4Request();
        environmentAttachV4Request.setLdaps(entity.getRequest().getLdaps());
        environmentAttachV4Request.setProxies(entity.getRequest().getProxies());
        environmentAttachV4Request.setDatabases(entity.getRequest().getDatabases());

        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .attach(cloudbreakClient.getWorkspaceId(), entity.getName(), environmentAttachV4Request));
        logJSON("Environment put attach response: ", entity.getResponse());
        return entity;
    }
}