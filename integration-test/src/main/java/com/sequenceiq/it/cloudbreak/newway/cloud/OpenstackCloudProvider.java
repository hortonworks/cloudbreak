package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class OpenstackCloudProvider extends CloudProviderHelper {

    public static final String OPENSTACK = "openstack";

    public static final String OPENSTACK_CAPITAL = "OPENSTACK";

    static final String CREDNAME = "testopenstackcred";

    static final String CREDDESC = "test credential";

    private static final String OPENSTACK_CLUSTER_DEFAULT_NAME = "openstack-cluster";

    public OpenstackCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential() {
        return Credential.isCreated()
                .withName(CREDNAME)
                .withDescription(CREDDESC)
                .withCloudPlatform(OPENSTACK_CAPITAL)
                .withParameters(azureCredentialDetails());
    }

    @Override
    String availabilityZone() {
        String az = "nova";
        String azParam = getTestParameter().get("openstackAvailabilityZone");

        return azParam == null ? az : azParam;
    }

    @Override
    String region() {
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
        String instanceTypeDefaultValue = "m1.large";
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
    public String getClusterDefaultName() {
        String clustername = getTestParameter().get("openstackClusterName");
        return clustername == null ? OPENSTACK_CLUSTER_DEFAULT_NAME : clustername;
    }

    Map<String, Object> azureCredentialDetails() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("tenantName", getTestParameter().get("integrationtest.openstackcredential.tenantName"));
        map.put("userName", getTestParameter().get("integrationtest.openstackcredential.userName"));
        map.put("password", getTestParameter().get("integrationtest.openstackcredential.password"));
        map.put("endpoint", getTestParameter().get("integrationtest.openstackcredential.endpoint"));
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");
        return map;
    }

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");
        return network;
    }
}