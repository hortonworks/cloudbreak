package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotEmpty;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Request;
import com.sequenceiq.cloudbreak.controller.NotificationController;
import com.sequenceiq.cloudbreak.domain.Recipe;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Responses.proxyV4Responses;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ProxyConfig.class)
public class ProxyConfigV4Controller extends NotificationController implements ProxyConfigV4Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public ProxyV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
        Set<ProxyV4Response> proxies = proxyConfigService.findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal).stream()
                .map(config -> conversionService.convert(config, ProxyV4Response.class))
                .collect(Collectors.toSet());
        return proxyV4Responses(proxies);
    }

    @Override
    public ProxyV4Response get(Long workspaceId, String name) {
        ProxyConfig config = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(config, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response post(Long workspaceId, ProxyV4Request request) {
        ProxyConfig config = conversionService.convert(request, ProxyConfig.class);
        config = proxyConfigService.createInEnvironment(config, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return conversionService.convert(config, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response delete(Long workspaceId, String name) {
        ProxyConfig deleted = proxyConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return conversionService.convert(deleted, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response attach(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return proxyConfigService.attachToEnvironmentsAndConvert(name, environmentNames, workspaceId, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Response detach(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return proxyConfigService.detachFromEnvironmentsAndConvert(name, environmentNames, workspaceId, ProxyV4Response.class);
    }

    @Override
    public ProxyV4Request getRequest(Long workspaceId, String name) {
        ProxyConfig proxyConfig = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(proxyConfig, ProxyV4Request.class);
    }
}
