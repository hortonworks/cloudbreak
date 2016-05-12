package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.filter.LoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltBootResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltBootResponses;
import com.sequenceiq.cloudbreak.util.KeyStoreUtil;

public class SaltConnector implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltConnector.class);

    // apache http connection pool defaults are constraining
    // https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
    private static final int MAX_TOTAL_CONNECTION = 100;
    private static final int MAX_PER_ROUTE_CONNECTION = 20;

    private static final String USER = "cbadmin";
    private static final String PASSWORD = "cbadmin";

    private static final String SALT_USER = "saltuser";
    private static final String SALT_PASSWORD = "saltpass";

    private final GatewayConfig gatewayConfig;
    private final Client restClient;
    private final WebTarget saltTarget;

    public SaltConnector(GatewayConfig gatewayConfig, boolean debug) {
        this.gatewayConfig = gatewayConfig;
        try {
            this.restClient = constructClient(debug);
            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(USER, PASSWORD);
            this.saltTarget = this.restClient.target(gatewayConfig.getGatewayUrl()).register(feature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create rest client with 2-way-ssl config", e);
        }
    }

    // Client is a heavywight object, consider not creating one for each, but cache it
    private Client constructClient(boolean debug) throws Exception {

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(KeyStoreUtil.createTrustStore(gatewayConfig.getServerCert()), null)
                .loadKeyMaterial(KeyStoreUtil.createKeyStore(gatewayConfig.getClientCert(), gatewayConfig.getClientKey()), "consul".toCharArray())
                .build();

        LOGGER.info("Constructing jax rs client for config: {}, debug: {}", gatewayConfig, debug);
        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.FOLLOW_REDIRECTS, "false");

        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        registryBuilder.register("http", PlainConnectionSocketFactory.getSocketFactory());
        registryBuilder.register("https", new SSLConnectionSocketFactory(sslContext));

        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(registryBuilder.build());

        connectionManager.setMaxTotal(MAX_TOTAL_CONNECTION);
        connectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE_CONNECTION);

        // tell the jersey config about the connection manager
        config.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
        config.connectorProvider(new ApacheConnectorProvider());

        ClientBuilder builder = JerseyClientBuilder.newBuilder().withConfig(config);

        if (debug) {
            builder = builder.register(new LoggingFilter(java.util.logging.Logger.getLogger(SaltConnector.class.getName()),
                    true));
        }

        Client client = builder.build();
        LOGGER.info("SaltConnector has been constructed client: {}, sslContext: {}", client, sslContext);
        return client;
    }

    public SaltBootResponse health() {
        SaltBootResponse response = saltTarget.path(SaltEndpoint.BOOT_HEALTH.getContextPath()).
                request().get().readEntity(SaltBootResponse.class);
        LOGGER.info("Health response: {}", response);
        return response;
    }

    public SaltBootResponse pillar(Pillar pillar) {
        SaltBootResponse response = saltTarget.path(SaltEndpoint.BOOT_PILLAR_SAVE
                .getContextPath()).request()
                .post(Entity.json(pillar)).readEntity(SaltBootResponse.class);
        LOGGER.info("Pillar response: {}", response);
        return response;
    }

    public SaltBootResponses action(SaltAction saltAction) {
        SaltBootResponses responses = saltTarget.path(SaltEndpoint.BOOT_ACTION_DISTRIBUTE
                .getContextPath()).request()
                .post(Entity.json(saltAction)).readEntity(SaltBootResponses.class);
        LOGGER.info("SaltAction response: {}", responses);
        return responses;
    }


    public <T> T run(Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", clientType.getType())
                .param("tgt", target.getTarget())
                .param("expr_form", target.getType());
        if (arg != null) {
            if (clientType.equals(SaltClientType.LOCAL) || clientType.equals(SaltClientType.LOCAL_ASYNC)) {
                for (String a : arg) {
                    form.param("arg", a);
                }
            } else {
                for (int i = 0; i < arg.length - 1; i = i + 2) {
                    form.param(arg[i], arg[i + 1]);
                }
            }
        }
        T response = saltTarget.path(SaltEndpoint.SALT_RUN
                .getContextPath()).request()
                .post(Entity.form(form)).readEntity(clazz);
        LOGGER.info("Salt run response: {}", response);
        return response;
    }

    public <T> T wheel(String fun, Collection<String> match, Class<T> clazz) {
        Form form = new Form();
        form = addAuth(form)
                .param("fun", fun)
                .param("client", "wheel");
        if (match != null && !match.isEmpty()) {
            form.param("match", match.stream().collect(Collectors.joining(",")));
        }
        T response = saltTarget.path(SaltEndpoint.SALT_RUN
                .getContextPath()).request()
                .post(Entity.form(form)).readEntity(clazz);
        LOGGER.info("SaltAction response: {}", response);
        return response;
    }

    private Form addAuth(Form form) {
        form.param("username", SALT_USER)
                .param("password", SALT_PASSWORD)
                .param("eauth", "pam");
        return form;
    }

    @Override
    public void close() throws IOException {
        if (restClient != null) {
            restClient.close();
        }
    }
}
