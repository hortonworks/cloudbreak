package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StorageLocationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.S3CloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import org.apache.commons.lang3.NotImplementedException;

public class AwsCloudProvider extends CloudProviderHelper {

    public static final String AWS = "aws";

    public static final String AWS_CAPITAL = "AWS";

    public static final String AWS_CLUSTER_DEFAULT_NAME = "autotesting-aws-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-aws-cred";

    private static final String CREDENTIAL_DEFAULT_DESCRIPTION = "autotesting aws credential";

    private static final String BLUEPRINT_DEFAULT_NAME = "Data Science: Apache Spark 2, Apache Zeppelin";

    private static final String NETWORK_DEFAULT_NAME = "autotesting-aws-net";

    private static final String VPC_DEFAULT_ID = "vpc-e623b28d";

    private static final String INTERNET_GATEWAY_ID = "igw-b55b26dd";

    private static final String SUBNET_DEFAULT_ID = "subnet-83901cfe";

    private static final String DEFAULT_SUBNET_CIDR = "10.0.0.0/16";

    private static final String NETWORK_DEFAULT_DESCRIPTION = "autotesting aws network";

    public AwsCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential(boolean create) {
        String credentialType = getTestParameter().get("awsCredentialType");
        Map<String, Object> credentialParameters;
        credentialParameters = KEY_BASED_CREDENTIAL.equals(credentialType) ? awsCredentialDetailsKey() : awsCredentialDetailsArn();
        CredentialEntity credential = create ? Credential.isCreated() : Credential.request();
        return credential
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(AWS_CAPITAL)
                .withParameters(credentialParameters);
    }

    public Map<String, Object> awsCredentialDetailsArn() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "role-based");
        map.put("roleArn", getTestParameter().get("integrationtest.awscredential.roleArn"));

        return map;
    }

    public Map<String, Object> awsCredentialDetailsInvalidArn() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "role-based");
        map.put("roleArn", "arn:aws:iam::123456789012:role/fake");

        return map;
    }

    @Override
    public String availabilityZone() {
        String availabilityZone = "eu-west-1a";
        String availabilityZoneParam = getTestParameter().get("awsAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public Stack aValidDatalakeStackIsCreated() {
        throw new NotImplementedException("Unimplemented operation!");
    }

    @Override
    public CloudStorageRequest fileSystemForDatalake() {
        throw new NotImplementedException("Unimplemented operation!");
    }

    @Override
    public String region() {
        String region = "eu-west-1";
        String regionParam = getTestParameter().get("awsRegion");

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
        String instanceTypeDefaultValue = "m4.xlarge";
        String instanceTypeParam = getTestParameter().get("awsInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("awsInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("awsInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "gp2";
        String volumeTypeParam = getTestParameter().get("awsInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);
        return t;
    }

    @Override
    public String getClusterName() {
        String clustername = getTestParameter().get("awsClusterName");
        clustername = clustername == null ? AWS_CLUSTER_DEFAULT_NAME : clustername;
        return clustername + getClusterNamePostfix();
    }

    public StackAuthenticationRequest stackAuthentication() {
        StackAuthenticationRequest stackAuthentication = new StackAuthenticationRequest();
        stackAuthentication.setPublicKeyId("aszegedi");
        return stackAuthentication;
    }

    @Override
    public String getPlatform() {
        return AWS_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        String credentialName = getTestParameter().get("awsCredentialName");
        return credentialName == null ? CREDENTIAL_DEFAULT_NAME : credentialName;
    }

    @Override
    public String getBlueprintName() {
        String blueprintName = getTestParameter().get("awsBlueprintName");
        return blueprintName == null ? BLUEPRINT_DEFAULT_NAME : blueprintName;
    }

    @Override
    public String getNetworkName() {
        String networkName = getTestParameter().get("awsNetworkName");
        return networkName == null ? NETWORK_DEFAULT_NAME : networkName;
    }

    @Override
    public String getSubnetCIDR() {
        String subnetCIDR = getTestParameter().get("awsSubnetCIDR");
        return subnetCIDR == null ? DEFAULT_SUBNET_CIDR : subnetCIDR;
    }

    @Override
    public String getVpcId() {
        String vpcId = getTestParameter().get("awsVcpId");
        return vpcId == null ? VPC_DEFAULT_ID : vpcId;
    }

    @Override
    public String getSubnetId() {
        String subnetId = getTestParameter().get("awsSubnetId");
        return subnetId == null ? SUBNET_DEFAULT_ID : subnetId;
    }

    public String getInternetGatewayId() {
        String gatewayId = getTestParameter().get("awsInternetGatewayId");
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

    public Map<String, Object> awsCredentialDetailsKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "key-based");
        map.put("accessKey", getTestParameter().get("integrationtest.awscredential.accessKey"));
        map.put("secretKey", getTestParameter().get("integrationtest.awscredential.secretKey"));

        return map;
    }

    public Map<String, Object> awsCredentialDetailsInvalidAccessKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "key-based");
        map.put("accessKey", "ABCDEFGHIJKLMNOPQRST");
        map.put("secretKey", getTestParameter().get("integrationtest.awscredential.secretKey"));

        return map;
    }

    public Map<String, Object> awsCredentialDetailsInvalidSecretKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("selector", "key-based");
        map.put("accessKey", getTestParameter().get("integrationtest.awscredential.accessKey"));
        map.put("secretKey", "123456789ABCDEFGHIJKLMNOP0123456789=ABC+");

        return map;
    }

    private S3CloudStorageParameters s3CloudStorage() {
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile(getTestParameter().get("NN_AWS_INSTANCE_PROFILE"));
        return s3;
    }

    private Set<StorageLocationRequest> defaultDatalakeStorageLocationsForProvider(String clusterName) {
        Set<StorageLocationRequest> request = new LinkedHashSet<>(2);
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

    private StorageLocationRequest createLocation(String value, String propertyFile, String propertyName) {
        StorageLocationRequest location = new StorageLocationRequest();
        location.setValue(value);
        location.setPropertyFile(propertyFile);
        location.setPropertyName(propertyName);
        return location;
    }
}