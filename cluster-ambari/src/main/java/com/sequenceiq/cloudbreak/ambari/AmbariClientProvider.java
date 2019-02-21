package com.sequenceiq.cloudbreak.ambari;

import java.net.URL;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.proxy.ApplicationProxyConfig;

@Service
public class AmbariClientProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClientProvider.class);

    private static final Pattern AMBRI_URL_PATTERN = Pattern.compile("(http|https)://([a-zA-Z\\.0-9-]+):([0-9]+)");

    private static final int PROTOCOL_GROUP = 1;

    private static final int HOST_GROUP = 2;

    private static final int PORT_GROUP = 3;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private ApplicationProxyConfig applicationProxyConfig;

    /**
     * Create a new Ambari client. If the kerberos security is enabled on the cluster it will
     * automatically set the kerberos session. Clusters with kerberos security requires to
     * set this session otherwise the client cannot modify any resources.
     *
     * @param clientConfig HTTP client config
     * @param httpsPort    port number@param cluster Cloudbreak cluster
     * @return client
     */
    public AmbariClient getAmbariClient(HttpClientConfig clientConfig, Integer httpsPort, Cluster cluster) {
        return getAmbariClient(clientConfig, httpsPort,
                ambariSecurityConfigProvider.getCloudbreakClusterUserName(cluster), ambariSecurityConfigProvider.getCloudbreakClusterPassword(cluster));
    }

    /**
     * Create a new Ambari client. If the kerberos security is enabled
     * on the cluster this client won't be able to modify the cluster resources.
     *
     * @param clientConfig   tls configuration holding the ip address and the certificate paths
     * @param ambariUserName username for the Ambari server
     * @param ambariPassword password for the Ambari server
     * @return client
     */
    public AmbariClient getAmbariClient(HttpClientConfig clientConfig, Integer httpsPort, String ambariUserName, String ambariPassword) {
        if (applicationProxyConfig.isUseProxyForClusterConnection()) {
            String proxyHost = applicationProxyConfig.getHttpsProxyHost();
            int proxyPort = applicationProxyConfig.getHttpsProxyPort();
            if (applicationProxyConfig.isProxyAuthRequired()) {
                String proxyUser = applicationProxyConfig.getHttpsProxyUser();
                String proxyPassword = applicationProxyConfig.getHttpsProxyPassword();
                LOGGER.debug("Creating Ambari client with 2-way-ssl to connect to host:port: {}:{} through proxy: {}:{} with proxy user: {}",
                        clientConfig.getApiAddress(), httpsPort, proxyHost, proxyPort, proxyUser);
                return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort), ambariUserName, ambariPassword,
                        clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert(), proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                LOGGER.debug("Creating Ambari client with 2-way-ssl to connect to host:port: {}:{} through proxy: {}:{}",
                        clientConfig.getApiAddress(), httpsPort, proxyHost, proxyPort);
                return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort), ambariUserName, ambariPassword,
                        clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert(), proxyHost, proxyPort);
            }
        } else {
            LOGGER.debug("Creating Ambari client with 2-way-ssl to connect to host:port: {}:{}", clientConfig.getApiAddress(), httpsPort);
            return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort),
                    ambariUserName, ambariPassword, clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
        }
    }

    /**
     * Create a new Ambari client with the default user and password. If the kerberos security is enabled
     * on the cluster this client won't be able to modify the cluster resources.
     *
     * @param clientConfig tls configuration holding the ip address and the certificate paths
     * @return client
     */
    public AmbariClient getDefaultAmbariClient(HttpClientConfig clientConfig, Integer httpsPort) {
        if (applicationProxyConfig.isUseProxyForClusterConnection()) {
            String proxyHost = applicationProxyConfig.getHttpsProxyHost();
            int proxyPort = applicationProxyConfig.getHttpsProxyPort();
            if (applicationProxyConfig.isProxyAuthRequired()) {
                String proxyUser = applicationProxyConfig.getHttpsProxyUser();
                String proxyPassword = applicationProxyConfig.getHttpsProxyPassword();
                LOGGER.debug("Creating Ambari client with 2-way-ssl to connect to host:port: {}:{} through proxy: {}:{} with proxy user: {}",
                        clientConfig.getApiAddress(), httpsPort, proxyHost, proxyPort, proxyUser);
                return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort), "admin", "admin",
                        clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert(), proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                LOGGER.debug("Creating Ambari client with 2-way-ssl to connect to host:port: {}:{} through proxy: {}:{}",
                        clientConfig.getApiAddress(), httpsPort, proxyHost, proxyPort);
                return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort), "admin", "admin",
                        clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert(), proxyHost, proxyPort);
            }
        } else {
            LOGGER.debug("Creating Ambari client with default credentials with 2-way-ssl to connect to host:port: {}:{}",
                    clientConfig.getApiAddress(), httpsPort);
            return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort),
                    "admin", "admin", clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
        }
    }

    public AmbariClient getAmbariClient(URL ambariUrl, String ambariUser, String ambariPassword) {
        String basePath = StringUtils.removeEnd(ambariUrl.getPath(), "/");
        if (applicationProxyConfig.isUseProxyForClusterConnection()) {
            String proxyHost = applicationProxyConfig.getHttpsProxyHost();
            int proxyPort = applicationProxyConfig.getHttpsProxyPort();
            if (applicationProxyConfig.isProxyAuthRequired()) {
                String proxyUser = applicationProxyConfig.getHttpsProxyUser();
                String proxyPassword = applicationProxyConfig.getHttpsProxyPassword();
                LOGGER.debug("Creating Ambari client with user-pass to connect to host:port: {}:{} through proxy: {}:{} with proxy user: {}",
                        ambariUrl.getHost(), ambariUrl.getPort(), proxyHost, proxyPort, proxyUser);
                return new AmbariClient(ambariUrl.getHost(), String.valueOf(ambariUrl.getPort()), ambariUser, ambariPassword, basePath,
                        "https".equals(ambariUrl.getProtocol()), proxyHost, proxyPort, proxyUser, proxyPassword);
            } else {
                LOGGER.debug("Creating Ambari client with user-pass to connect to host:port: {}:{} through proxy: {}:{}",
                        ambariUrl.getHost(), ambariUrl.getPort(), proxyHost, proxyPort);
                return new AmbariClient(ambariUrl.getHost(), String.valueOf(ambariUrl.getPort()), ambariUser, ambariPassword, basePath,
                        "https".equals(ambariUrl.getProtocol()), proxyHost, proxyPort);
            }
        } else {
            LOGGER.debug("Creating Ambari client with default credentials with user-pass to connect to host:port: {}:{}",
                    ambariUrl.getHost(), ambariUrl.getPort());
            return new AmbariClient(ambariUrl.getHost(), String.valueOf(ambariUrl.getPort()), ambariUser, ambariPassword, basePath,
                    "https".equals(ambariUrl.getProtocol()));
        }
    }
}
