package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting gcp network";

    private final ResourceHelper resourceHelper;

    public GcpCloudProvider(TestParameter testParameter) {
        super(testParameter);
        resourceHelper = new GcpResourceHelper(testParameter, "-gcp");
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
        return getTestParameter().getWithDefault("gcpAvailabilityZone", "europe-west1-b");

    }

    @Override
    public String region() {
        return getTestParameter().getWithDefault("gcpRegion", "europe-west1");
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

        t.setInstanceType(getTestParameter().getWithDefault("gcpInstanceType", "n1-standard-8"));
        t.setVolumeCount(Integer.parseInt(getTestParameter().getWithDefault("gcpInstanceVolumeCount", "1")));
        t.setVolumeSize(Integer.parseInt(getTestParameter().getWithDefault("gcpInstanceVolumeSize", "100")));
        t.setVolumeType(getTestParameter().getWithDefault("gcpInstanceVolumeType", "pd-standard"));
        t.setRootVolumeSize(Integer.parseInt(getTestParameter().getWithDefault("ROOT_VOLUME_SIZE", "100")));

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

    public Map<String, Object> gcpCredentialDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "credential-p12");
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyP12File() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "credential-p12");
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", "");

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyProjectId() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "credential-p12");
        map.put("projectId", "");
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyServiceAccount() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "credential-p12");
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", "");
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }
}
