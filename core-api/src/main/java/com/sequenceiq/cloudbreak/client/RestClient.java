package com.sequenceiq.cloudbreak.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

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
        LOGGER.info("Contructing jax rs client: {}", configKey);
        ClientConfig config = new ClientConfig().property(ClientProperties.FOLLOW_REDIRECTS, "false");

        ClientBuilder builder = JerseyClientBuilder.newBuilder().withConfig(config);
        if (!configKey.isSecure()) {
            SSLContext sc = CertificateTrustManager.sslContext();
            //FIXME this is required because of Jersey + HTTP url connection issue, it behaves totally differnly if you are using Java7 or Java8
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            builder = builder.sslContext(sc);
            builder = builder.hostnameVerifier(CertificateTrustManager.hostnameVerifier());
        }
        if (configKey.isDebug()) {
            builder = builder.register(new LoggingFilter(java.util.logging.Logger.getLogger(RestClient.class.getName()), true));
        }
        Client client = builder.build();
        SSLContext sslContext = client.getSslContext();
        LOGGER.warn("RestClient has been constructed: {}, client: {}, sslContext: {}", configKey, client, sslContext);
        return client;
    }

}
