package com.sequenceiq.cloudbreak.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.sequenceiq.cloudbreak.api.endpoint.autoscale.AutoscaleEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.AuditEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ConnectorV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ManagementPackEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ProxyConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RepositoryConfigValidationEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityRuleEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SettingsEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UserEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.ConnectorV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.AuditV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.BlueprintV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ClusterTemplateV3EndPoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ConnectorV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.CredentialV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.FileSystemV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.FlexSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ImageCatalogV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.KnoxServicesV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.LdapConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ProxyConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.RdsConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.RecipeV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.SmartSenseSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.UserV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.UtilV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.WorkspaceV3Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakClient {

    private static final List<Class<?>> ENDPOINTS = Arrays.asList(
            AccountPreferencesEndpoint.class,
            AuditEndpoint.class,
            AuditV3Endpoint.class,
            AutoscaleEndpoint.class,
            BlueprintEndpoint.class,
            BlueprintV3Endpoint.class,
            EventEndpoint.class,
            EventV3Endpoint.class,
            UsageEndpoint.class,
            ClusterV1Endpoint.class,
            ClusterTemplateV3EndPoint.class,
            CredentialEndpoint.class,
            CredentialV3Endpoint.class,
            FlexSubscriptionEndpoint.class,
            FlexSubscriptionV3Endpoint.class,
            ImageCatalogV1Endpoint.class,
            ImageCatalogV3Endpoint.class,
            KnoxServicesV3Endpoint.class,
            LdapConfigEndpoint.class,
            LdapConfigV3Endpoint.class,
            ManagementPackEndpoint.class,
            ManagementPackV3Endpoint.class,
            WorkspaceV3Endpoint.class,
            ConnectorV1Endpoint.class,
            ConnectorV2Endpoint.class,
            ConnectorV3Endpoint.class,
            ProxyConfigEndpoint.class,
            ProxyConfigV3Endpoint.class,
            RdsConfigEndpoint.class,
            RdsConfigV3Endpoint.class,
            RecipeEndpoint.class,
            RecipeV3Endpoint.class,
            RepositoryConfigValidationEndpoint.class,
            SecurityRuleEndpoint.class,
            SettingsEndpoint.class,
            SmartSenseSubscriptionEndpoint.class,
            SmartSenseSubscriptionV3Endpoint.class,
            StackV1Endpoint.class,
            StackV2Endpoint.class,
            StackV3Endpoint.class,
            SubscriptionEndpoint.class,
            UserEndpoint.class,
            UserV3Endpoint.class,
            UtilEndpoint.class
    );

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

    public AccountPreferencesEndpoint accountPreferencesEndpoint() {
        return getEndpoint(AccountPreferencesEndpoint.class);
    }

    public AuditEndpoint auditEndpoint() {
        return getEndpoint(AuditEndpoint.class);
    }

    public AuditV3Endpoint auditV3Endpoint() {
        return getEndpoint(AuditV3Endpoint.class);
    }

    public AutoscaleEndpoint autoscaleEndpoint() {
        return getEndpoint(AutoscaleEndpoint.class);
    }

    public BlueprintEndpoint blueprintEndpoint() {
        return getEndpoint(BlueprintEndpoint.class);
    }

    public BlueprintV3Endpoint blueprintV3Endpoint() {
        return getEndpoint(BlueprintV3Endpoint.class);
    }

    public EventEndpoint eventEndpoint() {
        return getEndpoint(EventEndpoint.class);
    }

    public EventV3Endpoint eventV3Endpoint() {
        return getEndpoint(EventV3Endpoint.class);
    }

    public UsageEndpoint usageEndpoint() {
        return getEndpoint(UsageEndpoint.class);
    }

    public ClusterV1Endpoint clusterEndpoint() {
        return getEndpoint(ClusterV1Endpoint.class);
    }

    public CredentialEndpoint credentialEndpoint() {
        return getEndpoint(CredentialEndpoint.class);
    }

    public CredentialV3Endpoint credentialV3Endpoint() {
        return getEndpoint(CredentialV3Endpoint.class);
    }

    public FlexSubscriptionEndpoint flexSubscriptionEndpoint() {
        return getEndpoint(FlexSubscriptionEndpoint.class);
    }

    public FlexSubscriptionV3Endpoint flexSubscriptionV3Endpoint() {
        return getEndpoint(FlexSubscriptionV3Endpoint.class);
    }

    public ImageCatalogV1Endpoint imageCatalogEndpoint() {
        return getEndpoint(ImageCatalogV1Endpoint.class);
    }

    public ImageCatalogV3Endpoint imageCatalogV3Endpoint() {
        return getEndpoint(ImageCatalogV3Endpoint.class);
    }

    public KnoxServicesV3Endpoint knoxServicesV3Endpoint() {
        return getEndpoint(KnoxServicesV3Endpoint.class);
    }

    public LdapConfigEndpoint ldapConfigEndpoint() {
        return getEndpoint(LdapConfigEndpoint.class);
    }

    public LdapConfigV3Endpoint ldapConfigV3Endpoint() {
        return getEndpoint(LdapConfigV3Endpoint.class);
    }

    public ManagementPackEndpoint managementPackEndpoint() {
        return getEndpoint(ManagementPackEndpoint.class);
    }

    public ManagementPackV3Endpoint managementPackV3Endpoint() {
        return getEndpoint(ManagementPackV3Endpoint.class);
    }

    public WorkspaceV3Endpoint workspaceV3Endpoint() {
        return getEndpoint(WorkspaceV3Endpoint.class);
    }

    public ConnectorV1Endpoint connectorV1Endpoint() {
        return getEndpoint(ConnectorV1Endpoint.class);
    }

    public ConnectorV2Endpoint connectorV2Endpoint() {
        return getEndpoint(ConnectorV2Endpoint.class);
    }

    public ConnectorV3Endpoint connectorV3Endpoint() {
        return getEndpoint(ConnectorV3Endpoint.class);
    }

    public ProxyConfigEndpoint proxyConfigEndpoint() {
        return getEndpoint(ProxyConfigEndpoint.class);
    }

    public ProxyConfigV3Endpoint proxyConfigV3Endpoint() {
        return getEndpoint(ProxyConfigV3Endpoint.class);
    }

    public RdsConfigEndpoint rdsConfigEndpoint() {
        return getEndpoint(RdsConfigEndpoint.class);
    }

    public RdsConfigV3Endpoint rdsConfigV3Endpoint() {
        return getEndpoint(RdsConfigV3Endpoint.class);
    }

    public RecipeEndpoint recipeEndpoint() {
        return getEndpoint(RecipeEndpoint.class);
    }

    public RecipeV3Endpoint recipeV3Endpoint() {
        return getEndpoint(RecipeV3Endpoint.class);
    }

    public RepositoryConfigValidationEndpoint repositoryConfigValidationEndpoint() {
        return getEndpoint(RepositoryConfigValidationEndpoint.class);
    }

    public SecurityRuleEndpoint securityRuleEndpoint() {
        return getEndpoint(SecurityRuleEndpoint.class);
    }

    public SettingsEndpoint settingsEndpoint() {
        return getEndpoint(SettingsEndpoint.class);
    }

    public SmartSenseSubscriptionEndpoint smartSenseSubscriptionEndpoint() {
        return getEndpoint(SmartSenseSubscriptionEndpoint.class);
    }

    public SmartSenseSubscriptionV3Endpoint smartSenseSubscriptionV3Endpoint() {
        return getEndpoint(SmartSenseSubscriptionV3Endpoint.class);
    }

    public StackV1Endpoint stackV1Endpoint() {
        return getEndpoint(StackV1Endpoint.class);
    }

    public StackV2Endpoint stackV2Endpoint() {
        return getEndpoint(StackV2Endpoint.class);
    }

    public StackV3Endpoint stackV3Endpoint() {
        return getEndpoint(StackV3Endpoint.class);
    }

    public SubscriptionEndpoint subscriptionEndpoint() {
        return getEndpoint(SubscriptionEndpoint.class);
    }

    public UserEndpoint userEndpoint() {
        return getEndpoint(UserEndpoint.class);
    }

    public UserV3Endpoint userV3Endpoint() {
        return getEndpoint(UserV3Endpoint.class);
    }

    public UtilEndpoint utilEndpoint() {
        return getEndpoint(UtilEndpoint.class);
    }

    public FileSystemV3Endpoint filesystemV3Endpoint() {
        return getEndpoint(FileSystemV3Endpoint.class);
    }

    public UtilV3Endpoint utilV3Endpoint() {
        return getEndpoint(UtilV3Endpoint.class);
    }

    public ClusterTemplateV3EndPoint clusterTemplateV3EndPoint() {
        return getEndpoint(ClusterTemplateV3EndPoint.class);
    }

    protected <E> E getEndpoint(Class<E> clazz) {
        return refreshIfNeededAndGet(clazz);
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
        ENDPOINTS.forEach(e -> endpointWrapperHolder.setEndpoint(newEndpoint(e, headers)));
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
