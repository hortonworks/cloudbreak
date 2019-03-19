package com.sequenceiq.it.cloudbreak.newway.action.v4.proxy;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;

public class ProxyConfigCreateIfNotExistsAction implements Action<ProxyTestDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigCreateIfNotExistsAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, CloudbreakClient client) throws Exception {
        LOGGER.info("Create ProxyConfig with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getCloudbreakClient().proxyConfigV4Endpoint().post(client.getWorkspaceId(), testDto.getRequest())
            );
            logJSON(LOGGER, "ProxyConfig created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create ProxyConfig, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getCloudbreakClient().proxyConfigV4Endpoint()
                            .get(client.getWorkspaceId(), testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("ProxyConfig could not be created.");
        }
        return testDto;
    }
}
