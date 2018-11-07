package com.sequenceiq.periscope.service.security;


import javax.inject.Inject;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.CertificateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.service.secret.SecretService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;

@Service
public class TlsSecurityService {

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private SecretService secretService;

    public SecurityConfig prepareSecurityConfig(Long stackId) {
        CertificateResponse response = cloudbreakClient.autoscaleEndpoint().getCertificate(stackId);
        return new SecurityConfig(response.getClientKeyPath(), response.getClientCertPath(), response.getServerCert());
    }

    public TlsConfiguration getConfiguration(Cluster cluster) {
        SecurityConfig securityConfig = cluster.getSecurityConfig();
        if (securityConfig == null) {
            securityConfig = getSecurityConfigSilently(cluster);
        }
        if (securityConfig == null) {
            securityConfig = prepareSecurityConfig(cluster.getStackId());
        }
        String clientKey = new String(Base64.decode(secretService.get(securityConfig.getClientKey())));
        String clientCert = new String(Base64.decode(secretService.get(securityConfig.getClientCert())));
        String serverCert = new String(Base64.decode(securityConfig.getServerCert()));
        return new TlsConfiguration(clientKey, clientCert, serverCert);
    }

    private SecurityConfig getSecurityConfigSilently(Cluster cluster) {
        try {
            return securityConfigRepository.findByClusterId(cluster.getId());
        } catch (AccessDeniedException ignore) {
            return null;
        }
    }

}
