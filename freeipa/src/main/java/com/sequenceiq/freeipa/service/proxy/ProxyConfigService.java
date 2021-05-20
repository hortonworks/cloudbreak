package com.sequenceiq.freeipa.service.proxy;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;

@Service
public class ProxyConfigService {

    private static final String PROXY_KEY = "proxy";

    private static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    public Map<String, SaltPillarProperties> createProxyPillarConfig(String environmentCrn) {
        Optional<ProxyConfig> proxyConfig = proxyConfigDtoService.getByEnvironmentCrn(environmentCrn);
        if (proxyConfig.isPresent()) {
            ProxyConfig config = proxyConfig.get();
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", config.getServerHost());
            proxy.put("port", config.getServerPort());
            proxy.put("protocol", config.getProtocol());
            config.getProxyAuthentication().ifPresent(auth -> {
                proxy.put("user", auth.getUserName());
                proxy.put("password", auth.getPassword());
            });
            proxy.put("noProxyHosts", config.getNoProxyHosts());
            return Map.of(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
        } else {
            return Map.of();
        }
    }
}
