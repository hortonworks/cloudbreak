package com.sequenceiq.cloudbreak.service.cluster

import org.springframework.stereotype.Service

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig

@Service
class AmbariClientProvider {

    /**
     * Create a new Ambari client. If the kerberos security is enabled
     * on the cluster this client won't be able to modify the cluster resources.

     * @param clientConfig   tls configuration holding the ip address and the certificate paths
     * *
     * @param ambariUserName username for the Ambari server
     * *
     * @param ambariPassword password for the Ambari server
     * *
     * @return client
     */
    fun getAmbariClient(clientConfig: HttpClientConfig, httpsPort: Int?, ambariUserName: String, ambariPassword: String): AmbariClient {
        if (clientConfig.clientCert == null || httpsPort == null || clientConfig.clientKey == null || clientConfig.serverCert == null) {
            return AmbariClient(clientConfig.apiAddress, HTTP_PORT, ambariUserName, ambariPassword)
        }
        return AmbariClient(clientConfig.apiAddress, Integer.toString(httpsPort),
                ambariUserName, ambariPassword,
                clientConfig.clientCert, clientConfig.clientKey, clientConfig.serverCert)
    }

    /**
     * Create a new Ambari client with the default user and password. If the kerberos security is enabled
     * on the cluster this client won't be able to modify the cluster resources.

     * @param clientConfig tls configuration holding the ip address and the certificate paths
     * *
     * @return client
     */
    fun getDefaultAmbariClient(clientConfig: HttpClientConfig, httpsPort: Int?): AmbariClient {
        if (clientConfig.clientCert == null || clientConfig.clientKey == null || clientConfig.serverCert == null || httpsPort == null) {
            return AmbariClient(clientConfig.apiAddress, HTTP_PORT, "admin", "admin")
        }
        return AmbariClient(clientConfig.apiAddress, Integer.toString(httpsPort),
                "admin", "admin",
                clientConfig.clientCert, clientConfig.clientKey, clientConfig.serverCert)
    }

    /**
     * Create a new Ambari client. If the kerberos security is enabled on the cluster it will
     * automatically set the kerberos session. Clusters with kerberos security requires to
     * set this session otherwise the client cannot modify any resources.

     * @param cluster Cloudbreak cluster
     * *
     * @return client
     */
    fun getSecureAmbariClient(clientConfig: HttpClientConfig, httpsPort: Int?, cluster: Cluster): AmbariClient {
        val ambariClient = getAmbariClient(clientConfig, httpsPort, cluster.userName, cluster.password)
        if (cluster.isSecure) {
            setKerberosSession(ambariClient, cluster)
        }
        return ambariClient
    }

    /**
     * Any Ambari client can be updated with a kerberos session to be able to modify
     * cluster resources in a kerberos enabled cluster.

     * @param client client to be updated
     */
    fun setKerberosSession(client: AmbariClient, cluster: Cluster) {
        client.setKerberosSession(cluster.kerberosAdmin + ADMIN_PRINCIPAL, cluster.kerberosPassword)
    }

    companion object {

        private val HTTP_PORT = "8080"
        private val ADMIN_PRINCIPAL = "/admin"
    }
}
