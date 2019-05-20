package com.sequenceiq.it.cloudbreak.action.v4.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class ProxyConfigGetAction implements Action<ProxyTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigGetAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Proxy config get request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .proxyConfigV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, " Proxy config was get successfully:\n", testDto.getResponse());
        Log.log(LOGGER, String.format(" ID: %s", testDto.getResponse().getId()));
        return testDto;
    }

}