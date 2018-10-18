package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotEmpty;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ProxyConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(ProxyConfig.class)
public class ProxyConfigV3Controller extends NotificationController implements ProxyConfigV3Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<ProxyConfigResponse> listByWorkspace(Long workspaceId, String environment, Boolean attachGlobal) {
        return proxyConfigService.findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal).stream()
                .map(config -> conversionService.convert(config, ProxyConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public ProxyConfigResponse getByNameInWorkspace(Long workspaceId, String name) {
        ProxyConfig config = proxyConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(config, ProxyConfigResponse.class);
    }

    @Override
    public ProxyConfigResponse createInWorkspace(Long workspaceId, ProxyConfigRequest request) {
        ProxyConfig config = conversionService.convert(request, ProxyConfig.class);
        config = proxyConfigService.createInEnvironment(config, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return conversionService.convert(config, ProxyConfigResponse.class);
    }

    @Override
    public ProxyConfigResponse deleteInWorkspace(Long workspaceId, String name) {
        ProxyConfig deleted = proxyConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return conversionService.convert(deleted, ProxyConfigResponse.class);
    }

    @Override
    public ProxyConfigResponse attachToEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return null;
    }

    @Override
    public ProxyConfigResponse detachFromEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return null;
    }
}
