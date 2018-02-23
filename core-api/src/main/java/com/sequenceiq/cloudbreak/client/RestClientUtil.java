package com.sequenceiq.cloudbreak.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.apache.http.ssl.SSLContexts;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientUtil.class);

    private static final int CONNECT_TIMEOUT_MS = 20_000;

    private static final Map<ConfigKey, Client> CLIENTS = new ConcurrentHashMap<>();

    private RestClientUtil() {
    }

    public static synchronized Client get() {
        return get(new ConfigKey(false, false, false));
    }

    public static synchronized Client get(ConfigKey configKey) {
        Client client = CLIENTS.computeIfAbsent(configKey, RestClientUtil::createClient);
        LOGGER.info("RestClient cache size: {}, key: {}, fetched client: {}", CLIENTS.size(), configKey, client);
        return client;
    }

    public static Client createClient(String serverCert, String clientCert, String clientKey, boolean debug, Class<?> debugClass) throws Exception {
        SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(KeyStoreUtil.createTrustStore(serverCert), null)
            .loadKeyMaterial(KeyStoreUtil.createKeyStore(clientCert, clientKey), "consul".toCharArray())
            .build();

        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false");
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
        config.register(MultiPartFeature.class);

        ClientBuilder builder = ClientBuilder.newBuilder().withConfig(config);
        builder.sslContext(sslContext);
        builder.hostnameVerifier(CertificateTrustManager.hostnameVerifier());

        if (debug) {
            builder = builder.register(new LoggingFilter(java.util.logging.Logger.getLogger(debugClass.getName()), true));
        }

        Client client = builder.build();
        LOGGER.debug("Jax rs client has been constructed: {}, sslContext: {}", client, sslContext);
        return client;
    }

    private static Client createClient(ConfigKey configKey) {
        LOGGER.debug("Constructing jax rs client: {}", configKey);
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false");
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
        config.register(MultiPartFeature.class);

        ClientBuilder builder = ClientBuilder.newBuilder().withConfig(config);

        if (configKey.isDebug()) {
            builder = builder.register(new LoggingFilter(java.util.logging.Logger.getLogger(RestClientUtil.class.getName()), true));
        }

        if (!configKey.isSecure()) {
            builder.sslContext(CertificateTrustManager.sslContext());
            builder.hostnameVerifier(CertificateTrustManager.hostnameVerifier());
        }

        Client client = builder.build();
        client.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, configKey.isIgnorePreValidation());

        SSLContext sslContext = client.getSslContext();
        LOGGER.warn("RestClient has been constructed: {}, client: {}, sslContext: {}", configKey, client, sslContext);
        return client;
    }
}
