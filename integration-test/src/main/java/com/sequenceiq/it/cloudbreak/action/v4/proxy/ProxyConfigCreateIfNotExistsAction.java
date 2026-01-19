package com.sequenceiq.it.cloudbreak.action.v4.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class ProxyConfigCreateIfNotExistsAction implements Action<ProxyTestDto, EnvironmentClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigCreateIfNotExistsAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, EnvironmentClient client) throws Exception {
        Log.whenJson(LOGGER, " Proxy config post request:\n", testDto.getRequest());
        try {
            testDto.setResponse(
                    client.getDefaultClient(testContext).proxyV1Endpoint().post(testDto.getRequest())
            );
            Log.whenJson(LOGGER, "ProxyConfig created successfully, response: ", testDto.getResponse());
        } catch (Exception e) {
            Log.when(LOGGER, "Cannot create ProxyConfig, fetch existed one: " + testDto.getRequest().getName());
            testDto.setResponse(
                    client.getDefaultClient(testContext).proxyV1Endpoint()
                            .getByName(testDto.getRequest().getName()));
            Log.whenJson(LOGGER, "ProxyConfig fetched successfully, response: ", testDto.getResponse());
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("ProxyConfig could not be created.");
        }
        return testDto;
    }
}
