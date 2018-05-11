package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class OpenstackCloudProvider extends CloudProviderHelper {

    public static final String OPENSTACK = "openstack";

    public static final String OPENSTACK_CAPITAL = "OPENSTACK";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-os-cred";

    private static final String OPENSTACK_CLUSTER_DEFAULT_NAME = "autotesting-os-cluster";

    private static final String BLUEPRINT_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String NETWORK_DEFAULT_NAME = "autotesting-os-net";

    private static final String VPC_DEFAULT_ID = "f955d535-8a7a-456f-a90a-430d45f1c92b";

    private static final String SUBNET_DEFAULT_ID = "7a2c4679-1312-4cf6-91a5-76a2c1e3faa8";

    private static final String PUBLIC_NETWORK_ID = "999e09bc-cf75-4a19-98fb-c0b4ddee6d93";

    private static final String ROUTER_ID = "aa402f0a-8652-4799-904d-e73c95c1a711";

    private static final String INTERNET_GATEWAY_ID = null;

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting os network";

    private static final String NETWORKING_DEFAULT_OPTION = "self-service";

    public OpenstackCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        CredentialEntity credential = create ? Credential.isCreated() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(OPENSTACK_CAPITAL)
                .withParameters(openstackCredentialDetails());
    }

    @Override
    public String availabilityZone() {
        String availabilityZone = "nova";
        String availabilityZoneParam = getTestParameter().get("openstackAvailibilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public String region() {
        String region = "RegionOne";
        String regionParam = getTestParameter().get("openstackRegion");

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
        String instanceTypeDefaultValue = "m1.xlarge";
        String instanceTypeParam = getTestParameter().get("openstackInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("openstackInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 50;
        String volumeSizeParam = getTestParameter().get("openstackInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "HDD";
        String volumeTypeParam = getTestParameter().get("openstackInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        return t;
    }

    @Override
    public String getClusterName() {
        String clustername = getTestParameter().get("openstackClusterName");
        clustername = clustername == null ? OPENSTACK_CLUSTER_DEFAULT_NAME : clustername;
        return clustername + getClusterNamePostfix();
    }

    @Override
    public String getPlatform() {
        return OPENSTACK_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        String credentialName = getTestParameter().get("openstackCredentialName");
        return credentialName == null ? CREDENTIAL_DEFAULT_NAME : credentialName;
    }

    @Override
    public String getBlueprintName() {
        String blueprintName = getTestParameter().get("openstackBlueprintName");
        return blueprintName == null ? BLUEPRINT_DEFAULT_NAME : blueprintName;
    }

    @Override
    public String getNetworkName() {
        String networkName = getTestParameter().get("openstackNetworkName");
        return networkName == null ? NETWORK_DEFAULT_NAME : networkName;
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = getTestParameter().get("openstackSubnetCIDR");
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getVpcId() {
        String vpcId = getTestParameter().get("openstackVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    @Override
    public String getSubnetId() {
        String subnetId = getTestParameter().get("openstackSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public String getNetworkingOption() {
        String networkingOption = getTestParameter().get("networkingOption");
        return networkingOption == null ? NETWORKING_DEFAULT_OPTION : networkingOption;
    }

    public String getPublicNetId() {
        String publicNetId = getTestParameter().get("publicNetId");
        return publicNetId == null ? PUBLIC_NETWORK_ID : publicNetId;
    }

    public String getRouterId() {
        String routerId = getTestParameter().get("routerId");
        return routerId == null ? ROUTER_ID : routerId;
    }

    public String getInternetGatewayId() {
        String gatewayId = getTestParameter().get("openstackInternetGatewayId");
        return gatewayId == null ? INTERNET_GATEWAY_ID : gatewayId;
    }

    @Override
    public Map<String, Object> newNetworkProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("publicNetId", getPublicNetId());

        return map;
    }

    @Override
    public Map<String, Object> networkProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("publicNetId", getPublicNetId());
        map.put("networkId", getVpcId());
        map.put("routerId", getRouterId());

        return map;
    }

    @Override
    public Map<String, Object> subnetProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("networkingOption", getNetworkingOption());
        map.put("publicNetId", getPublicNetId());
        map.put("subnetId", getSubnetId());
        map.put("networkId", getVpcId());
        map.put("routerId", getRouterId());
        map.put("internetGatewayId", getInternetGatewayId());

        return map;
    }

    @Override
    public NetworkV2Request newNetwork() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR(getSubnetCIDR());
        network.setParameters(newNetworkProperties());
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

    public Map<String, Object> openstackCredentialDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"));
        map.put("userName", getTestParameter().get("integrationtest.openstackcredential.userName"));
        map.put("password", getTestParameter().get("integrationtest.openstackcredential.password"));
        map.put("endpoint", getTestParameter().get("integrationtest.openstackcredential.endpoint"));
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");

        return map;
    }

    public Map<String, Object> openstackV3CredentialDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantName", getTestParameter().get("integrationtest.openstackV3credential.tenantName"));
        map.put("userDomain", getTestParameter().get("integrationtest.openstackV3credential.userDomain"));
        map.put("userName", getTestParameter().get("integrationtest.openstackV3credential.userName"));
        map.put("password", getTestParameter().get("integrationtest.openstackV3credential.password"));
        map.put("endpoint", getTestParameter().get("integrationtest.openstackV3credential.endpoint"));
        map.put("projectDomainName", getTestParameter().get("integrationtest.openstackV3credential.projectDomainName"));
        map.put("projectName", getTestParameter().get("integrationtest.openstackV3credential.projectName"));
        map.put("keystoneAuthScope", getTestParameter().get("integrationtest.openstackV3credential.keystoneAuthScope"));
        map.put("keystoneVersion", "cb-keystone-v3");
        map.put("apiFacing", getTestParameter().get("integrationtest.openstackV3credential.apiFacing"));
        map.put("selector", "cb-keystone-v3-project-scope");

        return map;
    }

    public Map<String, Object> openstackCredentialDetailsInvalidUser() {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"));
        map.put("userName", "kisnyul");
        map.put("password", getTestParameter().get("integrationtest.openstackcredential.password"));
        map.put("endpoint", getTestParameter().get("integrationtest.openstackcredential.endpoint"));
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");

        return map;
    }

    public Map<String, Object> openstackCredentialDetailsInvalidEndpoint() {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"));
        map.put("userName", getTestParameter().get("integrationtest.openstackcredential.userName"));
        map.put("password", getTestParameter().get("integrationtest.openstackcredential.password"));
        map.put("endpoint", "https://index.hu/");
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");

        return map;
    }
}