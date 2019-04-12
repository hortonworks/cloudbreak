package com.sequenceiq.cloudbreak.client;

import static java.lang.String.format;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.ManagementPackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.UserV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakClient {

    protected static final Form EMPTY_FORM = new Form();

    protected static final String TOKEN_KEY = "TOKEN";

    private static final List<Class<?>> ENDPOINTS = Arrays.asList(
            AuditEventV4Endpoint.class,
            AutoscaleV4Endpoint.class,
            ClusterDefinitionV4Endpoint.class,
            DatabaseV4Endpoint.class,
            EnvironmentV4Endpoint.class,
            EventV4Endpoint.class,
            ClusterTemplateV4Endpoint.class,
            CredentialV4Endpoint.class,
            ImageCatalogV4Endpoint.class,
            LdapConfigV4Endpoint.class,
            ManagementPackV4Endpoint.class,
            KubernetesV4Endpoint.class,
            ConnectorV4Endpoint.class,
            ProxyV4Endpoint.class,
            RecipeV4Endpoint.class,
            StackV4Endpoint.class,
            UserProfileV4Endpoint.class,
            UserV4Endpoint.class,
            UtilV4Endpoint.class,
            WorkspaceAwareUtilV4Endpoint.class,
            KerberosConfigV4Endpoint.class
    );

    private final Client client;

    private final String cloudbreakAddress;

    private WebTarget webTarget;

    private final Logger logger = LoggerFactory.getLogger(CloudbreakClient.class);

    private final CaasClient caasClient;

    private final ExpiringMap<String, String> tokenCache;

    private EndpointWrapperHolder endpointWrapperHolder;

    private String refreshToken;

    protected CloudbreakClient(String cloudbreakAddress, String caasProtocol, String caasAddress, String refreshToken, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        this.refreshToken = refreshToken;
        caasClient = new CaasClient(caasProtocol, caasAddress, configKey);
        tokenCache = configTokenCache();
        logger.info("CloudbreakClient has been created with token. cloudbreak: {}, refreshToken: {}, configKey: {}", cloudbreakAddress,
                refreshToken, configKey);
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

    public EnvironmentV4Endpoint environmentV4Endpoint() {
        return getEndpoint(EnvironmentV4Endpoint.class);
    }

    public EventV4Endpoint eventV3Endpoint() {
        return getEndpoint(EventV4Endpoint.class);
    }

    public CredentialV4Endpoint credentialV4Endpoint() {
        return getEndpoint(CredentialV4Endpoint.class);
    }

    public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
        return getEndpoint(ImageCatalogV4Endpoint.class);
    }

    public LdapConfigV4Endpoint ldapConfigV4Endpoint() {
        return getEndpoint(LdapConfigV4Endpoint.class);
    }

    public ManagementPackV4Endpoint managementPackV4Endpoint() {
        return getEndpoint(ManagementPackV4Endpoint.class);
    }

    public KubernetesV4Endpoint kubernetesV4Endpoint() {
        return getEndpoint(KubernetesV4Endpoint.class);
    }

    public ConnectorV4Endpoint connectorV4Endpoint() {
        return getEndpoint(ConnectorV4Endpoint.class);
    }

    public ProxyV4Endpoint proxyConfigV4Endpoint() {
        return getEndpoint(ProxyV4Endpoint.class);
    }

    public DatabaseV4Endpoint databaseV4Endpoint() {
        return getEndpoint(DatabaseV4Endpoint.class);
    }

    public RecipeV4Endpoint recipeV4Endpoint() {
        return getEndpoint(RecipeV4Endpoint.class);
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

    public ClusterTemplateV4Endpoint clusterTemplateV4EndPoint() {
        return getEndpoint(ClusterTemplateV4Endpoint.class);
    }

    public KerberosConfigV4Endpoint kerberosConfigV4Endpoint() {
        return getEndpoint(KerberosConfigV4Endpoint.class);
    }

    public StackV4Endpoint stackV4Endpoint() {
        return getEndpoint(StackV4Endpoint.class);
    }

    protected <E> E getEndpoint(Class<E> clazz) {
        return refreshIfNeededAndGet(clazz);
    }

    protected Client getClient() {
        return client;
    }

    protected void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    protected synchronized <T> T refreshIfNeededAndGet(Class<T> clazz) {
        return refreshIfNeededAndGet(clazz, false);
    }

    protected String getCloudbreakAddress() {
        return cloudbreakAddress;
    }

    protected WebTarget getWebTarget() {
        return webTarget;
    }

    protected void setWebTarget(WebTarget webTarget) {
        this.webTarget = webTarget;
    }

    @SuppressWarnings("unchecked")
    private <T> T refreshIfNeededAndGet(@Nullable Class<T> clazz, boolean forced) {
        if (refreshToken != null) {
            String accessToken = tokenCache.get(TOKEN_KEY);
            if (forced || accessToken == null || endpointWrapperHolder == null) {
                TokenRequest tokenRequest = new TokenRequest();
                tokenRequest.setRefreshToken(refreshToken);
                accessToken = caasClient.getAccessToken(tokenRequest);
                tokenCache.put(TOKEN_KEY, accessToken, ExpirationPolicy.CREATED, 1, TimeUnit.MINUTES);
                refreshEndpointWrapperHolder(accessToken);
            }
            return (T) getRequiredEndpoint(clazz)
                    .orElseThrow(() -> new EndpointConfigurationException(format("Endpoint [%s] has not added to the endpoint list!", clazz)));
        }
        throw new TokenUnavailableException("No Refresh token provided for CloudbreakClient!");
    }

    private void refreshEndpointWrapperHolder(String token) {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("Authorization", "Bearer " + token);
        webTarget = client.target(cloudbreakAddress).path(CoreApi.API_ROOT_CONTEXT);
        endpointWrapperHolder = Optional.ofNullable(endpointWrapperHolder).orElse(new EndpointWrapperHolder());
        ENDPOINTS.forEach(e -> endpointWrapperHolder.setEndpoint(newEndpoint(e, headers)));
        logger.info("Endpoints have been renewed for CloudbreakClient");
    }

    private <T> Optional<?> getRequiredEndpoint(@Nullable Class<T> clazz) {
        return endpointWrapperHolder.endpoints.stream()
                .filter(e -> e.endpointType.equals(clazz))
                .map(e -> e.endPointProxy)
                .findFirst();
    }

    private ExpiringMap<String, String> configTokenCache() {
        return ExpiringMap.builder().variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();
    }

    private <C> Pair<Class<C>, C> newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return new ImmutablePair<>(resourceInterface,
                WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM));
    }

    @SuppressWarnings("unchecked")
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
                        logger.debug("Unauthorized request on {}.{}(): {}, refreshing the auth token and try again...", endpointType.getCanonicalName(),
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

        @SuppressWarnings("unchecked")
        private EndpointWrapper(Class<C> endpointType) {
            this.endpointType = endpointType;
            endPointProxy = (C) Proxy.newProxyInstance(endpointType.getClassLoader(), new Class[]{endpointType}, invocationHandler);
        }
    }

    public static class CloudbreakClientBuilder {

        private final String cloudbreakAddress;

        private String token;

        private String caasProtocol;

        private String caasAddress;

        private boolean debug;

        private boolean secure = true;

        private boolean ignorePreValidation;

        public CloudbreakClientBuilder(String cloudbreakAddress, String caasProtocol, String caasAddress) {
            this.cloudbreakAddress = cloudbreakAddress;
            this.caasProtocol = caasProtocol;
            this.caasAddress = caasAddress;
        }

        public CloudbreakClientBuilder withCredential(String token) {
            this.token = token;
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
            return new CloudbreakClient(cloudbreakAddress, caasProtocol, caasAddress, token, configKey);
        }
    }
}
