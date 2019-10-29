package com.sequenceiq.periscope.service.security;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.TlsConfiguration;

@Service
public class TlsSecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSecurityService.class);

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private SecretService secretService;

    @PostConstruct
    public void init() {
        LOGGER.info("init TlsSecurityService");
    }

    @Cacheable(cacheNames = "tlsConfigurationCache")
    public TlsConfiguration getTls(Long clusterId) {
        LOGGER.info("Get TlsConfiguration for clusterId: {}", clusterId);
        SecurityConfig securityConfig = securityConfigService.getSecurityConfig(clusterId);
        return createTls(securityConfig);
    }

    private TlsConfiguration createTls(SecurityConfig securityConfig) {
        String clientKey = new String(Base64.decode(secretService.get(securityConfig.getClientKey())));
        String clientCert = new String(Base64.decode(secretService.get(securityConfig.getClientCert())));
        String serverCert = new String(Base64.decode(securityConfig.getServerCert()));
        return new TlsConfiguration(clientKey, clientCert, serverCert);
    }
}
