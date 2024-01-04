package com.sequenceiq.cloudbreak.service.proxy;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;

@Service
public class ProxyConfigUserDataReplacer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigUserDataReplacer.class);

    @Inject
    private ProxyConfigDtoService proxyConfigDtoService;

    public String replaceProxyConfigInUserDataByEnvCrn(String userData, String envCrn) {
        Optional<ProxyConfig> proxyConfigOptional = proxyConfigDtoService.getByEnvironmentCrn(envCrn);
        LOGGER.info("Replacing proxy config in user data to {}", proxyConfigOptional.map(ProxyConfig::getCrn).orElse(null));
        Optional<ProxyAuthentication> proxyConfigOptionalAuth = proxyConfigOptional.flatMap(ProxyConfig::getProxyAuthentication);
        return new UserDataReplacer(userData)
                .replace("IS_PROXY_ENABLED", proxyConfigOptional.isPresent())
                .replace("PROXY_HOST", proxyConfigOptional.map(ProxyConfig::getServerHost).orElse(null))
                .replace("PROXY_PORT", proxyConfigOptional.map(ProxyConfig::getServerPort).orElse(null))
                .replace("PROXY_PROTOCOL", proxyConfigOptional.map(ProxyConfig::getProtocol).orElse(null))
                .replaceQuoted("PROXY_USER", proxyConfigOptionalAuth.map(ProxyAuthentication::getUserName).orElse(null))
                .replaceQuoted("PROXY_PASSWORD", proxyConfigOptionalAuth.map(ProxyAuthentication::getPassword).orElse(null))
                .replaceQuoted("PROXY_NO_PROXY_HOSTS", proxyConfigOptional.map(ProxyConfig::getNoProxyHosts).orElse(null))
                .getUserData();
    }
}
