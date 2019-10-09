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

    public static final String API_V_31 = "/api/v31";

    public static final String API_ROOT = "/api";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerApiClientProvider.class);

    public ApiClient getDefaultClient(Integer gatewayPort, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getClouderaManagerClient(clientConfig, gatewayPort, "admin", "admin");
    }

    public ApiClient getClient(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        if (StringUtils.isNoneBlank(user, password)) {
            return getClouderaManagerClient(clientConfig,
                    gatewayPort, user, password);
        } else {
            return getDefaultClient(gatewayPort, clientConfig);
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

    public ApiClient getClouderaManagerClient(HttpClientConfig clientConfig, Integer port, String userName, String password)
            throws ClouderaManagerClientInitException {
        try {
            ApiClient cmClient = new ApiClient();
            if (port != null) {
                cmClient.setBasePath("https://" + clientConfig.getApiAddress() + ':' + port + API_V_31);
            } else {
                cmClient.setBasePath("https://" + clientConfig.getApiAddress() + API_V_31);
            }
            return decorateClient(clientConfig, userName, password, cmClient);
        } catch (Exception e) {
            LOGGER.warn("Couldn't create client", e);
            throw new ClouderaManagerClientInitException("Couldn't create client", e);
        }
    }

    public ApiClient getClouderaManagerRootClient(HttpClientConfig clientConfig, Integer port, String userName, String password)
            throws ClouderaManagerClientInitException {
        try {
            ApiClient cmClient = new ApiClient();
            if (port != null) {
                cmClient.setBasePath("https://" + clientConfig.getApiAddress() + ':' + port + API_ROOT);
            } else {
                cmClient.setBasePath("https://" + clientConfig.getApiAddress() + API_ROOT);
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
            if (isCmSslConfigValidClientConfigValid(clientConfig)) {
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
            LOGGER.info("Can not create SSL context for Cloudera Manager", e);
            throw new ClouderaManagerClientInitException("Couldn't create client", e);
        }
    }

    private boolean isCmSslConfigValidClientConfigValid(HttpClientConfig config) {
        return config.getClientCert() != null && config.getServerCert() != null && config.getClientKey() != null;
    }
}
