package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.ProxyConfigEndpoint;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class ProxyConfigController extends NotificationController implements ProxyConfigEndpoint {

    @Autowired
    private ProxyConfigService proxyConfigService;

    @Autowired
    private ProxyConfigMapper proxyConfigMapper;

    @Inject
    private UserService userService;

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
        return createInDefaultOrganization(request);
    }

    @Override
    public ProxyConfigResponse postPrivate(ProxyConfigRequest request) {
        return createInDefaultOrganization(request);
    }

    @Override
    public Set<ProxyConfigResponse> getPrivates() {
        return listForUsersDefaultOrganization();
    }

    @Override
    public Set<ProxyConfigResponse> getPublics() {
        return listForUsersDefaultOrganization();
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
        return deleteInDefaultOrganization(name);
    }

    @Override
    public ProxyConfigResponse deletePrivate(String name) {
        return deleteInDefaultOrganization(name);
    }

    private ProxyConfigResponse getProxyConfigResponse(String name) {
        return proxyConfigMapper.mapEntityToResponse(proxyConfigService.getByNameForOrganizationId(name, restRequestThreadLocalService.getRequestedOrgId()));
    }

    private Set<ProxyConfigResponse> listForUsersDefaultOrganization() {
        return proxyConfigService.findAllByOrganizationId(restRequestThreadLocalService.getRequestedOrgId()).stream()
                .map(config -> proxyConfigMapper.mapEntityToResponse(config))
                .collect(Collectors.toSet());
    }

    private ProxyConfigResponse deleteInDefaultOrganization(String name) {
        ProxyConfig config = proxyConfigService.deleteByNameFromOrganization(name, restRequestThreadLocalService.getRequestedOrgId());
        return notifyAndReturn(config, ResourceEvent.PROXY_CONFIG_DELETED);
    }

    private ProxyConfigResponse createInDefaultOrganization(ProxyConfigRequest request) {
        ProxyConfig proxyConfig = proxyConfigMapper.mapRequestToEntity(request);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        proxyConfig = proxyConfigService.create(proxyConfig, restRequestThreadLocalService.getRequestedOrgId(), user);
        return notifyAndReturn(proxyConfig, ResourceEvent.PROXY_CONFIG_CREATED);
    }

    private ProxyConfigResponse notifyAndReturn(ProxyConfig proxyConfig, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return proxyConfigMapper.mapEntityToResponse(proxyConfig);
    }
}
