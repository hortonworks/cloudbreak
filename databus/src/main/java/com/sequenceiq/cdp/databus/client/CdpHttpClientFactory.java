package com.sequenceiq.cdp.databus.client;

import static com.cloudera.cdp.ValidationUtils.checkNotNullAndThrow;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;

import com.cloudera.cdp.CdpClientException;
import com.cloudera.cdp.ValidationUtils;
import com.cloudera.cdp.client.CdpClientConfiguration;
import com.cloudera.cdp.shaded.org.apache.http.config.Registry;
import com.cloudera.cdp.shaded.org.apache.http.config.RegistryBuilder;
import com.cloudera.cdp.shaded.org.apache.http.conn.socket.ConnectionSocketFactory;
import com.cloudera.cdp.shaded.org.apache.http.conn.socket.PlainConnectionSocketFactory;
import com.cloudera.cdp.shaded.org.apache.http.conn.ssl.DefaultHostnameVerifier;
import com.cloudera.cdp.shaded.org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import com.cloudera.cdp.shaded.org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CdpHttpClientFactory {

    private static final KeyStore USE_DEFAULT_KEYSTORE = null;

    /**
     * Create a client.
     *
     * @param config the client configuration
     * @return the client
     */
    public Client create(CdpClientConfiguration config) {
        checkNotNullAndThrow(config);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setDateFormat(new StdDateFormat());
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        JacksonJsonProvider jsonProvider = new JacksonJsonProvider(objectMapper);

        ConnectionKeepAliveStrategy keepAliveStrategy = null;
        if (config.getConnectionMaxIdle().toMillis() > 0) {
            keepAliveStrategy = (ConnectionKeepAliveStrategy) (response, context) -> {
                long maxIdleTime = config.getConnectionMaxIdle().toMillis();
                // If there's a Keep-Alive timeout directive in the response and it's
                // shorter than our configured max, honor that. Otherwise go with the
                // configured maximum.
                long duration = DefaultConnectionKeepAliveStrategy.INSTANCE
                        .getKeepAliveDuration(response, context);
                if (0 < duration && duration < maxIdleTime) {
                    return duration;
                }
                return maxIdleTime;
            };
        }

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.register(jsonProvider);
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        clientConfig.property(
                ApacheClientProperties.CONNECTION_MANAGER,
                createConnectionManager(config));
        clientConfig.property(
                ApacheClientProperties.KEEPALIVE_STRATEGY,
                keepAliveStrategy);
        clientConfig.property(
                ClientProperties.PROXY_URI,
                config.getProxyUri());
        clientConfig.property(
                ClientProperties.PROXY_USERNAME,
                config.getProxyUsername());
        clientConfig.property(
                ClientProperties.PROXY_PASSWORD,
                config.getProxyPassword());

        Client client = ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .build();
        client.property(ClientProperties.READ_TIMEOUT,
                (int) config.getReadTimeout().toMillis());
        client.property(ClientProperties.CONNECT_TIMEOUT,
                (int) config.getConnectionTimeout().toMillis());

        return client;
    }

    public PoolingHttpClientConnectionManager createConnectionManager(CdpClientConfiguration config) {
        ValidationUtils.checkNotNullAndThrow(config);
        TrustManager[] trustManagers = null;

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(USE_DEFAULT_KEYSTORE);
            trustManagers = trustManagerFactory.getTrustManagers();
        } catch (NoSuchAlgorithmException | KeyStoreException var8) {
            throw new CdpClientException("Error initializing truststore", var8);
        }

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
        } catch (KeyManagementException | NoSuchAlgorithmException var7) {
            throw new CdpClientException("Error initializing SSL", var7);
        }

        HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();
        RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = builder.register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext, hostnameVerifier)).build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setValidateAfterInactivity((int) config.getValidateAfterInactivity().toMillis());
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnections());
        connectionManager.setMaxTotal(config.getMaxConnections());
        return connectionManager;
    }
}
