package com.sequenceiq.periscope.service.security;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@Service
public class TlsHttpClientConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsHttpClientConfigurationService.class);

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Value("${clusterProxy.disabledPlatforms}")
    private Set<String> clusterProxyDisabledPlatforms;

    public HttpClientConfig buildTLSClientConfig(Cluster cluster) {
        LOGGER.debug("Building HttpClientConfig for stackCrn: {}, host: {}", cluster.getStackCrn(), cluster.getClusterManager().getHost());
        TlsConfiguration tlsConfiguration = tlsSecurityService.getTls(cluster.getId());
        Optional<String> clusterProxyUrl = Optional.empty();
        if (isClusterProxyApplicable(cluster.getCloudPlatform())) {
            clusterProxyUrl = clusterProxyConfigurationService.getClusterProxyUrl();
        }
        HttpClientConfig httpClientConfig =
                new HttpClientConfig(cluster.getClusterManager().getHost(), tlsConfiguration.getServerCert(),
                        tlsConfiguration.getClientCert(), tlsConfiguration.getClientKey());
        if (clusterProxyUrl.isPresent()) {
            httpClientConfig.withClusterProxy(clusterProxyUrl.get(), cluster.getStackCrn());
        }
        return httpClientConfig;
    }

    public boolean isClusterProxyApplicable(String cloudPlatform) {
        return !clusterProxyDisabledPlatforms.contains(cloudPlatform);
    }
}
