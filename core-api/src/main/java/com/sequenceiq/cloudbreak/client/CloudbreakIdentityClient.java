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
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.ClusterDefinitionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.ConnectorV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.FlexSubscriptionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.ManagementPackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.smartsense.SmartSenseSubscriptionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.UserV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakIdentityClient {

    private static final List<Class<?>> ENDPOINTS = Arrays.asList(
            AuditEventV4Endpoint.class,
            AutoscaleV4Endpoint.class,
            ClusterDefinitionV4Endpoint.class,
            EventV4Endpoint.class,
            ClusterTemplateV4Endpoint.class,
            CredentialV4Endpoint.class,
            DatabaseV4Endpoint.class,
            FlexSubscriptionV4Endpoint.class,
            ImageCatalogV4Endpoint.class,
            KerberosConfigV4Endpoint.class,
            LdapConfigV4Endpoint.class,
            ManagementPackV4Endpoint.class,
            KubernetesV4Endpoint.class,
            WorkspaceV4Endpoint.class,
            ConnectorV4Endpoint.class,
            ProxyV4Endpoint.class,
            RecipeV4Endpoint.class,
            SmartSenseSubscriptionV4Endpoint.class,
            UserProfileV4Endpoint.class,
            UserV4Endpoint.class,
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

    public AuditEventV4Endpoint auditV4Endpoint() {
        return getEndpoint(AuditEventV4Endpoint.class);
    }

    public AutoscaleV4Endpoint autoscaleEndpoint() {
        return getEndpoint(AutoscaleV4Endpoint.class);
    }

    public ClusterDefinitionV4Endpoint clusterDefinitionV4Endpoint() {
        return getEndpoint(ClusterDefinitionV4Endpoint.class);
    }

    public EventV4Endpoint eventV3Endpoint() {
        return getEndpoint(EventV4Endpoint.class);
    }

    public CredentialV4Endpoint credentialV4Endpoint() {
        return getEndpoint(CredentialV4Endpoint.class);
    }

    public FlexSubscriptionV4Endpoint flexSubscriptionV4Endpoint() {
        return getEndpoint(FlexSubscriptionV4Endpoint.class);
    }

    public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
        return getEndpoint(ImageCatalogV4Endpoint.class);
    }

    public LdapConfigV4Endpoint ldapConfigV3Endpoint() {
        return getEndpoint(LdapConfigV4Endpoint.class);
    }

    public ManagementPackV4Endpoint managementPackV3Endpoint() {
        return getEndpoint(ManagementPackV4Endpoint.class);
    }

    public KubernetesV4Endpoint kiubernetesConfigV3Endpoint() {
        return getEndpoint(KubernetesV4Endpoint.class);
    }

    public WorkspaceV4Endpoint workspaceV3Endpoint() {
        return getEndpoint(WorkspaceV4Endpoint.class);
    }

    public ConnectorV4Endpoint connectorV3Endpoint() {
        return getEndpoint(ConnectorV4Endpoint.class);
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

    public SmartSenseSubscriptionV4Endpoint smartSenseSubscriptionV4Endpoint() {
        return getEndpoint(SmartSenseSubscriptionV4Endpoint.class);
    }

    public UserProfileV4Endpoint userProfileV4Endpoint() {
        return getEndpoint(UserProfileV4Endpoint.class);
    }

    public UserV4Endpoint userV4Endpoint() {
        return getEndpoint(UserV4Endpoint.class);
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

    public ClusterTemplateV4Endpoint clusterTemplateV3EndPoint() {
        return getEndpoint(ClusterTemplateV4Endpoint.class);
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

