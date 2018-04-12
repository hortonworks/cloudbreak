package com.sequenceiq.cloudbreak.service.decorator;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class ClusterProxyDecorator {

    @Inject
    private ProxyConfigService proxyConfigService;

    public Cluster prepareProxyConfig(Cluster subject, IdentityUser user, String proxyName, Stack stack) {
        if (StringUtils.isNotBlank(proxyName)) {
            ProxyConfig proxyConfig = proxyConfigService.getPublicProxyConfig(proxyName, user);
            subject.setProxyConfig(proxyConfig);
        }
        return subject;
    }
}
