package com.sequenceiq.periscope.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.client.AccessToken;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.IdentityClient;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.api.endpoint.v1.AlertEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.AutoScaleClusterV1Endpoint;
import com.sequenceiq.periscope.api.endpoint.v1.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.HistoryEndpoint;
import com.sequenceiq.periscope.api.endpoint.v1.PolicyEndpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class AutoscaleClient {

    private static final Form EMPTY_FORM = new Form();

    private static final String TOKEN_KEY = "TOKEN";

    private static final double TOKEN_EXPIRATION_FACTOR = 0.9;

    private final Logger logger = LoggerFactory.getLogger(AutoscaleClient.class);

    private final ExpiringMap<String, String> tokenCache;

    private final Client client;

    private final IdentityClient identityClient;

    private final String autoscaleAddress;

    private String user;

    private String password;

    private String secret;

    private WebTarget webTarget;

    private EndpointHolder endpointHolder;

    private AutoscaleClient(String autoscaleAddress, String identityServerAddress, String user, String password, String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.autoscaleAddress = autoscaleAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.user = user;
        this.password = password;
        tokenCache = configTokenCache();
        logger.info("AutoscaleClient has been created with user / pass. autoscale: {}, identity: {}, clientId: {}, configKey: {}", autoscaleAddress,
                identityServerAddress, clientId, configKey);
    }

    private AutoscaleClient(String autoscaleAddress, String identityServerAddress, String secret, String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.autoscaleAddress = autoscaleAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.secret = secret;
        tokenCache = configTokenCache();
        logger.info("AutoscaleClient has been created with a secret. autoscale: {}, identity: {}, clientId: {}, configKey: {}", autoscaleAddress,
                identityServerAddress, clientId, configKey);
    }

    public AlertEndpoint alertEndpoint() {
        return refreshIfNeededAndGet(AlertEndpoint.class);
    }

    public AutoScaleClusterV1Endpoint clusterEndpoint() {
        return refreshIfNeededAndGet(AutoScaleClusterV1Endpoint.class);
    }

    public ConfigurationEndpoint configurationEndpoint() {
        return refreshIfNeededAndGet(ConfigurationEndpoint.class);
    }

    public HistoryEndpoint historyEndpoint() {
        return refreshIfNeededAndGet(HistoryEndpoint.class);
    }

    public PolicyEndpoint policyEndpoint() {
        return refreshIfNeededAndGet(PolicyEndpoint.class);
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    private synchronized <T> T refreshIfNeededAndGet(Class<T> clazz) {
        String token = tokenCache.get(TOKEN_KEY);
        if (token == null || endpointHolder == null) {
            AccessToken accessToken;
            accessToken = secret != null ? identityClient.getToken(secret) : identityClient.getToken(user, password);
            token = accessToken.getToken();
            int exp = (int) (accessToken.getExpiresIn() * TOKEN_EXPIRATION_FACTOR);
            logger.info("Token has been renewed and expires in {} seconds", exp);
            tokenCache.put(TOKEN_KEY, accessToken.getToken(), ExpirationPolicy.CREATED, exp, TimeUnit.SECONDS);
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add("Authorization", "Bearer " + token);
            webTarget = client.target(autoscaleAddress).path(AutoscaleApi.API_ROOT_CONTEXT);
            endpointHolder = new EndpointHolder(newEndpoint(AlertEndpoint.class, headers), newEndpoint(AutoScaleClusterV1Endpoint.class, headers),
                    newEndpoint(ConfigurationEndpoint.class, headers), newEndpoint(HistoryEndpoint.class, headers), newEndpoint(PolicyEndpoint.class, headers));
            logger.info("Endpoints have been renewed for AutoscaleClient");
        }
        return (T) endpointHolder.endpoints.stream().filter(e -> e.getClass().equals(clazz)).findFirst().get();
    }

    private <C> C newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM);
    }

    private static class EndpointHolder {
        private final List<?> endpoints;

        EndpointHolder(Object... endpoints) {
            this.endpoints = Arrays.asList(endpoints);
        }
    }

    public static class AutoscaleClientBuilder {

        private final String autoscaleAddress;

        private final String identityServerAddress;

        private final String clientId;

        private String user;

        private String password;

        private String secret;

        private boolean debug;

        private boolean secure = true;

        private boolean ignorePreValidation;

        public AutoscaleClientBuilder(String autoscaleAddress, String identityServerAddress, String clientId) {
            this.autoscaleAddress = autoscaleAddress;
            this.identityServerAddress = identityServerAddress;
            this.clientId = clientId;
        }

        public AutoscaleClientBuilder withCredential(String user, String password) {
            this.user = user;
            this.password = password;
            return this;
        }

        public AutoscaleClientBuilder withSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public AutoscaleClientBuilder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public AutoscaleClientBuilder withCertificateValidation(boolean secure) {
            this.secure = secure;
            return this;
        }

        public AutoscaleClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
            this.ignorePreValidation = ignorePreValidation;
            return this;
        }

        public AutoscaleClient build() {
            ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation);
            return secret != null ? new AutoscaleClient(autoscaleAddress, identityServerAddress, secret, clientId, configKey)
                    : new AutoscaleClient(autoscaleAddress, identityServerAddress, user, password, clientId, configKey);
        }
    }
}
