package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ProxyConfig.class)
public class ProxyV4Controller extends NotificationController implements ProxyV4Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public ProxyV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
        Set<ProxyConfig> allInWorkspaceAndEnvironment = proxyConfigService
                .findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal);
        return new ProxyV4Responses(converterUtil.convertAllAsSet(allInWorkspaceAndEnvironment, ProxyV4Response.class));
    }

    @Override
    public ProxyV4Response get(Long workspaceId, String name) {
        ProxyConfig config = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(config, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response post(Long workspaceId, ProxyV4Request request) {
        ProxyConfig config = converterUtil.convert(request, ProxyConfig.class);
        config = proxyConfigService.createInEnvironment(config, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return converterUtil.convert(config, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response delete(Long workspaceId, String name) {
        ProxyConfig deleted = proxyConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return converterUtil.convert(deleted, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<ProxyConfig> deleted = proxyConfigService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return new ProxyV4Responses(converterUtil.convertAllAsSet(deleted, ProxyV4Response.class));
    }

    @Override
    public ProxyV4Response attach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        return proxyConfigService.attachToEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response detach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        return proxyConfigService.detachFromEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Request getRequest(Long workspaceId, String name) {
        ProxyConfig proxyConfig = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(proxyConfig, ProxyV4Request.class);
    }
}
