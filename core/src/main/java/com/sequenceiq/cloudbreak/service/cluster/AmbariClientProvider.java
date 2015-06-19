package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.service.stack.flow.TLSClientConfig;

@Service
public class AmbariClientProvider {

    private static final String PORT = "443";
    private static final String ADMIN_PRINCIPAL = "/admin";

    /**
     * Create a new Ambari client. If the kerberos security is enabled
     * on the cluster this client won't be able to modify the cluster resources.
     *
     * @param clientConfig tls configuration holding the ip address and the certificate paths
     * @param ambariUserName username for the Ambari server
     * @param ambariPassword password for the Ambari server
     * @return client
     */
    public AmbariClient getAmbariClient(TLSClientConfig clientConfig, String ambariUserName, String ambariPassword) {
        return new AmbariClient(clientConfig.getApiAddress(), PORT,
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
    public AmbariClient getDefaultAmbariClient(TLSClientConfig clientConfig) {
        return new AmbariClient(clientConfig.getApiAddress(), PORT,
                "admin", "admin",
                clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
    }

    /**
     * Create a new Ambari client. If the kerberos security is enabled on the cluster it will
     * automatically set the kerberos session. Clusters with kerberos security requires to
     * set this session otherwise the client cannot modify any resources.
     *
     * @param cluster Cloudbreak cluster
     * @return client
     */
    public AmbariClient getSecureAmbariClient(TLSClientConfig clientConfig, Cluster cluster) {
        AmbariClient ambariClient = getAmbariClient(clientConfig, cluster.getUserName(), cluster.getPassword());
        if (cluster.isSecure()) {
            setKerberosSession(ambariClient, cluster);
        }
        return ambariClient;
    }

    /**
     * Any Ambari client can be updated with a kerberos session to be able to modify
     * cluster resources in a kerberos enabled cluster.
     *
     * @param client client to be updated
     */
    public void setKerberosSession(AmbariClient client, Cluster cluster) {
        client.setKerberosSession(cluster.getKerberosAdmin() + ADMIN_PRINCIPAL, cluster.getKerberosPassword());
    }
}
