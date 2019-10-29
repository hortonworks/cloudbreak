package com.sequenceiq.periscope.service;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.proxy.ApplicationProxyConfig;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.model.TlsConfiguration;
import com.sequenceiq.periscope.service.security.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Service
public class AmbariClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClientProvider.class);

    @Autowired
    private TlsSecurityService tlsSecurityService;

    @Autowired
    private ApplicationProxyConfig proxyConfig;

    @Inject
    private SecretService secretService;

    public AmbariClient createAmbariClient(Cluster cluster) {
        String ambariUser = secretService.get(cluster.getClusterManagerUser());
        String ambariPass = secretService.get(cluster.getClusterManagerPassword());
        if (cluster.getStackCrn() != null) {
            TlsConfiguration tlsConfig = tlsSecurityService.getTls(cluster.getId());
            if (proxyConfig.isUseProxyForClusterConnection()) {
                String proxyHost = proxyConfig.getHttpsProxyHost();
                int proxyPort = proxyConfig.getHttpsProxyPort();
                if (proxyConfig.isProxyAuthRequired()) {
                    String proxyUser = proxyConfig.getHttpsProxyUser();
                    String proxyPassword = proxyConfig.getHttpsProxyPassword();
                    LOGGER.debug("Create Ambari client to connect to {}:{}, through proxy: {}:{} with proxy user: {}",
                            cluster.getHost(), cluster.getPort(), proxyHost, proxyPort, proxyUser);
                    return new AmbariClient(cluster.getHost(), cluster.getPort(), ambariUser, ambariPass, tlsConfig.getClientCert(),
                            tlsConfig.getClientKey(), tlsConfig.getServerCert(), proxyHost, proxyPort, proxyUser, proxyPassword);
                } else {
                    LOGGER.debug("Create Ambari client to connect to {}:{}, through proxy: {}:{}", cluster.getHost(), cluster.getPort(), proxyHost, proxyPort);
                    return new AmbariClient(cluster.getHost(), cluster.getPort(), ambariUser, ambariPass, tlsConfig.getClientCert(),
                            tlsConfig.getClientKey(), tlsConfig.getServerCert(), proxyHost, proxyPort);
                }
            } else {
                LOGGER.debug("Create Ambari client to connect to {}:{}", cluster.getHost(), cluster.getPort());
                return new AmbariClient(cluster.getHost(), cluster.getPort(), ambariUser, ambariPass,
                        tlsConfig.getClientCert(), tlsConfig.getClientKey(), tlsConfig.getServerCert());
            }
        } else {
            return getAmbariClientForNonCloudbreakCluster(cluster, ambariUser, ambariPass);
        }
    }

    private AmbariClient getAmbariClientForNonCloudbreakCluster(Cluster cluster, String ambariUser, String ambariPass) {
        if (proxyConfig.isUseProxyForClusterConnection()) {
            String proxyHost = proxyConfig.getHttpsProxyHost();
            int proxyPort = proxyConfig.getHttpsProxyPort();
            if (proxyConfig.isProxyAuthRequired()) {
                String proxyUser = proxyConfig.getHttpsProxyUser();
                String proxyPassword = proxyConfig.getHttpsProxyPassword();
                LOGGER.debug("Create Ambari client to connect to non Cloudbreak cluster {}:{}, through proxy: {}:{} with proxy user: {}",
                        cluster.getHost(), cluster.getPort(), proxyHost, proxyPort, proxyUser);
                return new AmbariClient(cluster.getHost(), cluster.getPort(), ambariUser,
                        ambariPass, null, null, null, proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                LOGGER.debug("Create Ambari client to connect to non Cloudbreak cluster {}:{}, through proxy: {}:{}",
                        cluster.getHost(), cluster.getPort(), proxyHost, proxyPort);
                return new AmbariClient(cluster.getHost(), cluster.getPort(), ambariUser,
                        ambariPass, null, null, null, proxyHost, proxyPort);
            }
        } else {
            LOGGER.debug("Create Ambari client to connect to non Cloudbreak cluster {}:{}", cluster.getHost(), cluster.getPort());
            return new AmbariClient(cluster.getHost(), cluster.getPort(), ambariUser, ambariPass);
        }
    }
}
