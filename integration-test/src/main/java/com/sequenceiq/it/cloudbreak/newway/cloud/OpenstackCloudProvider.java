package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
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

    private static final String CREDENTIAL_ENGENDPOINT_URL = "integrationtest.openstackEngcredential.endpoint";

    private final ResourceHelper<?> resourceHelper;

    public OpenstackCloudProvider(TestParameter testParameter) {
        super(testParameter);
        resourceHelper = null;
    }

    public String engOpenStackEndpoint() {
        return getTestParameter().get(CREDENTIAL_ENGENDPOINT_URL);
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        CredentialEntity credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(OPENSTACK_CAPITAL)
                .withParameters(openstackCredentialDetailsKilo());
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
    public StackAuthenticationRequest stackauth() {
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
        return Map.of("publicNetId", getPublicNetId());
    }

    @Override
    public Map<String, Object> networkProperties() {
        return Map.of("publicNetId", getPublicNetId(), "networkId", getVpcId(), "routerId",
                getRouterId());
    }

    @Override
    public Map<String, Object> subnetProperties() {
        return Map.of("networkingOption", getNetworkingOption(), "publicNetId", getPublicNetId(), "subnetId", getSubnetId(),
                "networkId", getVpcId(), "routerId", getRouterId(), "internetGatewayId", getInternetGatewayId());
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

    public Map<String, Object> openstackCredentialDetailsKilo() {
        return Map.of("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"), "userName",
                getTestParameter().get("integrationtest.openstackcredential.userName"), "password",
                getTestParameter().get("integrationtest.openstackcredential.password"), "endpoint",
                getTestParameter().get("integrationtest.openstackcredential.endpoint"), "keystoneVersion", "cb-keystone-v2",
                "selector", "cb-keystone-v2");
    }

    public Map<String, Object> openstackCredentialDetailsKiloAdmin() {
        return Map.of("tenantName", getTestParameter().get("integrationtest.openstackAdmincredential.tenantName"), "userName",
                getTestParameter().get("integrationtest.openstackAdmincredential.userName"), "password",
                getTestParameter().get("integrationtest.openstackAdmincredential.password"), "endpoint",
                getTestParameter().get("integrationtest.openstackAdmincredential.endpoint"), "keystoneVersion", "cb-keystone-v2",
                "selector", "cb-keystone-v2");
    }

    public Map<String, Object> openstackCredentialDetailsEngineering() {
        return Map.of("tenantName", getTestParameter().get("integrationtest.openstackEngcredential.tenantName"), "userName",
                getTestParameter().get("integrationtest.openstackEngcredential.userName"), "password",
                getTestParameter().get("integrationtest.openstackEngcredential.password"), "endpoint",
                getTestParameter().get("integrationtest.openstackEngcredential.endpoint"), "keystoneVersion", "cb-keystone-v2",
                "selector", "cb-keystone-v2");
    }

    public Map<String, Object> openstackCredentialDetailsField() {
        Map<String, Object> map = Map.ofEntries(
                Map.entry("tenantName", getTestParameter().get("integrationtest.openstackFieldcredential.tenantName")),
                Map.entry("userDomain", getTestParameter().get("integrationtest.openstackFieldcredential.userDomain")),
                Map.entry("userName", getTestParameter().get("integrationtest.openstackFieldcredential.userName")),
                Map.entry("password", getTestParameter().get("integrationtest.openstackFieldcredential.password")),
                Map.entry("endpoint", getTestParameter().get("integrationtest.openstackFieldcredential.endpoint")),
                Map.entry("projectDomainName", getTestParameter().get("integrationtest.openstackFieldcredential.projectDomainName")),
                Map.entry("projectName", getTestParameter().get("integrationtest.openstackFieldcredential.projectName")),
                Map.entry("keystoneAuthScope", "cb-keystone-v3-project-scope"),
                Map.entry("keystoneVersion", "cb-keystone-v3"),
                Map.entry("apiFacing", "internal"),
                Map.entry("selector", "cb-keystone-v3-project-scope"));

        return map;
    }

    public Map<String, Object> openstackCredentialDetailsInvalidUser() {
        return Map.of("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"), "userName", "kisnyul",
                "password", getTestParameter().get("integrationtest.openstackcredential.password"), "endpoint",
                getTestParameter().get("integrationtest.openstackcredential.endpoint"), "keystoneVersion", "cb-keystone-v2",
                "selector", "cb-keystone-v2");
    }

    public Map<String, Object> openstackCredentialDetailsInvalidEndpoint() {
        return Map.of("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"), "userName",
                getTestParameter().get("integrationtest.openstackcredential.userName"), "password",
                getTestParameter().get("integrationtest.openstackcredential.password"), "endpoint",
                "https://index.hu/", "keystoneVersion", "cb-keystone-v2", "selector", "cb-keystone-v2");
    }
}