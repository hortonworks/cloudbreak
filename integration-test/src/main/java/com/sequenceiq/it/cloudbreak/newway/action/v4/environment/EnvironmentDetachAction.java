package com.sequenceiq.it.cloudbreak.newway.action.v4.environment;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;

public class EnvironmentDetachAction implements Action<EnvironmentTestDto> {

    @Override
    public EnvironmentTestDto action(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        EnvironmentDetachV4Request environmentDetachV4Request = new EnvironmentDetachV4Request();
        environmentDetachV4Request.setLdaps(testDto.getRequest().getLdaps());
        environmentDetachV4Request.setProxies(testDto.getRequest().getProxies());
        environmentDetachV4Request.setDatabases(testDto.getRequest().getDatabases());
        environmentDetachV4Request.setKerberoses(testDto.getRequest().getKerberoses());
        testDto.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .environmentV4Endpoint()
                        .detach(cloudbreakClient.getWorkspaceId(), testDto.getName(), environmentDetachV4Request));
        logJSON("Environment put detach response: ", testDto.getResponse());
        return testDto;
    }
}