package com.sequenceiq.cloudbreak.service.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.domain.Cluster;

@Service
public class AmbariClientProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClientProvider.class);

    private static final String HTTP_PORT = "8080";

    @Inject
    private AmbariAuthenticationProvider ambariAuthenticationProvider;

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
                ambariAuthenticationProvider.getAmbariUserName(cluster), ambariAuthenticationProvider.getAmbariPassword(cluster));
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
        if (clientConfig.getClientCert() == null || httpsPort == null || clientConfig.getClientKey() == null || clientConfig.getServerCert() == null) {
            LOGGER.info("Creating Ambari client without 2-way-ssl to connect to host:port: " + clientConfig.getApiAddress() + ':' + HTTP_PORT);
            return new AmbariClient(clientConfig.getApiAddress(), HTTP_PORT, ambariUserName, ambariPassword);
        }
        LOGGER.info(String.format("Creating Ambari client with 2-way-ssl to connect to host:port: %s:%s", clientConfig.getApiAddress(), httpsPort));
        return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort),
                ambariUserName, ambariPassword,
                clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
    }

    /**
     * Create a new Ambari client with the default user and password. If the kerberos security is enabled
     * on the cluster this client won't be able to modify the cluster resources.
     *
     * @param clientConfig tls configuration holding the ip address and the certificate paths
     * @return client
     */
    public AmbariClient getDefaultAmbariClient(HttpClientConfig clientConfig, Integer httpsPort) {
        if (clientConfig.getClientCert() == null || clientConfig.getClientKey() == null || clientConfig.getServerCert() == null || httpsPort == null) {
            LOGGER.info("Creating Ambari client with default credentials without 2-way-ssl to connect to host:port: "
                    + clientConfig.getApiAddress() + ':' + HTTP_PORT);
            return new AmbariClient(clientConfig.getApiAddress(), HTTP_PORT, "admin", "admin");
        }
        LOGGER.info(String.format("Creating Ambari client with default "
                + "credentials with 2-way-ssl to connect to host:port: %s:%s", clientConfig.getApiAddress(), httpsPort));
        return new AmbariClient(clientConfig.getApiAddress(), Integer.toString(httpsPort),
                "admin", "admin", clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
    }
}
