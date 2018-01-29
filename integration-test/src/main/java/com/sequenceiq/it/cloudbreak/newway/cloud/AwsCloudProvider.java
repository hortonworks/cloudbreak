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

    public static final String AWS_CLUSTER_DEFAULT_NAME = "aws-cluster";

    public static final String KEY_BASED_CREDENTIAL = "key";

    private static final String CREDNAME = "testawscred";

    private static final String CREDDESC = "test credential";

    public AwsCloudProvider() {
    }

    @Override
    public CredentialEntity aValidCredential() {
        String credentialType = TestParameter.get("awsCredentialType");
        Map<String, Object> credentialParameters;
        if (credentialType == null || KEY_BASED_CREDENTIAL.equals(credentialType)) {
            credentialParameters = awsCredentialDetailsKey();
        } else {
            credentialParameters = awsCredentialDetailsArn();
        }
        return Credential.isCreated()
                .withName(CREDNAME)
                .withDescription(CREDDESC)
                .withCloudPlatform(AWS_CAPITAL)
                .withParameters(credentialParameters);
    }

    private Map<String, Object> awsCredentialDetailsArn() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("selector", "role-based");
        map.put("secretKey", TestParameter.get("integrationtest.awscredential.roleArn"));
        return map;
    }

    @Override
    String availabilityZone() {
        String availabilityZone = "eu-west-1a";
        String availabilityZoneParam = TestParameter.get("awsAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    String region() {
        String region = "eu-west-1";
        String regionParam = TestParameter.get("awsRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();
        String defaultValue = "seq-master";
        String param = TestParameter.get("awsPublicKeyId");
        stackauth.setPublicKeyId(param == null ? defaultValue : param);
        return stackauth;
    }

    @Override
    TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();
        String instanceTypeDefaultValue = "m4.xlarge";
        String instanceTypeParam = TestParameter.get("awsInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = TestParameter.get("awsInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 10;
        String volumeSizeParam = TestParameter.get("awsInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "gp2";
        String volumeTypeParam = TestParameter.get("awsInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);
        return t;
    }

    @Override
    public String getClusterDefaultName() {
        String clustername = TestParameter.get("awsClusterName");
        return clustername == null ? AWS_CLUSTER_DEFAULT_NAME : clustername;
    }

    Map<String, Object> awsCredentialDetailsKey() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("selector", "key-based");
        map.put("accessKey", TestParameter.get("integrationtest.awscredential.accessKey"));
        map.put("secretKey", TestParameter.get("integrationtest.awscredential.secretKey"));

        return map;
    }

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");
        return network;
    }
}