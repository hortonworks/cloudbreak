package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ProxyConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@Controller
@Transactional(TxType.NEVER)
public class ProxyConfigController extends NotificationController implements ProxyConfigEndpoint {

    @Autowired
    private ProxyConfigService proxyConfigService;

    @Autowired
    private ProxyConfigMapper proxyConfigMapper;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ProxyConfigResponse get(Long id) {
        return proxyConfigMapper.mapEntityToResponse(proxyConfigService.get(id));
    }

    @Override
    public ProxyConfigResponse delete(Long id) {
        ProxyConfig deleted = proxyConfigService.delete(id);
        notify(ResourceEvent.RECIPE_DELETED);
        return proxyConfigMapper.mapEntityToResponse(deleted);
    }

    @Override
    public ProxyConfigResponse postPublic(ProxyConfigRequest request) {
        return createInDefaultWorkspace(request);
    }

    @Override
    public ProxyConfigResponse postPrivate(ProxyConfigRequest request) {
        return createInDefaultWorkspace(request);
    }

    @Override
    public Set<ProxyConfigResponse> getPrivates() {
        return listForUsersDefaultWorkspace();
    }

    @Override
    public Set<ProxyConfigResponse> getPublics() {
        return listForUsersDefaultWorkspace();
    }

    @Override
    public ProxyConfigResponse getPrivate(String name) {
        return getProxyConfigResponse(name);
    }

    @Override
    public ProxyConfigResponse getPublic(String name) {
        return getProxyConfigResponse(name);
    }

    @Override
    public ProxyConfigResponse deletePublic(String name) {
        return deleteInDefaultWorkspace(name);
    }

    @Override
    public ProxyConfigResponse deletePrivate(String name) {
        return deleteInDefaultWorkspace(name);
    }

    private ProxyConfigResponse getProxyConfigResponse(String name) {
        return proxyConfigMapper.mapEntityToResponse(proxyConfigService.getByNameForWorkspaceId(name, restRequestThreadLocalService.getRequestedWorkspaceId()));
    }

    private Set<ProxyConfigResponse> listForUsersDefaultWorkspace() {
        return proxyConfigService.findAllByWorkspaceId(restRequestThreadLocalService.getRequestedWorkspaceId()).stream()
                .map(config -> proxyConfigMapper.mapEntityToResponse(config))
                .collect(Collectors.toSet());
    }

    private ProxyConfigResponse deleteInDefaultWorkspace(String name) {
        ProxyConfig config = proxyConfigService.deleteByNameFromWorkspace(name, restRequestThreadLocalService.getRequestedWorkspaceId());
        return notifyAndReturn(config, ResourceEvent.PROXY_CONFIG_DELETED);
    }

    private ProxyConfigResponse createInDefaultWorkspace(ProxyConfigRequest request) {
        ProxyConfig proxyConfig = proxyConfigMapper.mapRequestToEntity(request);
        proxyConfig = proxyConfigService.createInEnvironment(proxyConfig, request.getEnvironments(),
                restRequestThreadLocalService.getRequestedWorkspaceId());
        return notifyAndReturn(proxyConfig, ResourceEvent.PROXY_CONFIG_CREATED);
    }

    private ProxyConfigResponse notifyAndReturn(ProxyConfig proxyConfig, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return proxyConfigMapper.mapEntityToResponse(proxyConfig);
    }
}
