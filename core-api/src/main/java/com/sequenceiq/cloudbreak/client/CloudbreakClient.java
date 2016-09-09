package com.sequenceiq.cloudbreak.client;

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

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ConstraintTemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SssdConfigEndpoint;
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

    private CredentialEndpoint credentialEndpoint;
    private TemplateEndpoint templateEndpoint;
    private TopologyEndpoint topologyEndpoint;
    private UsageEndpoint usageEndpoint;
    private UserEndpoint userEndpoint;
    private EventEndpoint eventEndpoint;
    private SecurityGroupEndpoint securityGroupEndpoint;
    private StackEndpoint stackEndpoint;
    private SubscriptionEndpoint subscriptionEndpoint;
    private NetworkEndpoint networkEndpoint;
    private RecipeEndpoint recipeEndpoint;
    private SssdConfigEndpoint sssdConfigEndpoint;
    private RdsConfigEndpoint rdsConfigEndpoint;
    private AccountPreferencesEndpoint accountPreferencesEndpoint;
    private BlueprintEndpoint blueprintEndpoint;
    private ClusterEndpoint clusterEndpoint;
    private ConnectorEndpoint connectorEndpoint;
    private ConstraintTemplateEndpoint constraintTemplateEndpoint;
    private UtilEndpoint utilEndpoint;
    private LdapConfigEndpoint ldapConfigEndpoint;

    private CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String user, String password, String clientId, ConfigKey configKey) {
        this.client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        this.identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.user = user;
        this.password = password;
        this.tokenCache = configTokenCache();
        refresh();
        LOGGER.info("CloudbreakClient has been created with user / pass. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
                identityServerAddress, clientId, configKey);
    }

    private CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String secret, String clientId, ConfigKey configKey) {
        this.client = RestClientUtil.get(configKey);
        this.cloudbreakAddress = cloudbreakAddress;
        this.identityClient = new IdentityClient(identityServerAddress, clientId, configKey);
        this.secret = secret;
        this.tokenCache = configTokenCache();
        refresh();
        LOGGER.info("CloudbreakClient has been created with a secret. cloudbreak: {}, identity: {}, clientId: {}, configKey: {}", cloudbreakAddress,
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
        this.t = client.target(cloudbreakAddress).path(CoreApi.API_ROOT_CONTEXT);
        this.credentialEndpoint = newResource(CredentialEndpoint.class, headers);
        this.templateEndpoint = newResource(TemplateEndpoint.class, headers);
        this.topologyEndpoint = newResource(TopologyEndpoint.class, headers);
        this.usageEndpoint = newResource(UsageEndpoint.class, headers);
        this.eventEndpoint = newResource(EventEndpoint.class, headers);
        this.securityGroupEndpoint = newResource(SecurityGroupEndpoint.class, headers);
        this.stackEndpoint = newResource(StackEndpoint.class, headers);
        this.subscriptionEndpoint = newResource(SubscriptionEndpoint.class, headers);
        this.networkEndpoint = newResource(NetworkEndpoint.class, headers);
        this.recipeEndpoint = newResource(RecipeEndpoint.class, headers);
        this.sssdConfigEndpoint = newResource(SssdConfigEndpoint.class, headers);
        this.rdsConfigEndpoint = newResource(RdsConfigEndpoint.class, headers);
        this.accountPreferencesEndpoint = newResource(AccountPreferencesEndpoint.class, headers);
        this.blueprintEndpoint = newResource(BlueprintEndpoint.class, headers);
        this.clusterEndpoint = newResource(ClusterEndpoint.class, headers);
        this.connectorEndpoint = newResource(ConnectorEndpoint.class, headers);
        this.userEndpoint = newResource(UserEndpoint.class, headers);
        this.constraintTemplateEndpoint = newResource(ConstraintTemplateEndpoint.class, headers);
        this.utilEndpoint = newResource(UtilEndpoint.class, headers);
        this.ldapConfigEndpoint = newResource(LdapConfigEndpoint.class, headers);
        LOGGER.info("Endpoints have been renewed for CloudbreakClient");
    }

    private <C> C newResource(final Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
        return WebResourceFactory.newResource(resourceInterface, t, false, headers, Collections.emptyList(), EMPTY_FORM);
    }

    public CredentialEndpoint credentialEndpoint() {
        refresh();
        return credentialEndpoint;
    }

    public TemplateEndpoint templateEndpoint() {
        refresh();
        return templateEndpoint;
    }

    public TopologyEndpoint topologyEndpoint() {
        refresh();
        return topologyEndpoint;
    }

    public UsageEndpoint usageEndpoint() {
        refresh();
        return usageEndpoint;
    }

    public UserEndpoint userEndpoint() {
        refresh();
        return userEndpoint;
    }

    public EventEndpoint eventEndpoint() {
        refresh();
        return eventEndpoint;
    }

    public SecurityGroupEndpoint securityGroupEndpoint() {
        refresh();
        return securityGroupEndpoint;
    }

    public StackEndpoint stackEndpoint() {
        refresh();
        return stackEndpoint;
    }

    public SubscriptionEndpoint subscriptionEndpoint() {
        refresh();
        return subscriptionEndpoint;
    }

    public NetworkEndpoint networkEndpoint() {
        refresh();
        return networkEndpoint;
    }

    public RecipeEndpoint recipeEndpoint() {
        refresh();
        return recipeEndpoint;
    }

    public SssdConfigEndpoint sssdConfigEndpoint() {
        refresh();
        return sssdConfigEndpoint;
    }

    public RdsConfigEndpoint rdsConfigEndpoint() {
        refresh();
        return rdsConfigEndpoint;
    }

    public AccountPreferencesEndpoint accountPreferencesEndpoint() {
        refresh();
        return accountPreferencesEndpoint;
    }

    public BlueprintEndpoint blueprintEndpoint() {
        refresh();
        return blueprintEndpoint;
    }

    public ClusterEndpoint clusterEndpoint() {
        refresh();
        return clusterEndpoint;
    }

    public ConnectorEndpoint connectorEndpoint() {
        refresh();
        return connectorEndpoint;
    }

    public LdapConfigEndpoint ldapConfigEndpoint() {
        refresh();
        return ldapConfigEndpoint;
    }

    public ConstraintTemplateEndpoint constraintTemplateEndpoint() {
        return constraintTemplateEndpoint;
    }

    public UtilEndpoint utilEndpoint() {
        refresh();
        return utilEndpoint;
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

        public CloudbreakClient build() {
            ConfigKey configKey = new ConfigKey(secure, debug);
            if (secret != null) {
                return new CloudbreakClient(cloudbreakAddress, identityServerAddress, secret, clientId, configKey);
            } else {
                return new CloudbreakClient(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey);
            }
        }

    }

}
