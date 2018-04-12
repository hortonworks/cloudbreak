package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

import java.util.HashMap;
import java.util.Map;

public class AwsCloudProvider extends CloudProviderHelper {

    public static final String AWS = "aws";

    public static final String AWS_CAPITAL = "AWS";

    public static final String AWS_CLUSTER_DEFAULT_NAME = "autotesting-aws-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    private static final String CREDENTIAL_DEFAULT_NAME = "autotesting-aws-cred";

    public AwsCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential() {
        String credentialType = getTestParameter().get("awsCredentialType");
        Map<String, Object> credentialParameters;
        credentialParameters = KEY_BASED_CREDENTIAL.equals(credentialType) ? awsCredentialDetailsKey() : awsCredentialDetailsArn();
        return Credential.isCreated()
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
        return String.join(clustername == null ? AWS_CLUSTER_DEFAULT_NAME : clustername, getClusterNamePostfix());
    }

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");
        return network;
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
}