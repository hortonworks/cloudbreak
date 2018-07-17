package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private final ResourceHelper<?> resourceHelper;

    public OpenstackCloudProvider(TestParameter testParameter) {
        super(testParameter);
        resourceHelper = null;
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
        return getTestParameter().getWithDefault("openstackAvailibilityZone", "nova");
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault("openstackRegion", "RegionOne");
    }

    @Override
    StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();

        stackauth.setPublicKey(getTestParameter().get(CloudProviderHelper.INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    public TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();

        t.setInstanceType(getTestParameter().getWithDefault("openstackInstanceType", "m1.xlarge"));
        t.setVolumeCount(Integer.parseInt(getTestParameter().getWithDefault("openstackInstanceVolumeCount", "0")));
        t.setVolumeSize(Integer.parseInt(getTestParameter().getWithDefault("openstackInstanceVolumeSize", "50")));
        t.setVolumeType(getTestParameter().getWithDefault("openstackInstanceVolumeType", "HDD"));

        return t;
    }

    @Override
    public String getClusterName() {
        return getTestParameter().getWithDefault("openstackClusterName", OPENSTACK_CLUSTER_DEFAULT_NAME);
    }

    @Override
    public String getPlatform() {
        return OPENSTACK_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        return getTestParameter().getWithDefault("openstackCredentialName", CREDENTIAL_DEFAULT_NAME);
    }

    @Override
    public String getBlueprintName() {
        return getTestParameter().getWithDefault("openstackBlueprintName", BLUEPRINT_DEFAULT_NAME);

    }

    @Override
    public String getNetworkName() {
        return getTestParameter().getWithDefault("openstackNetworkName", NETWORK_DEFAULT_NAME);

    }

    @Override
    public String getSubnetCIDR() {
        return getTestParameter().getWithDefault("openstackSubnetCIDR", DEFAULT_SUBNET_CIDR);

    }

    @Override
    public String getVpcId() {
        return getTestParameter().getWithDefault("openstackVcpId", VPC_DEFAULT_ID);

    }

    @Override
    public String getSubnetId() {
        return getTestParameter().getWithDefault("openstackSubnetId", SUBNET_DEFAULT_ID);

    }

    public String getNetworkingOption() {
        return getTestParameter().getWithDefault("networkingOption", NETWORKING_DEFAULT_OPTION);

    }

    public String getPublicNetId() {
        return getTestParameter().getWithDefault("publicNetId", PUBLIC_NETWORK_ID);

    }

    public String getRouterId() {
        return getTestParameter().getWithDefault("routerId", ROUTER_ID);

    }

    public String getInternetGatewayId() {
        return getTestParameter().getWithDefault("openstackInternetGatewayId", INTERNET_GATEWAY_ID);

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
        throw new NotImplementedException("Resource helper for Openstack is not implemented yet");
    }

    @Override
    public Cluster aValidDatalakeCluster() {
        throw new NotImplementedException("not implemented!");
    }

    @Override
    public Cluster aValidAttachedCluster(String datalakeClusterName) {
        throw new NotImplementedException("not implemented!");
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