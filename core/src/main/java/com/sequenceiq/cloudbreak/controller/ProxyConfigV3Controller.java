package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ProxyConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class ProxyConfigV3Controller extends NotificationController implements ProxyConfigV3Endpoint {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<ProxyConfigResponse> listByOrganization(Long organizationId) {
        return proxyConfigService.findAllByOrganizationId(organizationId).stream()
                .map(config -> conversionService.convert(config, ProxyConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public ProxyConfigResponse getByNameInOrganization(Long organizationId, String name) {
        ProxyConfig config = proxyConfigService.getByNameForOrganizationId(name, organizationId);
        return conversionService.convert(config, ProxyConfigResponse.class);
    }

    @Override
    public ProxyConfigResponse createInOrganization(Long organizationId, ProxyConfigRequest request) {
        ProxyConfig config = conversionService.convert(request, ProxyConfig.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        config = proxyConfigService.create(config, organizationId, user);
        notify(ResourceEvent.PROXY_CONFIG_CREATED);
        return conversionService.convert(config, ProxyConfigResponse.class);
    }

    @Override
    public ProxyConfigResponse deleteInOrganization(Long organizationId, String name) {
        ProxyConfig deleted = proxyConfigService.deleteByNameFromOrganization(name, organizationId);
        notify(ResourceEvent.PROXY_CONFIG_DELETED);
        return conversionService.convert(deleted, ProxyConfigResponse.class);
    }
}
