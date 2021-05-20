package com.sequenceiq.cloudbreak.service.proxy;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Service
public class ProxyConfigProvider {
    public static final String PROXY_KEY = "proxy";

    public static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigProvider.class);

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    public void decoratePillarWithProxyDataIfNeeded(Map<String, SaltPillarProperties> servicePillar, Cluster cluster) {
        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByCrnWithEnvironmentFallback(cluster.getProxyConfigCrn(), cluster.getEnvironmentCrn());
        proxyConfig.ifPresent(pc -> {
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", pc.getServerHost());
            proxy.put("port", pc.getServerPort());
            proxy.put("protocol", pc.getProtocol());
            pc.getProxyAuthentication().ifPresent(auth -> {
                proxy.put("user", auth.getUserName());
                proxy.put("password", auth.getPassword());
            });
            proxy.put("noProxyHosts", pc.getNoProxyHosts());
            servicePillar.put(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
            LOGGER.info("Salt pillar properties extend with proxy config: {}", pc);
        });
    }
}
