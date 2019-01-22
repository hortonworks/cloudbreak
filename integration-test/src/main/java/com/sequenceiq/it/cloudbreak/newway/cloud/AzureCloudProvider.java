package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.StackAction;
import com.sequenceiq.it.cloudbreak.newway.StackCreation;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Azure.Database.Ranger;

public class AzureCloudProvider extends CloudProviderHelper {

    public static final String AZURE = "azure";

    public static final String AZURE_CAPITAL = "AZURE";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-azure-cred";

    private static final String BLUEPRINT_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String AZURE_CLUSTER_DEFAULT_NAME = "autotesting-azure-cluster";

    private static final String NETWORK_DEFAULT_NAME = "autotesting-azure-net";

    private static final String VPC_DEFAULT_ID = "aszegedi";

    private static final String SUBNET_DEFAULT_ID = "default";

    private static final String RESOURCE_GROUP_DEFAULT_NAME = "aszegedi";

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting azure network";

    private static final String CREDENTIAL_ACCESS_KEY_ENV_KEY = "integrationtest.azurermcredential.accessKey";

    private static final String CREDENTIAL_SECRET_KEY_ENV_KEY = "integrationtest.azurermcredential.secretKey";

    private static final String CREDENTIAL_NEWACCESS_KEY_ENV_KEY = "integrationtest.azurermcredential.newAccessKey";

    private static final String CREDENTIAL_NEWSECRET_KEY_ENV_KEY = "integrationtest.azurermcredential.newSecretKey";

    private static final String CREDENTIAL_TENANT_ID_ENV_KEY = "integrationtest.azurermcredential.tenantId";

    private static final String CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY = "integrationtest.azurermcredential.subscriptionId";

    private static final String ACCESS_KEY_PARAM_KEY = "accessKey";

    private static final String SECRET_KEY_PARAM_KEY = "secretKey";

    private static final String SUBSCRIPTION_ID_PARAM_KEY = "subscriptionId";

    private static final String TENANT_ID_PARAM_KEY = "tenantId";

    private static final String GENERIC_TEST_VALUE = "12345abcdefg789";

    private final ResourceHelper<?> resourceHelper;

    public AzureCloudProvider(TestParameter testParameter) {
        super(testParameter);
        String storageType = testParameter.get("cloudStorageType");
        if (storageType != null) {
            switch (storageType.toUpperCase()) {
                case "WASB":
                    resourceHelper = new AzureWasbResourceHelper(testParameter, "-azure-wasb");
                    break;
                case "ADLS_GEN_2":
                    resourceHelper = new AzureAdlsGen2ResourceHelper(testParameter, "-azure-adls-gen2");
                    break;
                default:
                    resourceHelper = new AzureAdlsResourceHelper(testParameter, "-azure-adls");
                    break;
            }
        } else {
            resourceHelper = new AzureWasbResourceHelper(testParameter, "-azure-wasb");
        }
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        CredentialEntity credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(AZURE_CAPITAL)
                .withParameters(azureCredentialDetails());
    }

    @Override
    public String availabilityZone() {
        return null;
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault("azureRegion", "North Europe");
    }

    @Override
    public StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();

        stackauth.setPublicKey(getTestParameter().get(INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    public TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();

        t.setInstanceType(getTestParameter().getWithDefault("azureInstanceType", "Standard_D3_v2"));
        t.setVolumeCount(Integer.parseInt(getTestParameter().getWithDefault("azureInstanceVolumeCount", "1")));
        t.setVolumeSize(Integer.parseInt(getTestParameter().getWithDefault("azureInstanceVolumeSize", "100")));
        t.setVolumeType(getTestParameter().getWithDefault("azureInstanceVolumeType", "Standard_LRS"));

        Map<String, Object> params = Map.of("encrypted", "false", "managedDisk", "true");
        t.setParameters(params);
        return t;
    }

    @Override
    public String getClusterName() {
        return getTestParameter().getWithDefault("azureClusterName", AZURE_CLUSTER_DEFAULT_NAME);
    }

    @Override
    public String getPlatform() {
        return AZURE_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        return getTestParameter().getWithDefault("azureCredentialName", CREDENTIAL_DEFAULT_NAME);
    }

    @Override
    public String getBlueprintName() {
        return getTestParameter().getWithDefault("azureBlueprintName", BLUEPRINT_DEFAULT_NAME);
    }

    @Override
    public String getNetworkName() {
        return getTestParameter().getWithDefault("azureNetworkName", NETWORK_DEFAULT_NAME);
    }

    @Override
    public String getSubnetCIDR() {
        return getTestParameter().getWithDefault("azureSubnetCIDR", DEFAULT_SUBNET_CIDR);
    }

    @Override
    public String getVpcId() {
        return getTestParameter().getWithDefault("azureVcpId", VPC_DEFAULT_ID);
    }

    @Override
    public String getSubnetId() {
        return getTestParameter().getWithDefault("azureSubnetId", SUBNET_DEFAULT_ID);
    }

    public String getNewApplicationID() {
        return getTestParameter().get(CREDENTIAL_NEWACCESS_KEY_ENV_KEY);
    }

    public String getResourceGroupName() {
        return getTestParameter().getWithDefault("resourceGroupName", RESOURCE_GROUP_DEFAULT_NAME);
    }

    public boolean getNoFirewallRules() {
        Boolean firewallRules = Boolean.valueOf(getTestParameter().get("azureNoFirewallRules"));
        return firewallRules == null ? false : firewallRules;
    }

    public boolean getNoPublicIp() {
        Boolean publicIp = Boolean.valueOf(getTestParameter().get("azureNoPublicIp"));
        return publicIp == null ? false : publicIp;
    }

    @Override
    public Map<String, Object> newNetworkProperties() {
        return null;
    }

    @Override
    public Map<String, Object> networkProperties() {
        return null;
    }

    @Override
    public Map<String, Object> subnetProperties() {
        return Map.of("subnetId", getSubnetId(), "networkId", getVpcId(), "resourceGroupName", getResourceGroupName(), "noFirewallRules",
                getNoFirewallRules(), "noPublicIp", getNoPublicIp());
    }

    @Override
    public NetworkV2Request newNetwork() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR(getSubnetCIDR());
        return network;
    }

