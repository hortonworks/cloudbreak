package com.sequenceiq.cloudbreak.client;

import java.util.concurrent.ExecutionException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.exception.RestClientInitializationException;
import com.sequenceiq.cloudbreak.client.config.ConfigKey;
import com.sequenceiq.cloudbreak.client.security.CertificateTrustManager;

import jersey.repackaged.com.google.common.cache.CacheBuilder;
import jersey.repackaged.com.google.common.cache.CacheLoader;
import jersey.repackaged.com.google.common.cache.LoadingCache;

public class RestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClient.class);

    private static LoadingCache<ConfigKey, Client> clients;

    private RestClient() {
    }

    static synchronized Client get(ConfigKey configKey) {
        try {

            if (clients == null) {
                clients = CacheBuilder.newBuilder()
                        .build(
                                new CacheLoader<ConfigKey, Client>() {
                                    public Client load(ConfigKey key) {
                                        return constructClient(key);
                                    }
                                });
            }

            return clients.get(configKey);
        } catch (ExecutionException e) {
            throw new RestClientInitializationException("Failed to setup RestClient", e);
        }
    }

    private static Client constructClient(ConfigKey configKey) {
        LOGGER.info("Contructing jax rs client: {}", configKey);
        ClientConfig config = new ClientConfig().property(ClientProperties.FOLLOW_REDIRECTS, "false");
        ClientBuilder builder = ClientBuilder.newBuilder().withConfig(config);
        if (!configKey.isSecure()) {
            builder = builder.sslContext(CertificateTrustManager.sslContext()).
                    hostnameVerifier(CertificateTrustManager.hostnameVerifier());
        }
        if (configKey.isDebug()) {
            builder = builder.register(new LoggingFilter(java.util.logging.Logger.getLogger(RestClient.class.getName()), true));
        }
        LOGGER.warn("RestClient has been constructed: {}", configKey);
        return builder.build();
    }

}
