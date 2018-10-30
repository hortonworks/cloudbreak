package com.sequenceiq.cloudbreak.service.proxy;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.vault.VaultService;

@Service
public class ProxyConfigProvider {

    public static final String PROXY_KEY = "proxy";

    public static final String PROXY_SLS_PATH = "/proxy/proxy.sls";

    @Inject
    private TransactionService transactionService;

    @Inject
    private VaultService vaultService;

    public void decoratePillarWithProxyDataIfNeeded(Map<String, SaltPillarProperties> servicePillar, Cluster cluster) {
        ProxyConfig proxyConfig = cluster.getProxyConfig();
        if (proxyConfig != null) {
            Map<String, Object> proxy = new HashMap<>();
            proxy.put("host", proxyConfig.getServerHost());
            proxy.put("port", proxyConfig.getServerPort());
            proxy.put("protocol", proxyConfig.getProtocol());
            if (StringUtils.isNotBlank(proxyConfig.getUserName()) && StringUtils.isNotBlank(proxyConfig.getPassword())) {
                proxy.put("user", vaultService.resolveSingleValue(proxyConfig.getUserName()));
                proxy.put("password", vaultService.resolveSingleValue(proxyConfig.getPassword()));
            }
            servicePillar.put(PROXY_KEY, new SaltPillarProperties(PROXY_SLS_PATH, singletonMap(PROXY_KEY, proxy)));
        }
    }
}
