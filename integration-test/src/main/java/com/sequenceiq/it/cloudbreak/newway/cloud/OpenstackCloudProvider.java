package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.OpenStackNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
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
    public CredentialTestDto aValidCredential(boolean create) {
        CredentialTestDto credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(OPENSTACK_CAPITAL)
                .withOpenstackParameters(openstackCredentialDetailsKilo());
    }

    @Override
    public StackTestDto aValidAttachedStackRequest(String datalakeClusterName) {
        throw new NotImplementedException("aValidAttachedStackRequest() method is not implemented yet");
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
    public StackAuthenticationV4Request stackauth() {
        StackAuthenticationV4Request stackauth = new StackAuthenticationV4Request();

        stackauth.setPublicKey(getTestParameter().get(CloudProviderHelper.INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    public InstanceTemplateV4Request template() {
        InstanceTemplateV4Request t = new InstanceTemplateV4Request();
        t.setCloudPlatform(CloudPlatform.OPENSTACK);

        VolumeV4Request vol = new VolumeV4Request();
        vol.setCount(Integer.parseInt(getTestParameter().getWithDefault("openstackInstanceVolumeCount", "0")));
        vol.setSize(Integer.parseInt(getTestParameter().getWithDefault("openstackInstanceVolumeSize", "50")));
        vol.setType(getTestParameter().getWithDefault("openstackInstanceVolumeType", "HDD"));
        t.setAttachedVolumes(Set.of(vol));

        t.setInstanceType(getTestParameter().getWithDefault("openstackInstanceType", "m1.xlarge"));

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
    public NetworkV4Request newNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());
        OpenStackNetworkV4Parameters params = new OpenStackNetworkV4Parameters();
        params.setPublicNetId(getPublicNetId());
        network.setOpenstack(params);
        return network;
    }

    @Override
    public NetworkV4Request existingNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());
        OpenStackNetworkV4Parameters params = new OpenStackNetworkV4Parameters();
        params.setPublicNetId(getPublicNetId());
        params.setNetworkId(getVpcId());
        params.setRouterId(getRouterId());
        network.setOpenstack(params);
        return network;
    }

    @Override
    public NetworkV4Request existingSubnet() {
        NetworkV4Request network = new NetworkV4Request();
        OpenStackNetworkV4Parameters params = new OpenStackNetworkV4Parameters();
        params.setPublicNetId(getPublicNetId());
        params.setNetworkId(getVpcId());
        params.setRouterId(getRouterId());
        params.setNetworkingOption(getNetworkingOption());
        params.setSubnetId(getSubnetId());
        network.setOpenstack(params);
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
        throw new NotImplementedException("Resource helper for Openstack is not implemented yet");
    }

    @Override
    public Cluster aValidDatalakeCluster() {
        throw new NotImplementedException("not implemented!");
    }

    @Override
    public Cluster aValidAttachedCluster() {
        throw new NotImplementedException("not implemented!");
    }

    public OpenstackCredentialV4Parameters openstackCredentialDetailsKilo() {
        OpenstackCredentialV4Parameters credentialParameters = new OpenstackCredentialV4Parameters();
        credentialParameters.setPassword(getTestParameter().get("integrationtest.openstackcredential.password"));
        credentialParameters.setUserName(getTestParameter().get("integrationtest.openstackcredential.userName"));
        credentialParameters.setEndpoint(getTestParameter().get("integrationtest.openstackcredential.endpoint"));
        return credentialParameters;
    }

    public OpenstackCredentialV4Parameters openstackCredentialDetailsEngineering() {
        OpenstackCredentialV4Parameters credentialParameters = new OpenstackCredentialV4Parameters();
        credentialParameters.setUserName(getTestParameter().get("integrationtest.openstackEngcredential.userName"));
        credentialParameters.setEndpoint(getTestParameter().get("integrationtest.openstackEngcredential.endpoint"));
        credentialParameters.setPassword(getTestParameter().get("integrationtest.openstackEngcredential.password"));
        KeystoneV2Parameters keystoneV2Parameters = new KeystoneV2Parameters();
        keystoneV2Parameters.setTenantName(getTestParameter().get("integrationtest.openstackEngcredential.tenantName"));
        credentialParameters.setKeystoneV2(keystoneV2Parameters);
        return credentialParameters;
    }

    public OpenstackCredentialV4Parameters openstackCredentialDetailsField() {
        OpenstackCredentialV4Parameters credentialParameters = new OpenstackCredentialV4Parameters();
        credentialParameters.setPassword(getTestParameter().get("integrationtest.openstackFieldcredential.password"));
        credentialParameters.setUserName(getTestParameter().get("integrationtest.openstackFieldcredential.userName"));
        credentialParameters.setEndpoint(getTestParameter().get("integrationtest.openstackFieldcredential.endpoint"));
        credentialParameters.setFacing("internal");
        return credentialParameters;
    }

    public OpenstackCredentialV4Parameters openstackCredentialDetailsInvalidUser() {
        OpenstackCredentialV4Parameters credentialParameters = new OpenstackCredentialV4Parameters();
        credentialParameters.setUserName("kisnyul");
        credentialParameters.setEndpoint(getTestParameter().get("integrationtest.openstackcredential.endpoint"));
        credentialParameters.setPassword(getTestParameter().get("integrationtest.openstackcredential.password"));
        return credentialParameters;
    }

    public OpenstackCredentialV4Parameters openstackCredentialDetailsInvalidEndpoint() {
        OpenstackCredentialV4Parameters credentialParameters = new OpenstackCredentialV4Parameters();
        credentialParameters.setUserName(getTestParameter().get("integrationtest.openstackcredential.userName"));
        credentialParameters.setEndpoint("https://index.hu/");
        credentialParameters.setPassword(getTestParameter().get("integrationtest.openstackcredential.password"));
        return credentialParameters;
    }
}