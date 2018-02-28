package com.sequenceiq.cloudbreak.service.decorator;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;

@Service
public class ClusterProxyDecorator {

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private ProxyConfigMapper mapper;

    public Cluster prepareProxyConfig(Cluster subject, IdentityUser user, Long proxyConfigId, ProxyConfigRequest proxyConfigRequest, Stack stack) {
        if (proxyConfigId != null) {
            ProxyConfig proxyConfig = proxyConfigService.get(proxyConfigId);
            subject.setProxyConfig(proxyConfig);
        } else if (proxyConfigRequest != null) {
            ProxyConfig proxyConfig = mapper.mapRequestToEntity(proxyConfigRequest, stack.isPublicInAccount());
            proxyConfig = proxyConfigService.create(user, proxyConfig);
            subject.setProxyConfig(proxyConfig);
        }
        return subject;
    }
}
