package com.sequenceiq.it.cloudbreak.action.v4.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ProxyConfigCreateIfNotExistsAction implements Action<ProxyTestDto, EnvironmentClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigCreateIfNotExistsAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, EnvironmentClient client) throws Exception {
        LOGGER.info("Create ProxyConfig with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getEnvironmentClient().proxyV1Endpoint().post(testDto.getRequest())
            );
            Log.logJSON(LOGGER, "ProxyConfig created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create ProxyConfig, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getEnvironmentClient().proxyV1Endpoint()
                            .getByName(testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("ProxyConfig could not be created.");
        }
        return testDto;
    }
}
