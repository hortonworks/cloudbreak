package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.MockNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.storage.S3CloudStorageV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.MockInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.EntityCreationStrategy;
import com.sequenceiq.it.cloudbreak.newway.PostCredentialWithNameFromMockStrategy;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class MockCloudProvider extends CloudProviderHelper {

    public static final String MOCK = "mock";

    public static final String MOCK_CAPITAL = "MOCK";

    public static final String MOCK_CLUSTER_DEFAULT_NAME = "autotesting-mock-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-mock-cred";

    private static final String CREDENTIAL_DEFAULT_DESCRIPTION = "autotesting mock credential";

    private static final String BLUEPRINT_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String NETWORK_DEFAULT_NAME = "autotesting-aws-net";

    private static final String VPC_DEFAULT_ID = "vpc-e623b28d";

    private static final String INTERNET_GATEWAY_ID = "igw-b55b26dd";

    private static final String SUBNET_DEFAULT_ID = "subnet-83901cfe";

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting mock network";

    public MockCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialTestDto aValidCredential(boolean create) {
        CredentialTestDto credential = create ? Credential.created() : Credential.request();
        credential = new EntityCreationStrategy<CredentialTestDto>().setCreationStrategy(credential, new PostCredentialWithNameFromMockStrategy());
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(MOCK_CAPITAL);
    }

    @Override
    public StackTestDto aValidAttachedStackRequest(String datalakeName) {
        throw new NotImplementedException("aValidAttachedStackRequest() method is not implemented yet");
    }

    @Override
    public String availabilityZone() {
        String availabilityZone = "eu-west-1a";
        String availabilityZoneParam = getTestParameter().get("mockAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public AmbariV4Request getAmbariRequestWithNoConfigStrategyAndEmptyMpacks(String blueprintName) {
        return null;
    }

    @Override
    public ResourceHelper<?> getResourceHelper() {
        return null;
    }

    @Override
    public Cluster aValidDatalakeCluster() {
        return null;
    }

    @Override
    public Cluster aValidAttachedCluster() {
        return null;
    }

    public Stack aValidDatalakeStackIsCreated() {
        throw new NotImplementedException("Unimplemented operation!");
    }

    public CloudStorageV4Request fileSystemForDatalake() {
        throw new NotImplementedException("Unimplemented operation!");
    }

    @Override
    public String region() {
        String region = "eu-west-1";
        String regionParam = getTestParameter().get("mockRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    public StackAuthenticationV4Request stackauth() {
        StackAuthenticationV4Request stackauth = new StackAuthenticationV4Request();

        stackauth.setPublicKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLob"
                + "pTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KV"
                + "rQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpjT7B"
                + "fcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+"
                + "xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh");
        return stackauth;
    }

    @Override
    public InstanceTemplateV4Request template() {
        InstanceTemplateV4Request t = new InstanceTemplateV4Request();
        String instanceTypeDefaultValue = "large";
        String instanceTypeParam = getTestParameter().get("mockInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        t.setMock(new MockInstanceTemplateV4Parameters());

        VolumeV4Request volume = new VolumeV4Request();

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("mockInstanceVolumeCount");
        volume.setCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("mockInstanceVolumeSize");
        volume.setSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "magnetic";
        String volumeTypeParam = getTestParameter().get("mockInstanceVolumeType");
        volume.setType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        t.setAttachedVolumes(Set.of(volume));

        return t;
    }

    @Override
    public String getClusterName() {
        String clustername = getTestParameter().get("mockClusterName");
        clustername = clustername == null ? MOCK_CLUSTER_DEFAULT_NAME : clustername;
        return clustername;
    }

    public StackAuthenticationV4Request stackAuthentication() {
        StackAuthenticationV4Request stackAuthentication = new StackAuthenticationV4Request();
        stackAuthentication.setPublicKeyId("aszegedi");
        return stackAuthentication;
    }

    @Override
    public String getPlatform() {
        return MOCK_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        String credentialName = getTestParameter().get("mockCredentialName");
        return credentialName == null ? CREDENTIAL_DEFAULT_NAME : credentialName;
    }

    @Override
    public String getBlueprintName() {
        String blueprintName = getTestParameter().get("mockBlueprintName");
        return blueprintName == null ? BLUEPRINT_DEFAULT_NAME : blueprintName;
    }

    @Override
    public String getNetworkName() {
        String networkName = getTestParameter().get("mockNetworkName");
        return networkName == null ? NETWORK_DEFAULT_NAME : networkName;
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = getTestParameter().get("mockSubnetCIDR");
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getVpcId() {
        String vpcId = getTestParameter().get("mockVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    @Override
    public String getSubnetId() {
        String subnetId = getTestParameter().get("mockSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public String getInternetGatewayId() {
        String gatewayId = getTestParameter().get("mockInternetGatewayId");
        return gatewayId == null ? INTERNET_GATEWAY_ID : gatewayId;
    }

    @Override
    public Map<String, Object> newNetworkProperties() {
        return null;
    }

    @Override
    public Map<String, Object> networkProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("internetGatewayId", getInternetGatewayId());
        map.put("vpcId", getVpcId());

        return map;
    }

    @Override
    public Map<String, Object> subnetProperties() {
        Map<String, Object> map = new HashMap<>();
        map.put("subnetId", getSubnetId());
        map.put("vpcId", getVpcId());

        return map;
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
        MockNetworkV4Parameters parameters = new MockNetworkV4Parameters();
        parameters.setInternetGatewayId(getInternetGatewayId());
        parameters.setVpcId(getVpcId());
        network.setMock(parameters);
        return network;
    }

    @Override
    public NetworkV4Request existingSubnet() {
        NetworkV4Request network = new NetworkV4Request();
        MockNetworkV4Parameters parameters = new MockNetworkV4Parameters();
        parameters.setSubnetId(getSubnetId());
        parameters.setVpcId(getVpcId());
        network.setMock(parameters);
        return network;
    }

    private S3CloudStorageV4Parameters s3CloudStorage() {
        S3CloudStorageV4Parameters s3 = new S3CloudStorageV4Parameters();
        s3.setInstanceProfile(getTestParameter().get("NN_AWS_INSTANCE_PROFILE"));
        return s3;
    }

    private Set<StorageLocationV4Request> defaultDatalakeStorageLocationsForProvider(String clusterName) {
        Set<StorageLocationV4Request> request = new LinkedHashSet<>(2);
        request.add(createLocation(
                String.format("s3a://%s/%s/apps/hive/warehouse", getTestParameter().get("NN_AWS_S3_BUCKET_NAME"), clusterName),
                "hive-site",
                "hive.metastore.warehouse.dir"));
        request.add(createLocation(
                String.format("s3a://%s/%s/apps/ranger/audit/%S", getTestParameter().get("NN_AWS_S3_BUCKET_NAME"), clusterName, clusterName),
                "ranger-env",
                "xasecure.audit.destination.hdfs.dir"));
        return request;
    }

    protected StorageLocationV4Request createLocation(String value, String propertyFile, String propertyName) {
        StorageLocationV4Request location = new StorageLocationV4Request();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }
}