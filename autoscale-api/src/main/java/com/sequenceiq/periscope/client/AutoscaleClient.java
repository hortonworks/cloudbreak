package com.sequenceiq.periscope.client;

import java.util.Collections;
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
import com.sequenceiq.periscope.api.endpoint.AlertEndpoint;
import com.sequenceiq.periscope.api.endpoint.ClusterEndpoint;
import com.sequenceiq.periscope.api.endpoint.ConfigurationEndpoint;
import com.sequenceiq.periscope.api.endpoint.HistoryEndpoint;
import com.sequenceiq.periscope.api.endpoint.PolicyEndpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class AutoscaleClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoscaleClient.class);

    private static final Form EMPTY_FORM = new Form();
    private static final String TOKEN_KEY = "TOKEN";
    private static final double TOKEN_EXPIRATION_FACTOR = 0.9;

    private final ExpiringMap<String, String> tokenCache;

    private final Client client;
    private final IdentityClient identityClient;
    private final String autoscaleAddress;

    private String user;
    private String password;

    private String secret;

    private WebTarget t;

    private AlertEndpoint alertEndpoint;
    private ClusterEndpoint clusterEndpoint;
    private ConfigurationEndpoint configurationEndpoint;
    private HistoryEndpoint historyEndpoint;
    private PolicyEndpoint policyEndpoint;

    private AutoscaleClient(String autoscaleAddress, String identityServerAddress, String user, String password, String clientId, ConfigKey configKey) {
        this.client = RestClientUtil.get(configKey);
        this.autoscaleAddress = autoscaleAddress;
        this.identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.user = user;
        this.password = password;
        this.tokenCache = configTokenCache();
        refresh();
        LOGGER.info("AutoscaleClient has been created with user / pass. autoscale: {}, identity: {}, clientId: {}, configKey: {}", autoscaleAddress,
                identityServerAddress, clientId, configKey);
    }

    private AutoscaleClient(String autoscaleAddress, String identityServerAddress, String secret, String clientId, ConfigKey configKey) {
        this.client = RestClientUtil.get(configKey);
        this.autoscaleAddress = autoscaleAddress;
        this.identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.secret = secret;
        this.tokenCache = configTokenCache();
        refresh();
        LOGGER.info("AutoscaleClient has been created with a secret. autoscale: {}, identity: {}, clientId: {}, configKey: {}", autoscaleAddress,
                identityServerAddress, clientId, configKey);
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    private synchronized void refresh() {
        String token = tokenCache.get(TOKEN_KEY);
        if (token == null) {
            AccessToken accessToken;
            if (secret != null) {
                accessToken = identityClient.getToken(secret);
            } else {
                accessToken = identityClient.getToken(user, password);
            }
            token = accessToken.getToken();
            int exp = (int) (accessToken.getExpiresIn() * TOKEN_EXPIRATION_FACTOR);
            LOGGER.info("Token has been renewed and expires in {} seconds", exp);
            tokenCache.put(TOKEN_KEY, accessToken.getToken(), ExpirationPolicy.CREATED, exp, TimeUnit.SECONDS);
            renewEndpoints(token);
        }
    }

    private void renewEndpoints(String token) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Authorization", "Bearer " + token);
        this.t = client.target(autoscaleAddress).path(AutoscaleApi.API_ROOT_CONTEXT);
        this.alertEndpoint = newResource(AlertEndpoint.class, headers);
        this.clusterEndpoint = newResource(ClusterEndpoint.class, headers);
        this.configurationEndpoint = newResource(ConfigurationEndpoint.class, headers);
        this.historyEndpoint = newResource(HistoryEndpoint.class, headers);
        this.policyEndpoint = newResource(PolicyEndpoint.class, headers);
        LOGGER.info("Endpoints have been renewed for AutoscaleClient");
    }

    private <C> C newResource(final Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return WebResourceFactory.newResource(resourceInterface, t, false, headers, Collections.emptyList(), EMPTY_FORM);
    }

    public AlertEndpoint alertEndpoint() {
        refresh();
        return alertEndpoint;
    }

    public ClusterEndpoint clusterEndpoint() {
        refresh();
        return clusterEndpoint;
    }

    public ConfigurationEndpoint configurationEndpoint() {
        refresh();
        return configurationEndpoint;
    }

    public HistoryEndpoint historyEndpoint() {
        refresh();
        return historyEndpoint;
    }

    public PolicyEndpoint policyEndpoint() {
        refresh();
        return policyEndpoint;
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

        public AutoscaleClient build() {
            ConfigKey configKey = new ConfigKey(secure, debug);
            if (secret != null) {
                return new AutoscaleClient(autoscaleAddress, identityServerAddress, secret, clientId, configKey);
            } else {
                return new AutoscaleClient(autoscaleAddress, identityServerAddress, user, password, clientId, configKey);
            }
        }

    }

}
