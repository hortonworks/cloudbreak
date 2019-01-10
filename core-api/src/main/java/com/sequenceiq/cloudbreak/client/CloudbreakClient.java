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
import com.sequenceiq.cloudbreak.api.endpoint.v3.FlexSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.KnoxServicesV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.SmartSenseSubscriptionV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4EndPoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

public class CloudbreakClient {

    protected static final Form EMPTY_FORM = new Form();

    protected static final String TOKEN_KEY = "TOKEN";

    private static final List<Class<?>> ENDPOINTS = Arrays.asList(
            AccountPreferencesEndpoint.class,
            AuditEventV4Endpoint.class,
            AutoscaleEndpoint.class,
            BlueprintV4Endpoint.class,
            DatabaseV4Endpoint.class,
            EnvironmentV4Endpoint.class,
            EventV3Endpoint.class,
            ClusterV1Endpoint.class,
            ClusterTemplateV4EndPoint.class,
            CredentialEndpoint.class,
            CredentialV3Endpoint.class,
            FlexSubscriptionEndpoint.class,
            FlexSubscriptionV3Endpoint.class,
            ImageCatalogV4Endpoint.class,
            KnoxServicesV3Endpoint.class,
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

    public EnvironmentV4Endpoint environmentV3Endpoint() {
        return getEndpoint(EnvironmentV4Endpoint.class);
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

    public KnoxServicesV3Endpoint knoxServicesV3Endpoint() {
        return getEndpoint(KnoxServicesV3Endpoint.class);
    }

    public LdapConfigV4Endpoint ldapConfigV4Endpoint() {
        return getEndpoint(LdapConfigV4Endpoint.class);
    }

    public ManagementPackEndpoint managementPackEndpoint() {
        return getEndpoint(ManagementPackEndpoint.class);
    }

    public ManagementPackV3Endpoint managementPackV3Endpoint() {
        return getEndpoint(ManagementPackV3Endpoint.class);
    }

    public KubernetesV4Endpoint kubernetesConfigV3Endpoint() {
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

    public ProxyV4Endpoint proxyConfigV4Endpoint() {
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

    public ClusterTemplateV4EndPoint clusterTemplateV4EndPoint() {
        return getEndpoint(ClusterTemplateV4EndPoint.class);
    }

    public KerberosConfigV4Endpoint kerberosConfigV4Endpoint() {
        return getEndpoint(KerberosConfigV4Endpoint.class);
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
