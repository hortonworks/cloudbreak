package com.sequenceiq.periscope.service.security;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.repository.ClusterRepository;

@Service
public class TlsHttpClientConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsHttpClientConfigurationService.class);

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public HttpClientConfig buildTLSClientConfig(String stackCrn, String host) {
        LOGGER.info("Building HttpClientConfig for stackCrn: {}, host: {}", stackCrn, host);
        Long clusterId = clusterRepository.findIdStackCrn(stackCrn);
        TlsConfiguration tlsConfiguration = tlsSecurityService.getTls(clusterId);
        return new HttpClientConfig(host, tlsConfiguration.getServerCert(), tlsConfiguration.getClientCert(), tlsConfiguration.getClientKey());
    }
}
