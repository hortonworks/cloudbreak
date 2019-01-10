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
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ConnectorV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ManagementPackEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityRuleEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SettingsEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.ConnectorV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ConnectorV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.CredentialV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.FlexSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.SmartSenseSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4EndPoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakIdentityClient {

    private static final List<Class<?>> ENDPOINTS = Arrays.asList(
            AccountPreferencesEndpoint.class,
            AuditEventV4Endpoint.class,
            AutoscaleEndpoint.class,
            BlueprintV4Endpoint.class,
            EventV3Endpoint.class,
            ClusterV1Endpoint.class,
            ClusterTemplateV4EndPoint.class,
            CredentialEndpoint.class,
            CredentialV3Endpoint.class,
            DatabaseV4Endpoint.class,
            FlexSubscriptionEndpoint.class,
            FlexSubscriptionV3Endpoint.class,
            ImageCatalogV4Endpoint.class,
            KerberosConfigV4Endpoint.class,
            LdapConfigV4Endpoint.class,
            ManagementPackEndpoint.class,
            ManagementPackV3Endpoint.class,
            KubernetesV4Endpoint.class,
            WorkspaceV4Endpoint.class,
            ConnectorV1Endpoint.class,
            ConnectorV2Endpoint.class,
            ConnectorV3Endpoint.class,
            ProxyV4Endpoint.class,
            RecipeV4Endpoint.class,
            SecurityRuleEndpoint.class,
            SettingsEndpoint.class,
            SmartSenseSubscriptionEndpoint.class,
            SmartSenseSubscriptionV3Endpoint.class,
            StackV1Endpoint.class,
            StackV2Endpoint.class,
            StackV3Endpoint.class,
            SubscriptionEndpoint.class,
            UserProfileV4Endpoint.class,
            UtilV4Endpoint.class
    );

    private static final Form EMPTY_FORM = new Form();

    private static final String TOKEN_KEY = "TOKEN";

    private static final double TOKEN_EXPIRATION_FACTOR = 0.9;

    private final Logger logger = LoggerFactory.getLogger(CloudbreakIdentityClient.class);

    private final ExpiringMap<String, String> tokenCache;

    private final Client client;

    private final IdentityClient identityClient;

    private final String cloudbreakAddress;

    private String user;

    private String password;

    private String secret;

    private WebTarget webTarget;

    private EndpointWrapperHolder endpointWrapperHolder;

    protected CloudbreakIdentityClient(String cloudbreakAddress, String identityServerAddress, String user, String password,
            String clientId, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.user = user;
        this.password = password;
        tokenCache = configTokenCache();
        logger.info("CloudbreakClient has been created with user / pass. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey);
    }

    protected CloudbreakIdentityClient(String cloudbreakAddress, String identityServerAddress, String secret, String clientId, ConfigKey configKey) {
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

    public AuditEventV4Endpoint auditV4Endpoint() {
        return getEndpoint(AuditEventV4Endpoint.class);
    }

    public AutoscaleEndpoint autoscaleEndpoint() {
        return getEndpoint(AutoscaleEndpoint.class);
    }

    public BlueprintV4Endpoint blueprintV4Endpoint() {
        return getEndpoint(BlueprintV4Endpoint.class);
    }

    public EventV3Endpoint eventV3Endpoint() {
        return getEndpoint(EventV3Endpoint.class);
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

    public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
        return getEndpoint(ImageCatalogV4Endpoint.class);
    }

    public LdapConfigV4Endpoint ldapConfigV3Endpoint() {
        return getEndpoint(LdapConfigV4Endpoint.class);
    }

    public ManagementPackEndpoint managementPackEndpoint() {
        return getEndpoint(ManagementPackEndpoint.class);
    }

    public ManagementPackV3Endpoint managementPackV3Endpoint() {
        return getEndpoint(ManagementPackV3Endpoint.class);
    }

    public KubernetesV4Endpoint kiubernetesConfigV3Endpoint() {
        return getEndpoint(KubernetesV4Endpoint.class);
    }

    public WorkspaceV4Endpoint workspaceV3Endpoint() {
        return getEndpoint(WorkspaceV4Endpoint.class);
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

    public ProxyV4Endpoint proxyConfigV3Endpoint() {
        return getEndpoint(ProxyV4Endpoint.class);
    }

    public DatabaseV4Endpoint databaseV4Endpoint() {
        return getEndpoint(DatabaseV4Endpoint.class);
    }

    public RecipeV4Endpoint recipeV4Endpoint() {
        return getEndpoint(RecipeV4Endpoint.class);
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

    public UserProfileV4Endpoint userV4Endpoint() {
        return getEndpoint(UserProfileV4Endpoint.class);
    }

    public FileSystemV4Endpoint filesystemV4Endpoint() {
        return getEndpoint(FileSystemV4Endpoint.class);
    }

    public UtilV4Endpoint utilV4Endpoint() {
        return getEndpoint(UtilV4Endpoint.class);
    }

    public WorkspaceAwareUtilV4Endpoint workspaceAwareUtilV4Endpoint() {
        return getEndpoint(WorkspaceAwareUtilV4Endpoint.class);
    }


    public ClusterTemplateV4EndPoint clusterTemplateV3EndPoint() {
        return getEndpoint(ClusterTemplateV4EndPoint.class);
    }

    public KerberosConfigV4Endpoint kerberosConfigV4Endpoint() {
        return getEndpoint(KerberosConfigV4Endpoint.class);
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

    public static class CloudbreakIdentityClientBuilder {

        private final String cloudbreakAddress;

        private final String identityServerAddress;

        private final String clientId;

        private String user;

        private String password;

        private String secret;

        private boolean debug;

        private boolean secure = true;

        private boolean ignorePreValidation;

        public CloudbreakIdentityClientBuilder(String cloudbreakAddress, String identityServerAddress, String clientId) {
            this.cloudbreakAddress = cloudbreakAddress;
            this.identityServerAddress = identityServerAddress;
            this.clientId = clientId;
        }

        public CloudbreakIdentityClientBuilder withCredential(String user, String password) {
            this.user = user;
            this.password = password;
            return this;
        }

        public CloudbreakIdentityClientBuilder withSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public CloudbreakIdentityClientBuilder withDebug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public CloudbreakIdentityClientBuilder withCertificateValidation(boolean secure) {
            this.secure = secure;
            return this;
        }

        public CloudbreakIdentityClientBuilder withIgnorePreValidation(boolean ignorePreValidation) {
            this.ignorePreValidation = ignorePreValidation;
            return this;
        }

        public CloudbreakIdentityClient build() {
            ConfigKey configKey = new ConfigKey(secure, debug, ignorePreValidation);
            return secret != null ? new CloudbreakIdentityClient(cloudbreakAddress, identityServerAddress, secret, clientId, configKey)
                    : new CloudbreakIdentityClient(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey);
        }
    }
}

