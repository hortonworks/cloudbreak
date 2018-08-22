package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Component
public class ProxyConfigRequestToProxyConfigConverter extends AbstractConversionServiceAwareConverter<ProxyConfigRequest, ProxyConfig> {

    @Inject
    private OrganizationService organizationService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ProxyConfig convert(ProxyConfigRequest source) {
        ProxyConfig proxyConfig = new ProxyConfig();
        Long orgId = restRequestThreadLocalService.getRequestedOrgId();
        if (orgId == null) {
            orgId = organizationService.getDefaultOrganizationForCurrentUser().getId();
        }
        proxyConfig.setOrganization(organizationService.get(orgId));
        proxyConfig.setName(source.getName());
        proxyConfig.setPassword(source.getPassword());
        proxyConfig.setDescription(source.getDescription());
        proxyConfig.setProtocol(source.getProtocol());
        proxyConfig.setServerHost(source.getServerHost());
        proxyConfig.setServerPort(source.getServerPort());
        proxyConfig.setUserName(source.getUserName());
        return proxyConfig;
    }
}
