package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.endpoint.v1.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ConnectorV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ConstraintTemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.FlexSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ImageCatalogV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.LdapConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ProxyConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RdsConfigEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.RepositoryConfigValidationEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SecurityRuleEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SmartSenseSubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.TopologyEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UserEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.UtilEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.ConnectorV2Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v2.StackV2Endpoint;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ProxyCloudbreakClient extends com.sequenceiq.cloudbreak.client.CloudbreakClient {

    private static final String CAST_EXCEPTION_MESSAGE = "The provided ../newway.Entity is not castable to a StackEntity";

    private static final String BEFORE_MESSAGE = " Stack post request:\n";

    private static final String AFTER_MESSAGE = " Stack post response:\n";

    public ProxyCloudbreakClient(String cloudbreakAddress, String identityServerAddress, String user, String password, String clientId,
                                    ConfigKey configKey) {
        super(cloudbreakAddress, identityServerAddress, user, password, clientId, configKey);
    }

    protected ProxyCloudbreakClient(String cloudbreakAddress, String identityServerAddress, String secret, String clientId,
                                    ConfigKey configKey) {
        super(cloudbreakAddress, identityServerAddress, secret, clientId, configKey);
    }

    public CredentialEndpoint credentialEndpoint() {
        return createProxy(super.credentialEndpoint(), CredentialEndpoint.class);
    }

    public TemplateEndpoint templateEndpoint() {
        return createProxy(super.templateEndpoint(), TemplateEndpoint.class);
    }

    public TopologyEndpoint topologyEndpoint() {
        return createProxy(super.topologyEndpoint(), TopologyEndpoint.class);
    }

    public UsageEndpoint usageEndpoint() {
        return createProxy(super.usageEndpoint(), UsageEndpoint.class);
    }

    public UserEndpoint userEndpoint() {
        return createProxy(super.userEndpoint(), UserEndpoint.class);
    }

    public EventEndpoint eventEndpoint() {
        return createProxy(super.eventEndpoint(), EventEndpoint.class);
    }

    public SecurityGroupEndpoint securityGroupEndpoint() {
        return createProxy(super.securityGroupEndpoint(), SecurityGroupEndpoint.class);
    }

    public SecurityRuleEndpoint securityRuleEndpoint() {
        return createProxy(super.securityRuleEndpoint(), SecurityRuleEndpoint.class);
    }

    public StackV1Endpoint stackV1Endpoint() {
        return createProxy(super.stackV1Endpoint(), StackV1Endpoint.class);
    }

    public StackV2Endpoint stackV2Endpoint() {
        return createProxy(super.stackV2Endpoint(), StackV2Endpoint.class);
    }

    public SubscriptionEndpoint subscriptionEndpoint() {
        return createProxy(super.subscriptionEndpoint(), SubscriptionEndpoint.class);
    }

    public NetworkEndpoint networkEndpoint() {
        return createProxy(super.networkEndpoint(), NetworkEndpoint.class);
    }

    public RecipeEndpoint recipeEndpoint() {
        return createProxy(super.recipeEndpoint(), RecipeEndpoint.class);
    }

    public RdsConfigEndpoint rdsConfigEndpoint() {
        return createProxy(super.rdsConfigEndpoint(), RdsConfigEndpoint.class);
    }

    public ProxyConfigEndpoint proxyConfigEndpoint() {
        return createProxy(super.proxyConfigEndpoint(), ProxyConfigEndpoint.class);
    }

    public AccountPreferencesEndpoint accountPreferencesEndpoint() {
        return createProxy(super.accountPreferencesEndpoint(), AccountPreferencesEndpoint.class);
    }

    public BlueprintEndpoint blueprintEndpoint() {
        return createProxy(super.blueprintEndpoint(), BlueprintEndpoint.class);
    }

    public ClusterV1Endpoint clusterEndpoint() {
        return createProxy(super.clusterEndpoint(), ClusterV1Endpoint.class);
    }

    public ConnectorV1Endpoint connectorV1Endpoint() {
        return createProxy(super.connectorV1Endpoint(), ConnectorV1Endpoint.class);
    }

    public ConnectorV2Endpoint connectorV2Endpoint() {
        return createProxy(super.connectorV2Endpoint(), ConnectorV2Endpoint.class);
    }

    public LdapConfigEndpoint ldapConfigEndpoint() {
        return createProxy(super.ldapConfigEndpoint(), LdapConfigEndpoint.class);
    }

    public SmartSenseSubscriptionEndpoint smartSenseSubscriptionEndpoint() {
        return createProxy(super.smartSenseSubscriptionEndpoint(), SmartSenseSubscriptionEndpoint.class);
    }

    public FlexSubscriptionEndpoint flexSubscriptionEndpoint() {
        return createProxy(super.flexSubscriptionEndpoint(), FlexSubscriptionEndpoint.class);
    }

    public ImageCatalogV1Endpoint imageCatalogEndpoint() {
        return createProxy(super.imageCatalogEndpoint(), ImageCatalogV1Endpoint.class);
    }

    public ConstraintTemplateEndpoint constraintTemplateEndpoint() {
        return createProxy(super.constraintTemplateEndpoint(), ConstraintTemplateEndpoint.class);
    }

    public UtilEndpoint utilEndpoint() {
        return createProxy(super.utilEndpoint(), UtilEndpoint.class);
    }

    public RepositoryConfigValidationEndpoint repositoryConfigValidationEndpoint() {
        return createProxy(super.repositoryConfigValidationEndpoint(), RepositoryConfigValidationEndpoint.class);
    }

    @SuppressWarnings("unchecked")
    private <I> I createProxy(I obj, Class<I> clazz) {
        return new ProxyInstanceCreator(new ProxyHandler<>(obj, new BeforeAfterMessagingProxyExecutor())).createProxy(clazz);
    }

}
