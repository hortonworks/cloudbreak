package com.sequenceiq.periscope.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.proxy.ApplicationProxyConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.security.TlsSecurityService;

@Service
public class AmbariClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClientProvider.class);

    @Autowired
    private TlsSecurityService tlsSecurityService;

    @Autowired
    private ApplicationProxyConfig proxyConfig;

    public AmbariClient createAmbariClient(Cluster cluster) {
        if (cluster.getStackId() != null) {
            TlsConfiguration tlsConfig = tlsSecurityService.getConfiguration(cluster);
            if (proxyConfig.isUseProxyForClusterConnection()) {
                String proxyHost = proxyConfig.getHttpsProxyHost();
                int proxyPort = proxyConfig.getHttpsProxyPort();
                LOGGER.info("Create Ambari client to connect to {}:{}, through proxy: {}:{}", cluster.getHost(), cluster.getPort(), proxyHost, proxyPort);
                return new AmbariClient(cluster.getHost(), cluster.getPort(), cluster.getAmbariUser(), cluster.getAmbariPass(), tlsConfig.getClientCert(),
                    tlsConfig.getClientKey(), tlsConfig.getServerCert(), proxyHost, proxyPort);
            } else {
                LOGGER.info("Create Ambari client to connect to {}:{}", cluster.getHost(), cluster.getPort());
                return new AmbariClient(cluster.getHost(), cluster.getPort(), cluster.getAmbariUser(), cluster.getAmbariPass(),
                    tlsConfig.getClientCert(), tlsConfig.getClientKey(), tlsConfig.getServerCert());
            }
        } else {
            return getAmbariClientForNonCloudbreakCluster(cluster);
        }
    }

    private AmbariClient getAmbariClientForNonCloudbreakCluster(Cluster cluster) {
        if (proxyConfig.isUseProxyForClusterConnection()) {
            String proxyHost = proxyConfig.getHttpsProxyHost();
            int proxyPort = proxyConfig.getHttpsProxyPort();
            LOGGER.info("Create Ambari client to connect to non Cloudbreak cluster {}:{}, through proxy: {}:{}",
                cluster.getHost(), cluster.getPort(), proxyHost, proxyPort);
            return new AmbariClient(cluster.getHost(), cluster.getPort(), cluster.getAmbariUser(),
                cluster.getAmbariPass(), null, null, null, proxyHost, proxyPort);
        } else {
            LOGGER.info("Create Ambari client to connect to non Cloudbreak cluster {}:{}", cluster.getHost(), cluster.getPort());
            return new AmbariClient(cluster.getHost(), cluster.getPort(), cluster.getAmbariUser(), cluster.getAmbariPass());
        }
    }
}
