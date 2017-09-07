package com.sequenceiq.periscope.utils;

import java.lang.reflect.Field;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.ConsulRawClient;
import com.sequenceiq.cloudbreak.client.KeyStoreUtil;
import com.sequenceiq.periscope.model.TlsConfiguration;

public final class ConsulUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulUtils.class);

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private static final int MAX_CONNECTION = 1000;

    private static final int MAX_ROUTE = 500;

    private ConsulUtils() {
        throw new IllegalStateException();
    }

    public static ConsulClient createClient(String apiAddress, String apiPort, TlsConfiguration tlsConfiguration) throws Exception {
        return createClient(apiAddress, Integer.parseInt(apiPort), tlsConfiguration);
    }

    public static ConsulClient createClient(String apiAddress, int apiPort, TlsConfiguration tlsConfiguration) throws Exception {
        HttpClient httpClient = createHttpClient(tlsConfiguration.getClientCert(), tlsConfiguration.getClientKey(), tlsConfiguration.getServerCert());
        ConsulRawClient rawClient = new ConsulRawClient("https://" + apiAddress + ':' + apiPort, httpClient);
        Field agentAddress = ReflectionUtils.findField(ConsulRawClient.class, "agentAddress");
        ReflectionUtils.makeAccessible(agentAddress);
        ReflectionUtils.setField(agentAddress, rawClient, "https://" + apiAddress + ':' + apiPort + "/consul");
        return new ConsulClient(rawClient);
    }

    private static HttpClient createHttpClient(String clientCert, String clientKey, String serverCert) throws Exception {
        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(KeyStoreUtil.createTrustStore(serverCert), null)
                .loadKeyMaterial(KeyStoreUtil.createKeyStore(clientCert, clientKey), "consul".toCharArray())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(setupSchemeRegistry(sslContext));
        connectionManager.setMaxTotal(MAX_CONNECTION);
        connectionManager.setDefaultMaxPerRoute(MAX_ROUTE);
        Builder requestBuilder = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_TIMEOUT_MS).setConnectionRequestTimeout(DEFAULT_TIMEOUT_MS).setSocketTimeout(DEFAULT_TIMEOUT_MS);
        return HttpClientBuilder.create().setConnectionManager(connectionManager).setDefaultRequestConfig(requestBuilder.build()).build();
    }

    private static Registry<ConnectionSocketFactory> setupSchemeRegistry(SSLContext sslContext) {
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
        if (sslContext != null) {
            registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext));
        }
        return registryBuilder.build();
    }

}

