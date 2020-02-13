package com.sequenceiq.cloudbreak.cm.client;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.KeyStoreUtil;

@Component
public class ClouderaManagerApiClientProvider {
    public static final String API_ROOT = "/api";

    public static final String API_V_31 = API_ROOT + "/v31";

    public static final String API_V_40 = API_ROOT + "/v40";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerApiClientProvider.class);

    private static final Integer CLUSTER_PROXY_TIMEOUT = 90000;

    public ApiClient getDefaultClient(Integer gatewayPort, HttpClientConfig clientConfig, String apiVersion) throws ClouderaManagerClientInitException {
        ApiClient client = getClouderaManagerClient(clientConfig, gatewayPort, "admin", "admin", apiVersion);
        if (clientConfig.isClusterProxyEnabled()) {
            client.addDefaultHeader("Proxy-Ignore-Auth", "true");
        }
        return client;
    }

    public ApiClient getClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_31);
    }

    private ApiClient getApiClientByApiVersion(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig, String apiVersion)
            throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return getClouderaManagerClient(clientConfig,
                    gatewayPort, user, password, apiVersion);
        } else {
            return getDefaultClient(gatewayPort, clientConfig, apiVersion);
        }
    }

    public ApiClient getRootClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return getClouderaManagerRootClient(clientConfig,
                    gatewayPort, user, password);
        } else {
            return getClouderaManagerRootClient(clientConfig, gatewayPort, "admin", "admin");
        }
    }

    public ApiClient getClouderaManagerClient(HttpClientConfig clientConfig, Integer port, String userName, String password, String apiVersion)
            throws ClouderaManagerClientInitException {
        return getApiClientWithContext(clientConfig, port, userName, password, apiVersion);
    }

    public ApiClient getClouderaManagerRootClient(HttpClientConfig clientConfig, Integer port, String userName, String password)
            throws ClouderaManagerClientInitException {
        return getClouderaManagerClient(clientConfig, port, userName, password, API_ROOT);
    }

    private ApiClient getApiClientWithContext(HttpClientConfig clientConfig, Integer port, String userName, String password, String context)
            throws ClouderaManagerClientInitException {
        try {
            ApiClient cmClient = new ApiClient();
            if (clientConfig.isClusterProxyEnabled()) {
                cmClient.setBasePath(clientConfig.getClusterProxyUrl() + "/proxy/" + clientConfig.getClusterCrn() + "/cb-internal" + context);
                cmClient.addDefaultHeader("Proxy-Ignore-Auth", "true");
                cmClient.addDefaultHeader("Proxy-With-Timeout", CLUSTER_PROXY_TIMEOUT.toString());
            } else if (port != null) {
                cmClient.setBasePath("https://" + clientConfig.getApiAddress() + ':' + port + context);
            } else {
                cmClient.setBasePath("https://" + clientConfig.getApiAddress() + context);
            }
            return decorateClient(clientConfig, userName, password, cmClient);
        } catch (Exception e) {
            LOGGER.warn("Couldn't create client", e);
            throw new ClouderaManagerClientInitException("Couldn't create client", e);
        }
    }

    private ApiClient decorateClient(HttpClientConfig clientConfig, String userName, String password, ApiClient cmClient) throws Exception {
        cmClient.setUsername(userName);
        cmClient.setPassword(password);
        cmClient.setVerifyingSsl(true);

        try {
            if (isCmSslConfigValidClientConfigValid(clientConfig) && !clientConfig.isClusterProxyEnabled()) {
                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(KeyStoreUtil.createTrustStore(clientConfig.getServerCert()), null)
                        .loadKeyMaterial(KeyStoreUtil.createKeyStore(clientConfig.getClientCert(), clientConfig.getClientKey()), "consul".toCharArray())
                        .build();
                cmClient.getHttpClient().setSslSocketFactory(sslContext.getSocketFactory());
                cmClient.getHttpClient().setHostnameVerifier(CertificateTrustManager.hostnameVerifier());
            }
            cmClient.getHttpClient().setConnectTimeout(1L, TimeUnit.MINUTES);
            cmClient.getHttpClient().setReadTimeout(1L, TimeUnit.MINUTES);
            cmClient.getHttpClient().setWriteTimeout(1L, TimeUnit.MINUTES);
            return cmClient;
        } catch (Exception e) {
            LOGGER.info("Cannot create SSL context for Cloudera Manager", e);
            throw new ClouderaManagerClientInitException("Couldn't create client", e);
        }
    }

    private boolean isCmSslConfigValidClientConfigValid(HttpClientConfig config) {
        return config.getClientCert() != null && config.getServerCert() != null && config.getClientKey() != null;
    }

    public ApiClient getV40Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_40);
    }
}
