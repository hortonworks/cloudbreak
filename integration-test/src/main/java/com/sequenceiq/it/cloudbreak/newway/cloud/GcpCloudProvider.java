package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class GcpCloudProvider extends CloudProviderHelper {
    public static final String GCP = "gcp";

    public static final String GCP_CAPITAL = "GCP";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-gcp-cred";

    private static final String BLUEPRINT_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String GCP_CLUSTER_DEFAULT_NAME = "autotesting-gcp-cluster";

    private static final String NETWORK_DEFAULT_NAME = "autotesting-gcp-net";

    private static final String VPC_DEFAULT_ID = "";

    private static final String SUBNET_DEFAULT_ID = "";

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting gcp network";

    public GcpCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        CredentialEntity credential = create ? Credential.isCreated() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(GCP_CAPITAL)
                .withParameters(gcpCredentialDetails());
    }

    @Override
    public String availabilityZone() {
        String availabilityZone = "europe-west1-b";
        String availabilityZoneParam = getTestParameter().get("gcpAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public String region() {
        String region = "europe-west1";
        String regionParam = getTestParameter().get("gcpRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();

        stackauth.setPublicKey(getTestParameter().get(CloudProviderHelper.INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();
        String instanceTypeDefaultValue = "n1-standard-4";
        String instanceTypeParam = getTestParameter().get("gcpInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("gcpInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("gcpInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "pd-standard";
        String volumeTypeParam = getTestParameter().get("gcpInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        return t;
    }

    @Override
    public String getClusterName() {
        String clustername = getTestParameter().get("gcpClusterName");
        clustername = clustername == null ? GCP_CLUSTER_DEFAULT_NAME : clustername;
        return clustername + getClusterNamePostfix();
    }

    @Override
    public String getPlatform() {
        return GCP_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        String credentialName = getTestParameter().get("gcpCredentialName");
        return credentialName == null ? CREDENTIAL_DEFAULT_NAME : credentialName;
    }

    @Override
    public String getBlueprintName() {
        String blueprintName = getTestParameter().get("gcpBlueprintName");
        return blueprintName == null ? BLUEPRINT_DEFAULT_NAME : blueprintName;
    }

    @Override
    public String getNetworkName() {
        String networkName = getTestParameter().get("gcpNetworkName");
        return networkName == null ? NETWORK_DEFAULT_NAME : networkName;
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = getTestParameter().get("gcpSubnetCIDR");
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getVpcId() {
        String vpcId = getTestParameter().get("gcpVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    @Override
    public String getSubnetId() {
        String subnetId = getTestParameter().get("gcpSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
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
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", getVpcId());

        return map;
    }

    @Override
    public Map<String, Object> subnetProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("subnetId", getSubnetId());
        map.put("networkId", getVpcId());
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
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR(getSubnetCIDR());
        network.setParameters(networkProperties());
        return network;
    }

    @Override
    public NetworkV2Request existingSubnet() {
        NetworkV2Request network = new NetworkV2Request();
        network.setParameters(subnetProperties());
        return network;
    }

    public Map<String, Object> gcpCredentialDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyP12File() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", "");

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyProjectId() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", "");
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyServiceAccount() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", "");
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }
}