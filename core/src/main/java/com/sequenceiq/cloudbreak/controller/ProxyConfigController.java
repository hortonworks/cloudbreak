package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ProxyConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@Component
public class ProxyConfigController extends NotificationController implements ProxyConfigEndpoint {

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Autowired
    private ProxyConfigService proxyConfigService;

    @Autowired
    private ProxyConfigMapper proxyConfigMapper;

    @Override
    public ProxyConfigResponse get(Long id) {
        ProxyConfig proxyConfig = proxyConfigService.get(id);
        return proxyConfigMapper.mapEntityToResponse(proxyConfig);
    }

    @Override
    public void delete(Long id) {
        executeAndNotify(user -> proxyConfigService.delete(id, user), ResourceEvent.PROXY_CONFIG_DELETED);
    }

    @Override
    public ProxyConfigResponse postPrivate(ProxyConfigRequest proxyConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createProxyConfig(user, proxyConfigRequest, false);
    }

    @Override
    public Set<ProxyConfigResponse> getPrivates() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<ProxyConfig> proxyConfigs = proxyConfigService.retrievePrivateProxyConfigs(user);
        return proxyConfigMapper.mapEntityToResponse(proxyConfigs);
    }

    @Override
    public ProxyConfigResponse getPrivate(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ProxyConfig proxyConfig = proxyConfigService.getPrivateProxyConfig(name, user);
        return proxyConfigMapper.mapEntityToResponse(proxyConfig);
    }

    @Override
    public void deletePrivate(String name) {
        executeAndNotify(user -> proxyConfigService.delete(name, user), ResourceEvent.PROXY_CONFIG_DELETED);
    }

    @Override
    public ProxyConfigResponse postPublic(ProxyConfigRequest proxyConfigRequest) {
        IdentityUser user = authenticatedUserService.getCbUser();
        return createProxyConfig(user, proxyConfigRequest, true);
    }

    @Override
    public Set<ProxyConfigResponse> getPublics() {
        IdentityUser user = authenticatedUserService.getCbUser();
        Set<ProxyConfig> proxyConfigs = proxyConfigService.retrieveAccountProxyConfigs(user);
        return proxyConfigMapper.mapEntityToResponse(proxyConfigs);
    }

    @Override
    public ProxyConfigResponse getPublic(String name) {
        IdentityUser user = authenticatedUserService.getCbUser();
        ProxyConfig proxyConfig = proxyConfigService.getPublicProxyConfig(name, user);
        return proxyConfigMapper.mapEntityToResponse(proxyConfig);
    }

    @Override
    public void deletePublic(String name) {
        executeAndNotify(user -> proxyConfigService.delete(name, user), ResourceEvent.PROXY_CONFIG_DELETED);
    }

    private ProxyConfigResponse createProxyConfig(IdentityUser user, ProxyConfigRequest request, boolean publicInAccount) {
        ProxyConfig proxyConfig = proxyConfigMapper.mapRequestToEntity(request, publicInAccount);
        proxyConfig = proxyConfigService.create(user, proxyConfig);
        notify(user, ResourceEvent.PROXY_CONFIG_CREATED);
        return proxyConfigMapper.mapEntityToResponse(proxyConfig);
    }
}
