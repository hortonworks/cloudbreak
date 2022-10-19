package com.sequenceiq.it.cloudbreak.action.v4.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.action.RetryableAction;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;

public class ProxyConfigListAction implements RetryableAction<ProxyTestDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigListAction.class);

    @Override
    public ProxyTestDto action(TestContext testContext, ProxyTestDto testDto, EnvironmentClient client) throws Exception {
        testDto.setResponses(
                Sets.newHashSet(client.getDefaultClient()
                        .proxyV1Endpoint()
                        .list().getResponses()));
        return testDto;
    }

}