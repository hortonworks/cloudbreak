package com.sequenceiq.environment.proxy;

import static com.sequenceiq.environment.TempConstants.TEMP_ACCOUNT_ID;
import static com.sequenceiq.environment.TempConstants.TEMP_WORKSPACE_ID;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.environment.api.EnvironmentNames;
import com.sequenceiq.environment.api.proxy.endpoint.ProxyV1Endpoint;
import com.sequenceiq.environment.api.proxy.model.request.ProxyV1Request;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Responses;
import com.sequenceiq.environment.proxy.converter.ProxyConfigToProxyV1RequestConverter;
import com.sequenceiq.environment.proxy.converter.ProxyConfigToProxyV1ResponseConverter;
import com.sequenceiq.environment.proxy.converter.ProxyV1RequestToProxyConfigConverter;
import com.sequenceiq.notification.NotificationController;
import com.sequenceiq.notification.ResourceEvent;

@Controller
@Transactional(TxType.NEVER)
public class ProxyV1Controller extends NotificationController implements ProxyV1Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private ProxyConfigToProxyV1RequestConverter proxyConfigToProxyV1RequestConverter;

    @Inject
    private ProxyConfigToProxyV1ResponseConverter proxyConfigToProxyV1ResponseConverter;

    @Inject
    private ProxyV1RequestToProxyConfigConverter proxyV1RequestToProxyConfigConverter;

    // TODO: implement these

    @Override
    public ProxyV1Responses list(String environment, Boolean attachGlobal) {
        Set<ProxyConfig> allInWorkspaceAndEnvironment = Set.of();
        return new ProxyV1Responses(new HashSet<>(proxyConfigToProxyV1ResponseConverter.convert(allInWorkspaceAndEnvironment)));
    }

    @Override
    public ProxyV1Response get(String name) {
        ProxyConfig config = new ProxyConfig();
        return proxyConfigToProxyV1ResponseConverter.convert(config);
    }

    @Override
    public ProxyV1Response post(ProxyV1Request request) {
        ProxyConfig config = new ProxyConfig();
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigToProxyV1ResponseConverter.convert(config);
    }

    @Override
    public ProxyV1Response delete(String name) {
        ProxyConfig deleted = new ProxyConfig();
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyConfigToProxyV1ResponseConverter.convert(deleted);
    }

    @Override
    public ProxyV1Responses deleteMultiple(Set<String> names) {
        Set<ProxyConfig> deleted = Set.of();
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        Set<ProxyV1Response> responses = new HashSet<>(proxyConfigToProxyV1ResponseConverter.convert(deleted));
        return new ProxyV1Responses(responses);
    }

    @Override
    public ProxyV1Response attach(String name, EnvironmentNames environmentNames) {
        return proxyConfigService.attachToEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                TEMP_WORKSPACE_ID, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Response detach(String name, EnvironmentNames environmentNames) {
        return proxyConfigService.detachFromEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                TEMP_WORKSPACE_ID, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Request getRequest(String name) {
        ProxyConfig proxyConfig = proxyConfigService.getByNameForAccountId(name, TEMP_ACCOUNT_ID);
        return proxyConfigToProxyV1RequestConverter.convert(proxyConfig);
    }
}
