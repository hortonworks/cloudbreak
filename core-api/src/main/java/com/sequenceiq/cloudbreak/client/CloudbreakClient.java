package com.sequenceiq.cloudbreak.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ConstraintTemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.TopologyEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UtilEndpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakClient.class);

    private static final Form EMPTY_FORM = new Form();

    private static final String TOKEN_KEY = "TOKEN";

    private static final double TOKEN_EXPIRATION_FACTOR = 0.9;

    private final ExpiringMap<String, String> tokenCache;

    private final Client client;

    private final IdentityClient identityClient;

    private final String cloudbreakAddress;

    private String user;

    private String password;

    private String secret;

    private WebTarget t;

    private EndpointWrapper<CredentialEndpoint> credentialEndpoint;

    private EndpointWrapper<TemplateEndpoint> templateEndpoint;

    private EndpointWrapper<TopologyEndpoint> topologyEndpoint;

    private EndpointWrapper<UsageEndpoint> usageEndpoint;

    private EndpointWrapper<UserEndpoint> userEndpoint;

    private EndpointWrapper<EventEndpoint> eventEndpoint;

    private EndpointWrapper<SecurityGroupEndpoint> securityGroupEndpoint;

    private EndpointWrapper<StackEndpoint> stackEndpoint;

    private EndpointWrapper<SubscriptionEndpoint> subscriptionEndpoint;

    private EndpointWrapper<NetworkEndpoint> networkEndpoint;

    private EndpointWrapper<RecipeEndpoint> recipeEndpoint;

    private EndpointWrapper<RdsConfigEndpoint> rdsConfigEndpoint;

    private EndpointWrapper<AccountPreferencesEndpoint> accountPreferencesEndpoint;

    private EndpointWrapper<BlueprintEndpoint> blueprintEndpoint;

    private EndpointWrapper<ClusterEndpoint> clusterEndpoint;

    private EndpointWrapper<ConnectorEndpoint> connectorEndpoint;

    private EndpointWrapper<ConstraintTemplateEndpoint> constraintTemplateEndpoint;

    private EndpointWrapper<UtilEndpoint> utilEndpoint;

    private EndpointWrapper<LdapConfigEndpoint> ldapConfigEndpoint;

    private EndpointWrapper<SmartSenseSubscriptionEndpoint> smartSenseSubscriptionEndpoint;

    private EndpointWrapper<FlexSubscriptionEndpoint> flexSubscriptionEndpoint;

    private CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String user, String password, String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.user = user;
        this.password = password;
        tokenCache = configTokenCache();
        refresh();
        LOGGER.info("CloudbreakClient has been created with user / pass. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey);
    }

    private CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String secret, String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.secret = secret;
        tokenCache = configTokenCache();
        refresh();
        LOGGER.info("CloudbreakClient has been created with a secret. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey);
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    private void refresh() {
        refresh(false);
    }

    synchronized void refresh(boolean force) {
        String token = tokenCache.get(TOKEN_KEY);
        if (force || token == null) {
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
        t = client.target(cloudbreakAddress).path(CoreApi.API_ROOT_CONTEXT);
        credentialEndpoint = newResource(credentialEndpoint, CredentialEndpoint.class, headers);
        templateEndpoint = newResource(templateEndpoint, TemplateEndpoint.class, headers);
        topologyEndpoint = newResource(topologyEndpoint, TopologyEndpoint.class, headers);
        usageEndpoint = newResource(usageEndpoint, UsageEndpoint.class, headers);
        eventEndpoint = newResource(eventEndpoint, EventEndpoint.class, headers);
        securityGroupEndpoint = newResource(securityGroupEndpoint, SecurityGroupEndpoint.class, headers);
        stackEndpoint = newResource(stackEndpoint, StackEndpoint.class, headers);
        subscriptionEndpoint = newResource(subscriptionEndpoint, SubscriptionEndpoint.class, headers);
        networkEndpoint = newResource(networkEndpoint, NetworkEndpoint.class, headers);
        recipeEndpoint = newResource(recipeEndpoint, RecipeEndpoint.class, headers);
        rdsConfigEndpoint = newResource(rdsConfigEndpoint, RdsConfigEndpoint.class, headers);
        accountPreferencesEndpoint = newResource(accountPreferencesEndpoint, AccountPreferencesEndpoint.class, headers);
        blueprintEndpoint = newResource(blueprintEndpoint, BlueprintEndpoint.class, headers);
        clusterEndpoint = newResource(clusterEndpoint, ClusterEndpoint.class, headers);
        connectorEndpoint = newResource(connectorEndpoint, ConnectorEndpoint.class, headers);
        userEndpoint = newResource(userEndpoint, UserEndpoint.class, headers);
        constraintTemplateEndpoint = newResource(constraintTemplateEndpoint, ConstraintTemplateEndpoint.class, headers);
        utilEndpoint = newResource(utilEndpoint, UtilEndpoint.class, headers);
        ldapConfigEndpoint = newResource(ldapConfigEndpoint, LdapConfigEndpoint.class, headers);
        smartSenseSubscriptionEndpoint = newResource(smartSenseSubscriptionEndpoint, SmartSenseSubscriptionEndpoint.class, headers);
        flexSubscriptionEndpoint = newResource(flexSubscriptionEndpoint, FlexSubscriptionEndpoint.class, headers);
        LOGGER.info("Endpoints have been renewed for CloudbreakClient");
    }

    private <C> EndpointWrapper<C> newResource(EndpointWrapper<C> endpointWrapper, Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        EndpointWrapper<C> result = endpointWrapper;
        if (result == null) {
            result = new EndpointWrapper<>(resourceInterface);
        }
        result.setEndpoint(WebResourceFactory.newResource(resourceInterface, t, false, headers, Collections.emptyList(), EMPTY_FORM));
        return result;
    }

    public CredentialEndpoint credentialEndpoint() {
        refresh();
        return credentialEndpoint.getEndpointProxy();
    }

    public TemplateEndpoint templateEndpoint() {
        refresh();
        return templateEndpoint.getEndpointProxy();
    }

    public TopologyEndpoint topologyEndpoint() {
        refresh();
        return topologyEndpoint.getEndpointProxy();
    }

    public UsageEndpoint usageEndpoint() {
        refresh();
        return usageEndpoint.getEndpointProxy();
    }

    public UserEndpoint userEndpoint() {
        refresh();
        return userEndpoint.getEndpointProxy();
    }

    public EventEndpoint eventEndpoint() {
        refresh();
        return eventEndpoint.getEndpointProxy();
    }

    public SecurityGroupEndpoint securityGroupEndpoint() {
        refresh();
        return securityGroupEndpoint.getEndpointProxy();
    }

    public StackEndpoint stackEndpoint() {
        refresh();
        return stackEndpoint.getEndpointProxy();
    }

    public SubscriptionEndpoint subscriptionEndpoint() {
        refresh();
        return subscriptionEndpoint.getEndpointProxy();
    }

    public NetworkEndpoint networkEndpoint() {
        refresh();
        return networkEndpoint.getEndpointProxy();
    }

    public RecipeEndpoint recipeEndpoint() {
        refresh();
        return recipeEndpoint.getEndpointProxy();
    }

    public RdsConfigEndpoint rdsConfigEndpoint() {
        refresh();
        return rdsConfigEndpoint.getEndpointProxy();
    }

    public AccountPreferencesEndpoint accountPreferencesEndpoint() {
        refresh();
        return accountPreferencesEndpoint.getEndpointProxy();
    }

    public BlueprintEndpoint blueprintEndpoint() {
        refresh();
        return blueprintEndpoint.getEndpointProxy();
    }

    public ClusterEndpoint clusterEndpoint() {
        refresh();
        return clusterEndpoint.getEndpointProxy();
    }

    public ConnectorEndpoint connectorEndpoint() {
        refresh();
        return connectorEndpoint.getEndpointProxy();
    }

    public LdapConfigEndpoint ldapConfigEndpoint() {
        refresh();
        return ldapConfigEndpoint.getEndpointProxy();
    }

    public SmartSenseSubscriptionEndpoint smartSenseSubscriptionEndpoint() {
        refresh();
        return smartSenseSubscriptionEndpoint.getEndpointProxy();
    }

    public FlexSubscriptionEndpoint flexSubscriptionEndpoint() {
        refresh();
        return flexSubscriptionEndpoint.getEndpointProxy();
    }

    public ConstraintTemplateEndpoint constraintTemplateEndpoint() {
        return constraintTemplateEndpoint.getEndpointProxy();
    }

    public UtilEndpoint utilEndpoint() {
        refresh();
        return utilEndpoint.getEndpointProxy();
    }

    private class EndpointWrapper<C> {

        private final Class<C> endpointType;

        private C endpoint;

        private final C endPointProxy;

        private final InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object result;
                try {
                    result = method.invoke(endpoint, args);
                } catch (InvocationTargetException ite) {
                    if (ite.getTargetException() instanceof NotAuthorizedException) {
                        LOGGER.warn("Unauthorized request on {}.{}(): {}, refreshing the auth token and try again...", endpointType.getCanonicalName(),
                                method.getName(), ite.getTargetException().getMessage());
                        refresh(true);
                        try {
                            result = method.invoke(endpoint, args);
                        } catch (InvocationTargetException iite) {
                            throw iite.getTargetException();
                        }
                    } else {
                        throw ite.getTargetException();
                    }
                }
                return result;
            }
        };

        private EndpointWrapper(Class<C> endpointType) {
            endPointProxy = (C) Proxy.newProxyInstance(endpointType.getClassLoader(), new Class[]{endpointType}, invocationHandler);
            this.endpointType = endpointType;
        }

        private void setEndpoint(C endpoint) {
            this.endpoint = endpoint;
        }

        private C getEndpointProxy() {
            return endPointProxy;
        }
    }

    public static class CloudbreakClientBuilder {

        private final String cloudbreakAddress;

        private final String identityServerAddress;

        private final String clientId;

        private String user;

        private String password;

        private String secret;

        private boolean debug;

        private boolean secure = true;

        private boolean ignorePreValidation;

        public CloudbreakClientBuilder(String cloudbreakAddress, String identityServerAddress, String clientId) {
            this.cloudbreakAddress = cloudbreakAddress;
            this.identityServerAddress = identityServerAddress;
            this.clientId = clientId;
        }

        public CloudbreakClientBuilder withCredential(String user, String password) {
            this.user = user;
            this.password = password;
            return this;
        }

        public CloudbreakClientBuilder withSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public CloudbreakClientBuilder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public CloudbreakClientBuilder withCertificateValidation(boolean secure) {
            this.secure = secure;
            return this;
        }

        public CloudbreakClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
            this.ignorePreValidation = ignorePreValidation;
            return this;
        }

        public CloudbreakClient build() {
            ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation);
            if (secret != null) {
                return new CloudbreakClient(cloudbreakAddress, identityServerAddress, secret, clientId, configKey);
            } else {
                return new CloudbreakClient(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey);
            }
        }

    }

}
