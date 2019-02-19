package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.JsonParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.P12Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Hive;
import com.sequenceiq.it.cloudbreak.parameters.RequiredInputParameters.Gcp.Database.Ranger;

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

    private static final String CREDENTIAL_NEWSERVICEACCOUNT_ID = "integrationtest.gcpcredential.newServiceAccountId";

    private final ResourceHelper resourceHelper;

    public GcpCloudProvider(TestParameter testParameter) {
        super(testParameter);
        resourceHelper = new GcpResourceHelper(testParameter, "-gcp");
    }

    @Override
    public CredentialTestDto aValidCredential(boolean create) {
        CredentialTestDto credential = create ? Credential.created() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(GCP_CAPITAL)
                .withGcpParameters(gcpCredentialDetails());
    }

    @Override
    public StackTestDto aValidAttachedStackRequest(String datalakeName) {
        throw new NotImplementedException("aValidAttachedStackRequest() method is not implemented yet");
    }

    @Override
    public String availabilityZone() {
        return getTestParameter().getWithDefault("gcpAvailabilityZone", "europe-west1-b");

    }

    public String newServiceAccountID() {
        return getTestParameter().get(CREDENTIAL_NEWSERVICEACCOUNT_ID);
    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault("gcpRegion", "europe-west1");
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
        t.setInstanceType(getTestParameter().getWithDefault("gcpInstanceType", "n1-standard-8"));

        VolumeV4Request volume = new VolumeV4Request();
        volume.setCount(Integer.parseInt(getTestParameter().getWithDefault("gcpInstanceVolumeCount", "1")));
        volume.setSize(Integer.parseInt(getTestParameter().getWithDefault("gcpInstanceVolumeSize", "100")));
        volume.setType(getTestParameter().getWithDefault("gcpInstanceVolumeType", "pd-standard"));
        t.setAttachedVolumes(Set.of(volume));

        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(Integer.parseInt(getTestParameter().getWithDefault("ROOT_VOLUME_SIZE", "100")));
        t.setRootVolume(rootVolume);

        return t;
    }

    @Override
    public String getClusterName() {
        return getTestParameter().getWithDefault("gcpClusterName", GCP_CLUSTER_DEFAULT_NAME);
    }

    @Override
    public String getPlatform() {
        return GCP_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        return getTestParameter().getWithDefault("gcpCredentialName", CREDENTIAL_DEFAULT_NAME);
    }

    @Override
    public String getBlueprintName() {
        return getTestParameter().getWithDefault("gcpBlueprintName", BLUEPRINT_DEFAULT_NAME);

    }

    @Override
    public String getNetworkName() {
        return getTestParameter().getWithDefault("gcpNetworkName", NETWORK_DEFAULT_NAME);

    }

    @Override
    public String getSubnetCIDR() {
        return getTestParameter().getWithDefault("gcpSubnetCIDR", DEFAULT_SUBNET_CIDR);

    }

    @Override
    public String getVpcId() {
        return getTestParameter().getWithDefault("gcpVcpId", VPC_DEFAULT_ID);

    }

    @Override
    public String getSubnetId() {
        return getTestParameter().getWithDefault("gcpSubnetId", SUBNET_DEFAULT_ID);

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
        return Map.of("networkId", getVpcId());
    }

    @Override
    public Map<String, Object> subnetProperties() {
        return Map.of("subnetId", getSubnetId(), "networkId", getVpcId(), "noFirewallRules", getNoFirewallRules(),
                "noPublicIp", getNoPublicIp());
    }

    @Override
    public NetworkV4Request newNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());
        return network;
    }

    @Override
    public NetworkV4Request existingNetwork() {
        NetworkV4Request network = new NetworkV4Request();
        network.setSubnetCIDR(getSubnetCIDR());

        GcpNetworkV4Parameters params = new GcpNetworkV4Parameters();
        params.setNetworkId(getVpcId());
        network.setGcp(params);
        return network;
    }

    @Override
    public NetworkV4Request existingSubnet() {
        NetworkV4Request network = new NetworkV4Request();
        GcpNetworkV4Parameters params = new GcpNetworkV4Parameters();
        params.setNetworkId(getVpcId());
        params.setSubnetId(getSubnetId());
        params.setNoFirewallRules(getNoFirewallRules());
        params.setNoPublicIp(getNoPublicIp());
        network.setGcp(params);
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
    public ResourceHelper getResourceHelper() {
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

    public GcpCredentialV4Parameters gcpCredentialDetails() {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setProjectId(getTestParameter().get("integrationtest.gcpcredential.projectId"));
        p12Parameters.setServiceAccountId(getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        p12Parameters.setServiceAccountPrivateKey(getTestParameter().get("integrationtest.gcpcredential.p12File").substring(CloudProviderHelper.BEGIN_INDEX));
        parameters.setP12(p12Parameters);
        return parameters;
    }

    public GcpCredentialV4Parameters gcpCredentialDetailsJson() {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        JsonParameters jsonParameters = new JsonParameters();
        jsonParameters.setCredentialJson(getTestParameter().get("integrationtest.gcpcredential.jsonFile").substring(CloudProviderHelper.BEGIN_INDEX));
        parameters.setJson(jsonParameters);
        return parameters;
    }

    public GcpCredentialV4Parameters gcpCredentialDetailsNewServiceAccount() {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setProjectId(getTestParameter().get("integrationtest.gcpcredential.projectId"));
        p12Parameters.setServiceAccountId(getTestParameter().get("integrationtest.gcpcredential.newServiceAccountId"));
        p12Parameters.setServiceAccountPrivateKey(getTestParameter().get("integrationtest.gcpcredential.newP12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));
        parameters.setP12(p12Parameters);
        return parameters;
    }

    public GcpCredentialV4Parameters gcpCredentialDetailsEmptyP12File() {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setProjectId(getTestParameter().get("integrationtest.gcpcredential.projectId"));
        p12Parameters.setServiceAccountId(getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        p12Parameters.setServiceAccountPrivateKey("");
        parameters.setP12(p12Parameters);
        return parameters;
    }

    public GcpCredentialV4Parameters gcpCredentialDetailsEmptyProjectId() {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setProjectId("");
        p12Parameters.setServiceAccountId(getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        p12Parameters.setServiceAccountPrivateKey(getTestParameter().get("integrationtest.gcpcredential.p12File").substring(CloudProviderHelper.BEGIN_INDEX));
        parameters.setP12(p12Parameters);
        return parameters;
    }

    public GcpCredentialV4Parameters gcpCredentialDetailsEmptyServiceAccount() {
        GcpCredentialV4Parameters parameters = new GcpCredentialV4Parameters();
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setProjectId(getTestParameter().get("integrationtest.gcpcredential.projectId"));
        p12Parameters.setServiceAccountId("");
        p12Parameters.setServiceAccountPrivateKey(getTestParameter().get("integrationtest.gcpcredential.p12File").substring(CloudProviderHelper.BEGIN_INDEX));
        parameters.setP12(p12Parameters);
        return parameters;
    }
}
