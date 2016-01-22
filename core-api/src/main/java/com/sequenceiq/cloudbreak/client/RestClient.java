package com.sequenceiq.cloudbreak.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.config.ConfigKey;
import com.sequenceiq.cloudbreak.client.security.CertificateTrustManager;

public class RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);

    // apache http connection pool defaults are constraining
    // https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    private static final int MAX_TOTAL_CONNECTION = 100;
    private static final int MAX_PER_ROUTE_CONNECTION = 20;

    private static ConcurrentMap<ConfigKey, Client> clients = new ConcurrentHashMap<>();

    private RestClient() {
    }

    static synchronized Client get(ConfigKey configKey) {
        Client client = clients.get(configKey);
        if (client == null) {
            client = constructClient(configKey);
            clients.put(configKey, client);
        }
        LOGGER.info("RestClient cache size: {}, key: {}, fetched client: {}", clients.size(), configKey, client);
        return client;

    }

    private static Client constructClient(ConfigKey configKey) {
        LOGGER.info("Constructing jax rs client: {}", configKey);
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false");

        PoolingHttpClientConnectionManager connectionManager;

        if (configKey.isSecure()) {
            connectionManager = new PoolingHttpClientConnectionManager();
        } else {
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", new SSLConnectionSocketFactory(CertificateTrustManager.sslContext()))
                    .build();

            connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        }
        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTION);
        connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE_CONNECTION);

        // tell the jersey config about the connection manager
        config.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);

        config.connectorProvider(new ApacheConnectorProvider());

        ClientBuilder builder = JerseyClientBuilder.newBuilder().withConfig(config);

        if (configKey.isDebug()) {
            builder = builder.register(new LoggingFilter(java.util.logging.Logger.getLogger(RestClient.class.getName()), true));
        }

        Client client = builder.build();

        SSLContext sslContext = client.getSslContext();
        LOGGER.warn("RestClient has been constructed: {}, client: {}, sslContext: {}", configKey, client, sslContext);
        return client;
    }

}
