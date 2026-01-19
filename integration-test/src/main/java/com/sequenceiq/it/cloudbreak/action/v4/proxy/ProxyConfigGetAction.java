package com.sequenceiq.it.cloudbreak.action.v4.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class ProxyConfigGetAction implements Action<ProxyTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigGetAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, EnvironmentClient client) throws Exception {
        Log.when(LOGGER, " Proxy config get request: " + testDto.getName());
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .proxyV1Endpoint()
                        .getByName(testDto.getName()));
        Log.whenJson(LOGGER, " Proxy config get successfully:\n", testDto.getResponse());
        return testDto;
    }

}