package com.sequenceiq.cloudbreak.service.proxy;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Service
public class ProxyConfigProvider {

    public static final String PROXY_KEY = "proxy";

    public static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

    public void decoratePillarWithProxyDataIfNeeded(Map<String, SaltPillarProperties> servicePillar, Cluster cluster) {
        ProxyConfig proxyConfig = cluster.getProxyConfig();
        if (proxyConfig != null) {
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", proxyConfig.getServerHost());
            proxy.put("port", proxyConfig.getServerPort());
            proxy.put("protocol", proxyConfig.getProtocol());
            if (StringUtils.isNotBlank(proxyConfig.getUserName().getRaw()) && StringUtils.isNotBlank(proxyConfig.getPassword().getRaw())) {
                proxy.put("user", proxyConfig.getUserName().getRaw());
                proxy.put("password", proxyConfig.getPassword().getRaw());
            }
            servicePillar.put(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
        }
    }
}
