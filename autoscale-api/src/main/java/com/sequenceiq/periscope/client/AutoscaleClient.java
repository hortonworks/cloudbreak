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

import com.sequenceiq.cloudbreak.auth.TokenUnavailableException;
import com.sequenceiq.cloudbreak.auth.caas.CaasClient;
import com.sequenceiq.cloudbreak.restclient.ConfigKey;
import com.sequenceiq.cloudbreak.restclient.RestClientUtil;
import com.sequenceiq.cloudbreak.restclient.TokenRequest;
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

    private final Logger logger = LoggerFactory.getLogger(AutoscaleClient.class);

    private final ExpiringMap<String, String> tokenCache;

    private final Client client;

    private final String autoscaleAddress;

    private String refreshToken;

    private final CaasClient caasClient;

    private WebTarget webTarget;

    private EndpointHolder endpointHolder;

    private AutoscaleClient(String autoscaleAddress, String caasProtocol, String caasAddress, String refreshToken, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.autoscaleAddress = autoscaleAddress;
        this.refreshToken = refreshToken;
        caasClient = new CaasClient(caasProtocol, caasAddress, configKey);
        tokenCache = configTokenCache();
        logger.info("AutoscaleClient has been created with token. autoscale: {}, token: {}, configKey: {}", autoscaleAddress, refreshToken, configKey);
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
        if (refreshToken != null) {
            String accessToken = tokenCache.get(TOKEN_KEY);
            if (accessToken == null || endpointHolder == null) {
                TokenRequest tokenRequest = new TokenRequest();
                tokenRequest.setRefreshToken(refreshToken);
                accessToken = caasClient.getAccessToken(tokenRequest);
                tokenCache.put(TOKEN_KEY, accessToken, ExpirationPolicy.CREATED, 1, TimeUnit.MINUTES);
                MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
                headers.add("Authorization", "Bearer " + accessToken);
                webTarget = client.target(autoscaleAddress).path(AutoscaleApi.API_ROOT_CONTEXT);
                endpointHolder = new EndpointHolder(newEndpoint(AlertEndpoint.class, headers),
                        newEndpoint(AutoScaleClusterV1Endpoint.class, headers),
                        newEndpoint(ConfigurationEndpoint.class, headers),
                        newEndpoint(HistoryEndpoint.class, headers),
                        newEndpoint(PolicyEndpoint.class, headers));
                logger.info("Endpoints have been renewed for AutoscaleClient");
            }
            return (T) endpointHolder.endpoints.stream().filter(e -> e.getClass().equals(clazz)).findFirst().get();
        }
        throw new TokenUnavailableException("No Refresh token provided for AutoscaleClient!");
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

        private String refreshToken;

        private String caasProtocol;

        private String caasAddress;

        private boolean debug;

        private boolean secure = true;

        private boolean ignorePreValidation;

        public AutoscaleClientBuilder(String autoscaleAddress, String caasProtocol, String caasAddress) {
            this.autoscaleAddress = autoscaleAddress;
            this.caasProtocol = caasProtocol;
            this.caasAddress = caasAddress;
        }

        public AutoscaleClientBuilder withCredential(String refreshToken) {
            this.refreshToken = refreshToken;
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
            return new AutoscaleClient(autoscaleAddress, caasProtocol, caasAddress, refreshToken, configKey);
        }
    }
}
