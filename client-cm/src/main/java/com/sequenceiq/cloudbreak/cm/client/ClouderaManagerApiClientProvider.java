package com.sequenceiq.cloudbreak.cm.client;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.tracing.CmRequestIdProviderInterceptor;
import com.sequenceiq.cloudbreak.cm.client.tracing.CmRequestLoggerInterceptor;
import com.sequenceiq.cloudbreak.service.sslcontext.SSLContextProvider;
import com.sequenceiq.cloudbreak.util.HostUtil;

@Component
public class ClouderaManagerApiClientProvider {

    public static final String API_ROOT = "/api";

    public static final String API_V_31 = API_ROOT + "/v31";

    public static final String API_V_40 = API_ROOT + "/v40";

    public static final String API_V_43 = API_ROOT + "/v43";

    public static final String API_V_45 = API_ROOT + "/v45";

    public static final String API_V_46 = API_ROOT + "/v46";

    public static final String API_V_51 = API_ROOT + "/v51";

    public static final String API_V_52 = API_ROOT + "/v52";

    public static final String API_V_53 = API_ROOT + "/v53";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerApiClientProvider.class);

    @Value("${cb.cm.client.cluster.proxy.timeout}")
    private Integer clusterProxyTimeout;

    @Value("${cb.cm.client.connect.timeout.seconds}")
    private Integer connectTimeoutSeconds;

    @Value("${cb.cm.client.read.timeout.seconds}")
    private Integer readTimeoutSeconds;

    @Value("${cb.cm.client.write.timeout.seconds}")
    private Integer writeTimeoutSeconds;

    @Inject
    private CmRequestIdProviderInterceptor cmRequestIdProviderInterceptor;

    @Inject
    private CmRequestLoggerInterceptor cmRequestLoggerInterceptor;

    @Inject
    private SSLContextProvider sslContextProvider;

    public ApiClient getDefaultClient(Integer gatewayPort, HttpClientConfig clientConfig, String apiVersion) throws ClouderaManagerClientInitException {
        ApiClient client = getClouderaManagerClient(clientConfig, gatewayPort, "admin", "admin", apiVersion);
        if (clientConfig.isClusterProxyEnabled()) {
            client.addDefaultHeader("Proxy-Ignore-Auth", "true");
        }
        return client;
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
                cmClient.addDefaultHeader("Proxy-With-Timeout", clusterProxyTimeout.toString());
            } else if (port != null && !HostUtil.hasPort(clientConfig.getApiAddress())) {
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
                SSLContext sslContext = sslContextProvider.getSSLContext(clientConfig.getServerCert(), Optional.empty(),
                        clientConfig.getClientCert(), clientConfig.getClientKey());
                cmClient.getHttpClient().setSslSocketFactory(sslContext.getSocketFactory());
                cmClient.getHttpClient().setHostnameVerifier(CertificateTrustManager.hostnameVerifier());
            }
            cmClient.getHttpClient().interceptors().add(cmRequestIdProviderInterceptor);
            cmClient.getHttpClient().interceptors().add(cmRequestLoggerInterceptor);
            cmClient.getHttpClient().setConnectTimeout(Long.valueOf(connectTimeoutSeconds), TimeUnit.SECONDS);
            cmClient.getHttpClient().setReadTimeout(Long.valueOf(readTimeoutSeconds), TimeUnit.SECONDS);
            cmClient.getHttpClient().setWriteTimeout(Long.valueOf(writeTimeoutSeconds), TimeUnit.SECONDS);
            return cmClient;
        } catch (Exception e) {
            LOGGER.info("Cannot create SSL context for Cloudera Manager", e);
            throw new ClouderaManagerClientInitException("Couldn't create client", e);
        }
    }

    private boolean isCmSslConfigValidClientConfigValid(HttpClientConfig config) {
        return config.getClientCert() != null && config.getServerCert() != null && config.getClientKey() != null;
    }

    public ApiClient getV31Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_31);
    }

    public ApiClient getV40Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_40);
    }

    public ApiClient getV43Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_43);
    }

    public ApiClient getV45Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_45);
    }

    public ApiClient getV46Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_46);
    }

    public ApiClient getV51Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_51);
    }

    public ApiClient getV52Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_52);
    }

    public ApiClient getV53Client(Integer gatewayPort, String user, String password, HttpClientConfig clientConfig) throws ClouderaManagerClientInitException {
        return getApiClientByApiVersion(gatewayPort, user, password, clientConfig, API_V_53);
    }
}
