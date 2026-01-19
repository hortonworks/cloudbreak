package com.sequenceiq.it.cloudbreak.action.v4.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class EnvironmentModifyProxyConfigAction extends AbstractEnvironmentAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentModifyProxyConfigAction.class);

    private final String proxyConfigName;

    public EnvironmentModifyProxyConfigAction(String proxyConfigName) {
        this.proxyConfigName = proxyConfigName;
    }

    @Override
    protected EnvironmentTestDto environmentAction(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient client) throws Exception {
        EnvironmentEditRequest request = createEditRequest();
        client.getDefaultClient(testContext)
                .environmentV1Endpoint()
                .editByCrn(testDto.getResponse().getCrn(), request);
        Log.when(LOGGER, "Environment modify proxy config action posted");
        return testDto;
    }

    private EnvironmentEditRequest createEditRequest() {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        ProxyRequest proxyRequest = new ProxyRequest();
        proxyRequest.setName(proxyConfigName);
        request.setProxy(proxyRequest);
        return request;
    }
}
