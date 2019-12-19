package com.sequenceiq.periscope.service.security;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.repository.ClusterRepository;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@Service
public class TlsHttpClientConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsHttpClientConfigurationService.class);

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    public HttpClientConfig buildTLSClientConfig(String stackCrn, String host, Tunnel tunnel) {
        LOGGER.info("Building HttpClientConfig for stackCrn: {}, host: {}", stackCrn, host);
        Long clusterId = clusterRepository.findIdStackCrn(stackCrn);
        TlsConfiguration tlsConfiguration = tlsSecurityService.getTls(clusterId);
        Optional<String> clusterProxyUrl = clusterProxyConfigurationService.getClusterProxyUrl();
        HttpClientConfig httpClientConfig =
                new HttpClientConfig(host, tlsConfiguration.getServerCert(), tlsConfiguration.getClientCert(), tlsConfiguration.getClientKey());
        if (clusterProxyUrl.isPresent() && tunnel.useClusterProxy()) {
            httpClientConfig.withClusterProxy(clusterProxyUrl.get(), stackCrn);
        }
        return httpClientConfig;
    }
}
