package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.response.CertificateV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.SecurityConfig;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.repository.SecurityConfigRepository;

@Service
public class TlsSecurityService {

    @Inject
    private CloudbreakInternalCrnClient internalCrnClient;

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private SecretService secretService;

    public SecurityConfig prepareSecurityConfig(String stackCrn) {
        CertificateV4Response response = internalCrnClient.withInternalCrn().autoscaleEndpoint().getCertificate(stackCrn);
        return new SecurityConfig(response.getClientKeyPath(), response.getClientCertPath(), response.getServerCert());
    }

    public TlsConfiguration getConfiguration(Cluster cluster) {
        SecurityConfig securityConfig = cluster.getSecurityConfig();
        if (securityConfig == null) {
            securityConfig = getSecurityConfigSilently(cluster);
        }
        if (securityConfig == null) {
            securityConfig = prepareSecurityConfig(cluster.getStackCrn());
        }
        String clientKey = new String(Base64.decode(secretService.get(securityConfig.getClientKey())));
        String clientCert = new String(Base64.decode(secretService.get(securityConfig.getClientCert())));
        String serverCert = new String(Base64.decode(securityConfig.getServerCert()));
        return new TlsConfiguration(clientKey, clientCert, serverCert);
    }

    public HttpClientConfig buildTLSClientConfig(Cluster cluster) {
        SecurityConfig securityConfig = cluster.getSecurityConfig();
        if (securityConfig == null) {
            securityConfig = getSecurityConfigSilently(cluster);
        }
        if (securityConfig == null) {
            securityConfig = prepareSecurityConfig(cluster.getStackCrn());
        }
        String clientKey = new String(Base64.decode(secretService.get(securityConfig.getClientKey())));
        String clientCert = new String(Base64.decode(secretService.get(securityConfig.getClientCert())));
        String serverCert = new String(Base64.decode(securityConfig.getServerCert()));
        return new HttpClientConfig(cluster.getClusterManager().getHost(), serverCert, clientCert, clientKey);
    }

    private SecurityConfig getSecurityConfigSilently(Cluster cluster) {
        return securityConfigRepository.findByClusterId(cluster.getId());
    }

}
