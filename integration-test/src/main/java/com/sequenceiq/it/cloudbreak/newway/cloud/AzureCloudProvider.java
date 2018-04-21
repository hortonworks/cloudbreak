package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

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

    public AzureCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        CredentialEntity credential = create ? Credential.isCreated() : Credential.request();
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
        String region = "North Europe";
        String regionParam = getTestParameter().get("azureRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();

        stackauth.setPublicKey(getTestParameter().get(INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();
        String instanceTypeDefaultValue = "Standard_D3_v2";
        String instanceTypeParam = getTestParameter().get("azureInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("azureInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("azureInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "Standard_LRS";
        String volumeTypeParam = getTestParameter().get("azureInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        Map<String, Object> params = new HashMap<>();
        params.put("encrypted", "false");
        params.put("managedDisk", "true");
        t.setParameters(params);
        return t;
    }

    @Override
    public String getClusterName() {
        String clustername = getTestParameter().get("azureClusterName");
        clustername = clustername == null ? AZURE_CLUSTER_DEFAULT_NAME : clustername;
        return clustername + getClusterNamePostfix();
    }

    @Override
    public String getPlatform() {
        return AZURE_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        String credentialName = getTestParameter().get("azureCredentialName");
        return credentialName == null ? CREDENTIAL_DEFAULT_NAME : credentialName;
    }

    @Override
    public String getBlueprintName() {
        String blueprintName = getTestParameter().get("azureBlueprintName");
        return blueprintName == null ? BLUEPRINT_DEFAULT_NAME : blueprintName;
    }

    @Override
    public String getNetworkName() {
        String networkName = getTestParameter().get("azureNetworkName");
        return networkName == null ? NETWORK_DEFAULT_NAME : networkName;
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = getTestParameter().get("azureSubnetCIDR");
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getVpcId() {
        String vpcId = getTestParameter().get("azureVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    @Override
    public String getSubnetId() {
        String subnetId = getTestParameter().get("azureSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public String getResourceGroupName() {
        String resourceGroupName = getTestParameter().get("resourceGroupName");
        return resourceGroupName == null ? RESOURCE_GROUP_DEFAULT_NAME : resourceGroupName;
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
        Map<String, Object> map = new HashMap<>();
        map.put("subnetId", getSubnetId());
        map.put("networkId", getVpcId());
        map.put("resourceGroupName", getResourceGroupName());
        map.put("noFirewallRules", getNoFirewallRules());
        map.put("noPublicIp", getNoPublicIp());

        return map;
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

    public Map<String, Object> azureCredentialDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("accessKey", getTestParameter().get("integrationtest.azurermcredential.accessKey"));
        map.put("secretKey", getTestParameter().get("integrationtest.azurermcredential.secretKey"));
        map.put("subscriptionId", getTestParameter().get("integrationtest.azurermcredential.subscriptionId"));
        map.put("tenantId", getTestParameter().get("integrationtest.azurermcredential.tenantId"));

        return map;
    }

    public Map<String, Object> azureCredentialDetailsInvalidAccessKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("accessKey", "12345abcdefg789");
        map.put("secretKey", getTestParameter().get("integrationtest.azurermcredential.secretKey"));
        map.put("subscriptionId", getTestParameter().get("integrationtest.azurermcredential.subscriptionId"));
        map.put("tenantId", getTestParameter().get("integrationtest.azurermcredential.tenantId"));

        return map;
    }

    public Map<String, Object> azureCredentialDetailsInvalidSecretKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("accessKey", getTestParameter().get("integrationtest.azurermcredential.accessKey"));
        map.put("secretKey", "12345abcdefg789");
        map.put("subscriptionId", getTestParameter().get("integrationtest.azurermcredential.subscriptionId"));
        map.put("tenantId", getTestParameter().get("integrationtest.azurermcredential.tenantId"));

        return map;
    }

    public Map<String, Object> azureCredentialDetailsInvalidSubscriptionID() {
        Map<String, Object> map = new HashMap<>();
        map.put("accessKey", getTestParameter().get("integrationtest.azurermcredential.accessKey"));
        map.put("secretKey", getTestParameter().get("integrationtest.azurermcredential.secretKey"));
        map.put("subscriptionId", "12345abcdefg789");
        map.put("tenantId", getTestParameter().get("integrationtest.azurermcredential.tenantId"));

        return map;
    }

    public Map<String, Object> azureCredentialDetailsInvalidTenantID() {
        Map<String, Object> map = new HashMap<>();
        map.put("accessKey", getTestParameter().get("integrationtest.azurermcredential.accessKey"));
        map.put("secretKey", getTestParameter().get("integrationtest.azurermcredential.secretKey"));
        map.put("subscriptionId", getTestParameter().get("integrationtest.azurermcredential.subscriptionId"));
        map.put("tenantId", "12345abcdefg789");

        return map;
    }
}