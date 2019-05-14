package com.sequenceiq.environment.proxy;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.cloudbreak.workspace.service.WorkspaceService;
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
@WorkspaceEntityType(ProxyConfig.class)
public class ProxyV1Controller extends NotificationController implements ProxyV1Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private ProxyConfigToProxyV1RequestConverter proxyConfigToProxyV1RequestConverter;

    @Inject
    private ProxyConfigToProxyV1ResponseConverter proxyConfigToProxyV1ResponseConverter;

    @Inject
    private ProxyV1RequestToProxyConfigConverter proxyV1RequestToProxyConfigConverter;

    @Inject
    private WorkspaceService workspaceService;

    @Override
    public ProxyV1Responses list(String environment, Boolean attachGlobal) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        Set<ProxyConfig> allInWorkspaceAndEnvironment = proxyConfigService
                .findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal);
        return new ProxyV1Responses(new HashSet<>(proxyConfigToProxyV1ResponseConverter.convert(allInWorkspaceAndEnvironment)));
    }

    @Override
    public ProxyV1Response get(String name) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        ProxyConfig config = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return proxyConfigToProxyV1ResponseConverter.convert(config);
    }

    @Override
    public ProxyV1Response post(ProxyV1Request request) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        ProxyConfig config = proxyV1RequestToProxyConfigConverter.convert(request);
        config = proxyConfigService.createInEnvironment(config, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigToProxyV1ResponseConverter.convert(config);
    }

    @Override
    public ProxyV1Response delete(String name) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        ProxyConfig deleted = proxyConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return proxyConfigToProxyV1ResponseConverter.convert(deleted);
    }

    @Override
    public ProxyV1Responses deleteMultiple(Set<String> names) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        Set<ProxyConfig> deleted = proxyConfigService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        Set<ProxyV1Response> responses = new HashSet<>(proxyConfigToProxyV1ResponseConverter.convert(deleted));
        return new ProxyV1Responses(responses);
    }

    @Override
    public ProxyV1Response attach(String name, EnvironmentNames environmentNames) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        return proxyConfigService.attachToEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Response detach(String name, EnvironmentNames environmentNames) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        return proxyConfigService.detachFromEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Request getRequest(String name) {
        Long workspaceId = workspaceService.getDefaultWorkspace().getId();
        ProxyConfig proxyConfig = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return proxyConfigToProxyV1RequestConverter.convert(proxyConfig);
    }
}
