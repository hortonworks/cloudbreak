package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AppBased;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.StackAction;
import com.sequenceiq.it.cloudbreak.newway.StackCreation;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
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

    private static final String CREDENTIAL_ACCESS_KEY_ENV_KEY = "integrationtest.azurermcredential.accessKey";

    private static final String CREDENTIAL_SECRET_KEY_ENV_KEY = "integrationtest.azurermcredential.secretKey";

    private static final String CREDENTIAL_NEWACCESS_KEY_ENV_KEY = "integrationtest.azurermcredential.newAccessKey";

    private static final String CREDENTIAL_NEWSECRET_KEY_ENV_KEY = "integrationtest.azurermcredential.newSecretKey";

    private static final String CREDENTIAL_TENANT_ID_ENV_KEY = "integrationtest.azurermcredential.tenantId";

    private static final String CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY = "integrationtest.azurermcredential.subscriptionId";

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
    public CredentialTestDto aValidCredential(boolean create) {
        CredentialTestDto credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(AZURE_CAPITAL)
                .withAzureParameters(azureCredentialDetails());
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
    public StackAuthenticationV4Request stackauth() {
        StackAuthenticationV4Request stackauth = new StackAuthenticationV4Request();

        stackauth.setPublicKey(getTestParameter().get(INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    public InstanceTemplateV4Request template() {
        InstanceTemplateV4Request t = new InstanceTemplateV4Request();
        t.setInstanceType(getTestParameter().getWithDefault("azureInstanceType", "Standard_D3_v2"));

        VolumeV4Request volume = new VolumeV4Request();

        volume.setCount(Integer.parseInt(getTestParameter().getWithDefault("azureInstanceVolumeCount", "1")));
        volume.setSize(Integer.parseInt(getTestParameter().getWithDefault("azureInstanceVolumeSize", "100")));
        volume.setType(getTestParameter().getWithDefault("azureInstanceVolumeType", "Standard_LRS"));

        t.setAttachedVolumes(Set.of(volume));

        AzureInstanceTemplateV4Parameters params = new AzureInstanceTemplateV4Parameters();
        params.setEncrypted(false);
        params.setManagedDisk(true);

        t.setAzure(params);
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
    public NetworkV4Request newNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());
        return network;
    }

    @Override
    public NetworkV4Request existingNetwork() {
        return null;
    }

    @Override
    public NetworkV4Request existingSubnet() {
        NetworkV4Request network = new NetworkV4Request();
        AzureNetworkV4Parameters params = new AzureNetworkV4Parameters();
        params.setSubnetId(getSubnetId());
        params.setNetworkId(getVpcId());
        params.setResourceGroupName(getResourceGroupName());
        params.setNoFirewallRules(getNoFirewallRules());
        params.setNoPublicIp(getNoPublicIp());
        network.setAzure(params);
        return network;
    }

    @Override
    public AmbariV4Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName) {
        var ambari = ambariRequestWithBlueprintName(blueprintName);
        var stackDetails = new StackRepositoryV4Request();
        stackDetails.setMpacks(Collections.emptyList());
        ambari.setConfigStrategy(null);
        ambari.setStackRepository(stackDetails);
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
    public Cluster aValidAttachedCluster() {
        return Cluster.request()
                .withAmbariRequest(ambariRequestWithBlueprintName(getBlueprintName()))
                .withCloudStorage(resourceHelper.getCloudStorageRequestForAttachedCluster())
                .withRdsConfigNames(new HashSet<>(Arrays.asList(
                        getTestParameter().get(Ranger.CONFIG_NAME),
                        getTestParameter().get(Hive.CONFIG_NAME))))
                .withLdapConfigName(resourceHelper.getLdapConfigName());
    }

    public Cluster aValidClusterWithFs() {
        AmbariV4Request ambariV2Request = ambariRequestWithBlueprintName(getDatalakeBlueprintName());
        StackRepositoryV4Request stackRepo = ambariV2Request.getStackRepository();
        stackRepo.setVersion("3.0");
        stackRepo.setOs("centos7");
        stackRepo.setStack("HDP");
        stackRepo.setEnableGplRepo(false);
        stackRepo.setVerify(false);

        return Cluster.request()
                .withAmbariRequest(ambariV2Request)
                .withCloudStorage(resourceHelper.getCloudStorageRequestForDatalake());
    }

    public AzureCredentialV4Parameters azureCredentialDetails() {
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setSecretKey(getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY));
        appBased.setAccessKey(getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY));
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY));
        credentialParameters.setTenantId(getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
        return credentialParameters;
    }

    public AzureCredentialV4Parameters azureCredentialDetailsNewApplication() {
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setSecretKey(getTestParameter().get(CREDENTIAL_NEWSECRET_KEY_ENV_KEY));
        appBased.setAccessKey(getTestParameter().get(CREDENTIAL_NEWACCESS_KEY_ENV_KEY));
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY));
        credentialParameters.setTenantId(getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
        return credentialParameters;
    }

    public AzureCredentialV4Parameters azureCredentialDetailsInvalidAccessKey() {
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setSecretKey(getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY));
        appBased.setAccessKey(getTestParameter().get(GENERIC_TEST_VALUE));
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY));
        credentialParameters.setTenantId(getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
        return credentialParameters;
    }

    public AzureCredentialV4Parameters azureCredentialDetailsInvalidSecretKey() {
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setSecretKey(getTestParameter().get(GENERIC_TEST_VALUE));
        appBased.setAccessKey(getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY));
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY));
        credentialParameters.setTenantId(getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
        return credentialParameters;
    }

    public AzureCredentialV4Parameters azureCredentialDetailsInvalidSubscriptionID() {
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setSecretKey(getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY));
        appBased.setAccessKey(getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY));
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(GENERIC_TEST_VALUE);
        credentialParameters.setTenantId(getTestParameter().get(CREDENTIAL_TENANT_ID_ENV_KEY));
        return credentialParameters;
    }

    public AzureCredentialV4Parameters azureCredentialDetailsInvalidTenantID() {
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setSecretKey(getTestParameter().get(CREDENTIAL_SECRET_KEY_ENV_KEY));
        appBased.setAccessKey(getTestParameter().get(CREDENTIAL_ACCESS_KEY_ENV_KEY));
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(getTestParameter().get(CREDENTIAL_SUBSCRIPTION_ID_ENV_KEY));
        credentialParameters.setTenantId(GENERIC_TEST_VALUE);
        return credentialParameters;
    }

    @Override
    public StackTestDto aValidAttachedStackRequest(String datalakeClusterName) {
        var request = new StackCreation(aValidStackRequest());
        request.setCreationStrategy(StackAction::determineNetworkAzureFromDatalakeStack);
        request.withSharedService(datalakeClusterName);
        return request.getStack();
    }
}