    @Override
    public NetworkV2Request existingNetwork() {
        return null;
    }

    @Override
    public NetworkV2Request existingSubnet() {
        NetworkV2Request network = new NetworkV2Request();
        network.setParameters(subnetProperties());
        return network;
    }

    @Override
    public AmbariV2Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName) {
        var ambari = ambariRequestWithBlueprintName(blueprintName);
        var stackDetails = new AmbariStackDetailsJson();
        stackDetails.setMpacks(Collections.emptyList());
        ambari.setConfigStrategy(null);
        ambari.setAmbariStackDetails(stackDetails);
        return ambari;
    }

    @Override
    public ResourceHelper<?> getResourceHelper() {
        return resourceHelper;
    }

    @Override
    public Cluster aValidDatalakeCluster() {
        return Cluster.request()
                .withAmbariRequest(ambariRequestWithBlueprintName(getDatalakeBlueprintName()))
                .withCloudStorage(resourceHelper.getCloudStorageRequestForDatalake())
                .withRdsConfigNames(Set.of(
                        getTestParameter().get(Ranger.CONFIG_NAME),
                        getTestParameter().get(Hive.CONFIG_NAME)))
                .withLdapConfigName(resourceHelper.getLdapConfigName());
    }

    @Override
    public Cluster aValidAttachedCluster(String datalakeClusterName) {
        return Cluster.request()
                .withSharedService(datalakeClusterName)
                .withAmbariRequest(ambariRequestWithBlueprintName(getBlueprintName()))
                .withCloudStorage(resourceHelper.getCloudStorageRequestForAttachedCluster())
                .withRdsConfigNames(new HashSet<>(Arrays.asList(
                        getTestParameter().get(Ranger.CONFIG_NAME),
                        getTestParameter().get(Hive.CONFIG_NAME))))
                .withLdapConfigName(resourceHelper.getLdapConfigName());
    }

    public Cluster aValidClusterWithFs() {
        AmbariV2Request ambariV2Request = ambariRequestWithBlueprintName(getDatalakeBlueprintName());
        AmbariStackDetailsJson ambariStackDetails = ambariV2Request.getAmbariStackDetails();
        ambariStackDetails.setVersion("3.0");
        ambariStackDetails.setOs("centos7");
        ambariStackDetails.setStack("HDP");
        ambariStackDetails.setEnableGplRepo(false);
        ambariStackDetails.setVerify(false);

        return Cluster.request()
                .withAmbariRequest(ambariV2Request)
                .withCloudStorage(resourceHelper.getCloudStorageRequestForDatalake());
    }

    public Map<String, Object> azureCredentialDetails() {
        return Map.of(ACCESS_KEY_PARAM_KEY, getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY), SECRET_KEY_PARAM_KEY,
                getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY), SUBSCRIPTION_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY),
                TENANT_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
    }

    public Map<String, Object> azureCredentialDetailsNewApplication() {
        return Map.of(ACCESS_KEY_PARAM_KEY, getTestParameter().get(CREDENTIAL_NEWACCESS_KEY_ENV_KEY), SECRET_KEY_PARAM_KEY,
                getTestParameter().get(CREDENTIAL_NEWSECRET_KEY_ENV_KEY), SUBSCRIPTION_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY),
                TENANT_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
    }

    public Map<String, Object> azureCredentialDetailsInvalidAccessKey() {
        return Map.of(ACCESS_KEY_PARAM_KEY, GENERIC_TEST_VALUE, SECRET_KEY_PARAM_KEY,
                getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY), SUBSCRIPTION_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY),
                TENANT_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
    }

    public Map<String, Object> azureCredentialDetailsInvalidSecretKey() {
        return Map.of(ACCESS_KEY_PARAM_KEY, getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY), SECRET_KEY_PARAM_KEY,
                GENERIC_TEST_VALUE, SUBSCRIPTION_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY),
                TENANT_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
    }

    public Map<String, Object> azureCredentialDetailsInvalidSubscriptionID() {
        return Map.of(ACCESS_KEY_PARAM_KEY, getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY), SECRET_KEY_PARAM_KEY,
                getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY), SUBSCRIPTION_ID_PARAM_KEY, GENERIC_TEST_VALUE,
                TENANT_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
    }

    public Map<String, Object> azureCredentialDetailsInvalidTenantID() {
        return Map.of(ACCESS_KEY_PARAM_KEY, getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY), SECRET_KEY_PARAM_KEY,
                getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY), SUBSCRIPTION_ID_PARAM_KEY, getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY),
                TENANT_ID_PARAM_KEY, GENERIC_TEST_VALUE);
    }

    @Override
    public StackEntity aValidAttachedStackRequest() {
        var request = new StackCreation(aValidStackRequest());
        request.setCreationStrategy(StackAction::determineNetworkAzureFromDatalakeStack);
        return request.getStack();
    }
}