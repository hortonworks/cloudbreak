package com.sequenceiq.it.cloudbreak.action.v4.proxy;

import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ProxyConfigDeleteAction implements Action<ProxyTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigDeleteAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, EnvironmentClient client) throws Exception {
        Log.log(LOGGER, format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Proxy config delete request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getEnvironmentClient()
                        .proxyV1Endpoint()
                        .deleteByName(testDto.getName()));
        Log.logJSON(LOGGER, " Proxy config was deleted successfully:\n", testDto.getResponse());
        Log.log(LOGGER, format(" CRN: %s", testDto.getResponse().getCrn()));
        return testDto;
    }

}