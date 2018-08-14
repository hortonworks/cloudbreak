package com.sequenceiq.cloudbreak.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ConnectorV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.OrganizationV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ProxyConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RepositoryConfigValidationEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityRuleEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UserEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.ConnectorV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ImageCatalogV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.RdsConfigV3Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakClient {

    private static final Form EMPTY_FORM = new Form();

    private static final String TOKEN_KEY = "TOKEN";

    private static final double TOKEN_EXPIRATION_FACTOR = 0.9;

    private final Logger logger = LoggerFactory.getLogger(CloudbreakClient.class);

    private final ExpiringMap<String, String> tokenCache;

    private final Client client;

    private final IdentityClient identityClient;

    private final String cloudbreakAddress;

    private String user;

    private String password;

    private String secret;

    private WebTarget webTarget;

    private EndpointWrapperHolder endpointWrapperHolder;

    protected CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String user, String password, String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.user = user;
        this.password = password;
        tokenCache = configTokenCache();
        logger.info("CloudbreakClient has been created with user / pass. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey);
    }

    protected CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String secret, String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.secret = secret;
        tokenCache = configTokenCache();
        logger.info("CloudbreakClient has been created with a secret. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey);
    }

    public CredentialEndpoint credentialEndpoint() {
        return refreshIfNeededAndGet(CredentialEndpoint.class);
    }

    public UsageEndpoint usageEndpoint() {
        return refreshIfNeededAndGet(UsageEndpoint.class);
    }

    public UserEndpoint userEndpoint() {
        return refreshIfNeededAndGet(UserEndpoint.class);
    }

    public EventEndpoint eventEndpoint() {
        return refreshIfNeededAndGet(EventEndpoint.class);
    }

    public SecurityRuleEndpoint securityRuleEndpoint() {
        return refreshIfNeededAndGet(SecurityRuleEndpoint.class);
    }

    public StackV1Endpoint stackV1Endpoint() {
        return refreshIfNeededAndGet(StackV1Endpoint.class);
    }

    public StackV2Endpoint stackV2Endpoint() {
        return refreshIfNeededAndGet(StackV2Endpoint.class);
    }

    public SubscriptionEndpoint subscriptionEndpoint() {
        return refreshIfNeededAndGet(SubscriptionEndpoint.class);
    }

    public RecipeEndpoint recipeEndpoint() {
        return refreshIfNeededAndGet(RecipeEndpoint.class);
    }

    public OrganizationV3Endpoint organizationEndpoint() {
        return refreshIfNeededAndGet(OrganizationV3Endpoint.class);
    }

    public RdsConfigEndpoint rdsConfigEndpoint() {
        return refreshIfNeededAndGet(RdsConfigEndpoint.class);
    }

    public ProxyConfigEndpoint proxyConfigEndpoint() {
        return refreshIfNeededAndGet(ProxyConfigEndpoint.class);
    }

    public AccountPreferencesEndpoint accountPreferencesEndpoint() {
        return refreshIfNeededAndGet(AccountPreferencesEndpoint.class);
    }

    public BlueprintEndpoint blueprintEndpoint() {
        return refreshIfNeededAndGet(BlueprintEndpoint.class);
    }

    public ClusterV1Endpoint clusterEndpoint() {
        return refreshIfNeededAndGet(ClusterV1Endpoint.class);
    }

    public ConnectorV1Endpoint connectorV1Endpoint() {
        return refreshIfNeededAndGet(ConnectorV1Endpoint.class);
    }

    public ConnectorV2Endpoint connectorV2Endpoint() {
        return refreshIfNeededAndGet(ConnectorV2Endpoint.class);
    }

    public LdapConfigEndpoint ldapConfigEndpoint() {
        return refreshIfNeededAndGet(LdapConfigEndpoint.class);
    }

    public SmartSenseSubscriptionEndpoint smartSenseSubscriptionEndpoint() {
        return refreshIfNeededAndGet(SmartSenseSubscriptionEndpoint.class);
    }

    public FlexSubscriptionEndpoint flexSubscriptionEndpoint() {
        return refreshIfNeededAndGet(FlexSubscriptionEndpoint.class);
    }

    public ImageCatalogV1Endpoint imageCatalogEndpoint() {
        return refreshIfNeededAndGet(ImageCatalogV1Endpoint.class);
    }

    public UtilEndpoint utilEndpoint() {
        return refreshIfNeededAndGet(UtilEndpoint.class);
    }

    public ManagementPackV3Endpoint managementPackV3Endpoint() {
        return refreshIfNeededAndGet(ManagementPackV3Endpoint.class);
    }

    public ImageCatalogV3Endpoint imageCatalogV3Endpoint() {
        return refreshIfNeededAndGet(ImageCatalogV3Endpoint.class);
    }

    public RdsConfigV3Endpoint rdsConfigV3Endpoint() {
        return refreshIfNeededAndGet(RdsConfigV3Endpoint.class);
    }

    public RepositoryConfigValidationEndpoint repositoryConfigValidationEndpoint() {
        return refreshIfNeededAndGet(RepositoryConfigValidationEndpoint.class);
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    protected synchronized <T> T refreshIfNeededAndGet(Class<T> clazz) {
        return refreshIfNeededAndGet(clazz, false);
    }

    protected synchronized <T> T refreshIfNeededAndGet(@Nullable Class<T> clazz, boolean forced) {
        String token = tokenCache.get(TOKEN_KEY);
        if (token == null || endpointWrapperHolder == null) {
            AccessToken accessToken;
            accessToken = secret != null ? identityClient.getToken(secret) : identityClient.getToken(user, password);
            token = accessToken.getToken();
            int exp = (int) (accessToken.getExpiresIn() * TOKEN_EXPIRATION_FACTOR);
            logger.info("Token has been renewed and expires in {} seconds", exp);
            tokenCache.put(TOKEN_KEY, accessToken.getToken(), ExpirationPolicy.CREATED, exp, TimeUnit.SECONDS);
            refreshEndpointWrapperHolder(token);
        }
        Optional<?> first = endpointWrapperHolder.endpoints.stream()
                .filter(e -> e.endpointType.equals(clazz))
                .map(e -> e.endPointProxy)
                .findFirst();
        if (first.isPresent()) {
            return (T) first.get();
        } else {
            return null;
        }
    }

    protected void refreshEndpointWrapperHolder(String token) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Authorization", "Bearer " + token);
        webTarget = client.target(cloudbreakAddress).path(CoreApi.API_ROOT_CONTEXT);
        endpointWrapperHolder = Optional.ofNullable(endpointWrapperHolder).orElse(new EndpointWrapperHolder());
        endpointWrapperHolder.setEndpoint(newEndpoint(CredentialEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(UsageEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(EventEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(SecurityRuleEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(StackV1Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(StackV2Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(SubscriptionEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(RecipeEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(RdsConfigEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ProxyConfigEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(AccountPreferencesEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(BlueprintEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ClusterV1Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ConnectorV1Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ConnectorV2Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(UserEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(UtilEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(LdapConfigEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(SmartSenseSubscriptionEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(FlexSubscriptionEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ImageCatalogV1Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(RepositoryConfigValidationEndpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ManagementPackV3Endpoint.class, headers));
        endpointWrapperHolder.setEndpoint(newEndpoint(ImageCatalogV3Endpoint.class, headers));
        logger.info("Endpoints have been renewed for CloudbreakClient");
    }

    private <C> Pair<Class<C>, C> newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return new ImmutablePair<>(resourceInterface,
                WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM));
    }

    private class EndpointWrapperHolder {
        private final List<EndpointWrapper<?>> endpoints = new ArrayList<>();

        private <C> void setEndpoint(Pair<Class<C>, C> details) {
            EndpointWrapper<C> wrapper = (EndpointWrapper<C>) endpoints.stream()
                    .filter(e -> e.endpointType.equals(details.getLeft()))
                    .findFirst().orElse(new EndpointWrapper<>(details.getLeft()));
            if (wrapper.endpoint == null) {
                endpoints.add(wrapper);
            }
            wrapper.endpoint = details.getRight();
        }
    }

    private class EndpointWrapper<C> {

        private final Class<C> endpointType;

        private final C endPointProxy;

        private final InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                try {
                    return method.invoke(endpoint, args);
                } catch (InvocationTargetException ite) {
                    if (ite.getTargetException() instanceof NotAuthorizedException) {
                        logger.warn("Unauthorized request on {}.{}(): {}, refreshing the auth token and try again...", endpointType.getCanonicalName(),
                                method.getName(), ite.getTargetException().getMessage());
                        refreshIfNeededAndGet(null, true);
                        try {
                            return method.invoke(endpoint, args);
                        } catch (InvocationTargetException iite) {
                            throw iite.getTargetException();
                        }
                    }
                    throw ite.getTargetException();
                }
            }
        };

        private C endpoint;

        private EndpointWrapper(Class<C> endpointType) {
            this.endpointType = endpointType;
            endPointProxy = (C) Proxy.newProxyInstance(endpointType.getClassLoader(), new Class[]{endpointType}, invocationHandler);
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
            return secret != null ? new CloudbreakClient(cloudbreakAddress, identityServerAddress, secret, clientId, configKey)
                    : new CloudbreakClient(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey);
        }
    }
}
