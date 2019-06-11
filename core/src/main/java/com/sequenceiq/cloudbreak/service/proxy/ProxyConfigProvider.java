package com.sequenceiq.cloudbreak.service.proxy;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Service
public class ProxyConfigProvider {

    public static final String PROXY_KEY = "proxy";

    public static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    public void decoratePillarWithProxyDataIfNeeded(Map<String, SaltPillarProperties> servicePillar, Cluster cluster) {
        String proxyConfigCrn = cluster.getProxyConfigCrn();
        if (StringUtils.isNotEmpty(proxyConfigCrn)) {
            ProxyConfig proxyConfig = proxyConfigDtoService.getByCrn(proxyConfigCrn);
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", proxyConfig.getServerHost());
            proxy.put("port", proxyConfig.getServerPort());
            proxy.put("protocol", proxyConfig.getProtocol());
            if (StringUtils.isNotBlank(proxyConfig.getUserName()) && StringUtils.isNotBlank(proxyConfig.getPassword())) {
                proxy.put("user", proxyConfig.getUserName());
                proxy.put("password", proxyConfig.getPassword());
            }
            servicePillar.put(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
        }
    }
}
