package com.sequenceiq.cloudbreak.client;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class RestClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientUtil.class);

    private static final int CONNECT_TIMEOUT_MS = 20_000;

    private static final int READ_TIMEOUT_MS = 60_000;

    private static final Map<ConfigKey, Client> CLIENTS = new ConcurrentHashMap<>();

    private static final Lock LOCK = new ReentrantLock();

    private RestClientUtil() {
    }

    public static Client get() {
        return get(new ConfigKey(false, false, false));
    }

    public static Client get(ConfigKey configKey) {
        try {
            LOCK.lock();
            Client client = CLIENTS.computeIfAbsent(configKey, RestClientUtil::createClient);
            LOGGER.debug("RestClient cache size: {}, key: {}, fetched client: {}", CLIENTS.size(), configKey, client);
            return client;
        } finally {
            LOCK.unlock();
        }
    }

    public static Client createClient(SSLContext sslContext, int connectionTimeout, int readTimeout, boolean debug)
            throws Exception {
        return createClient(sslContext, connectionTimeout, OptionalInt.of(readTimeout), debug);
    }

    public static Client createClient(SSLContext sslContext, int connectionTimeout,
            OptionalInt readTimeout, boolean debug)
            throws Exception {
        return createClient(sslContext, connectionTimeout, readTimeout, debug, Collections.emptySet());
    }

    public static Client createClient(SSLContext sslContext, boolean debug) {
        return createClient(sslContext, CONNECT_TIMEOUT_MS, OptionalInt.empty(), debug, Collections.emptySet());
    }

    public static Client createClient(SSLContext sslContext, int connectionTimeout, OptionalInt readTimeout, boolean debug,
            Collection<Object> providers) {
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false");
        config.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout);
        config.property(ClientProperties.READ_TIMEOUT, readTimeout.orElse(READ_TIMEOUT_MS));
        config.register(MultiPartFeature.class);
        config.register(RequestIdProviderFeature.class);
        config.register(EncodingFilter.class);
        config.register(GZipEncoder.class);
        for (Object provider : providers) {
            config.register(provider);
        }

        ClientBuilder builder = ClientBuilder.newBuilder().withConfig(config);
        builder.sslContext(sslContext);
        builder.hostnameVerifier(CertificateTrustManager.hostnameVerifier());
        if (debug) {
            builder = enableRestDebug(builder);
        }
        Client client = builder.build();
        client.register(createIgnoreUnknownFieldsProvider());
        LOGGER.debug("Jax rs client has been constructed: {}, sslContext: {}", client, sslContext);
        return client;
    }

    private static Client createClient(ConfigKey configKey) {
        LOGGER.debug("Constructing jax rs client: {}", configKey);
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, configKey.isFollowRedirects());
        config.property(ClientProperties.CONNECT_TIMEOUT, configKey.getTimeout().orElse(CONNECT_TIMEOUT_MS));
        config.property(ClientProperties.READ_TIMEOUT, configKey.getTimeout().orElse(READ_TIMEOUT_MS));
        config.register(MultiPartFeature.class);
        config.register(RequestIdProviderFeature.class);

        ClientBuilder builder = ClientBuilder.newBuilder().withConfig(config);

        if (configKey.isDebug()) {
            builder = enableRestDebug(builder);
        }

        if (!configKey.isSecure()) {
            builder.sslContext(CertificateTrustManager.sslContext());
            builder.hostnameVerifier(CertificateTrustManager.hostnameVerifier());
        }

        Client client = builder.build();
        client.property(ClientProperties.SUPPRESS_HTTP_COMPLIANCE_VALIDATION, configKey.isIgnorePreValidation());
        client.register(createIgnoreUnknownFieldsProvider());

        SSLContext sslContext = client.getSslContext();
        LOGGER.debug("RestClient has been constructed: {}, client: {}, sslContext: {}", configKey, client, sslContext);
        return client;
    }

    private static JacksonJsonProvider createIgnoreUnknownFieldsProvider() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return new JacksonJsonProvider(mapper);
    }

    private static ClientBuilder enableRestDebug(ClientBuilder builder) {
        return builder.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, Verbosity.PAYLOAD_ANY)
                .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, "INFO");
    }
}
