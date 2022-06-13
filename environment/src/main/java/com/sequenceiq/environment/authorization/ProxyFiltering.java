package com.sequenceiq.environment.authorization;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponses;
import com.sequenceiq.environment.proxy.service.ProxyConfigService;
import com.sequenceiq.environment.proxy.v1.converter.ProxyConfigToProxyResponseConverter;

@Component
public class ProxyFiltering extends AbstractAuthorizationFiltering<ProxyResponses> {

    private final ProxyConfigService proxyConfigService;

    private final ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter;

    public ProxyFiltering(ProxyConfigService proxyConfigService,
            ProxyConfigToProxyResponseConverter proxyConfigToProxyResponseConverter) {
        this.proxyConfigService = proxyConfigService;
        this.proxyConfigToProxyResponseConverter = proxyConfigToProxyResponseConverter;
    }

    @Override
    protected List<ResourceWithId> getAllResources(Map<String, Object> args) {
        return proxyConfigService.getProxyResources();
    }

    @Override
    protected ProxyResponses filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return new ProxyResponses(proxyConfigService.listInAccount(ThreadBasedUserCrnProvider.getAccountId())
                .stream()
                .filter(proxy -> authorizedResourceIds.contains(proxy.getId()))
                .map(proxy -> proxyConfigToProxyResponseConverter.convert(proxy))
                .collect(Collectors.toSet()));
    }

    @Override
    protected ProxyResponses getAll(Map<String, Object> args) {
        return new ProxyResponses(proxyConfigService.listInAccount(ThreadBasedUserCrnProvider.getAccountId()).stream()
                .map(proxy -> proxyConfigToProxyResponseConverter.convert(proxy))
                .collect(Collectors.toSet()));
    }
}
