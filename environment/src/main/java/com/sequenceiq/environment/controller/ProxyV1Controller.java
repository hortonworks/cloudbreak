package com.sequenceiq.environment.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.environment.api.EnvironmentNames;
import com.sequenceiq.environment.api.proxy.endpoint.ProxyV1Endpoint;
import com.sequenceiq.environment.api.proxy.model.request.ProxyV1Request;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Responses;
import com.sequenceiq.environment.domain.proxy.ProxyConfig;
import com.sequenceiq.environment.service.proxy.ProxyConfigService;

import ch.qos.logback.core.pattern.ConverterUtil;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ProxyConfig.class)
public class ProxyV1Controller implements ProxyV1Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public ProxyV1Responses list(String environment, Boolean attachGlobal) {
        Set<ProxyConfig> allInWorkspaceAndEnvironment = proxyConfigService
                .findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal);
        return new ProxyV1Responses(converterUtil.convertAllAsSet(allInWorkspaceAndEnvironment, ProxyV1Response.class));
    }

    @Override
    public ProxyV1Response get(String name) {
        ProxyConfig config = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(config, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Response post(ProxyV1Request request) {
        ProxyConfig config = converterUtil.convert(request, ProxyConfig.class);
        config = proxyConfigService.createInEnvironment(config, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return converterUtil.convert(config, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Response delete(String name) {
        ProxyConfig deleted = proxyConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return converterUtil.convert(deleted, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Responses deleteMultiple(Set<String> names) {
        Set<ProxyConfig> deleted = proxyConfigService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return new ProxyV1Responses(converterUtil.convertAllAsSet(deleted, ProxyV1Response.class));
    }

    @Override
    public ProxyV1Response attach(String name, EnvironmentNames environmentNames) {
        return proxyConfigService.attachToEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Response detach(String name, EnvironmentNames environmentNames) {
        return proxyConfigService.detachFromEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(),
                workspaceId, ProxyV1Response.class);
    }

    @Override
    public ProxyV1Request getRequest(String name) {
        ProxyConfig proxyConfig = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(proxyConfig, ProxyV1Request.class);
    }
}
