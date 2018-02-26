package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

import java.util.HashMap;
import java.util.Map;

public class OpenstackCloudProvider extends CloudProviderHelper {

    public static final String OPENSTACK = "openstack";

    public static final String OPENSTACK_CAPITAL = "OPENSTACK";

    public static final String CREDNAME = "testopenstackcred";

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
                .withParameters(openstackCredentialDetails());
    }

    @Override
    String availabilityZone() {
        String az = "nova";
        String azParam = getTestParameter().get("openstackAvailabilityZone");

        return azParam == null ? az : azParam;
    }

    @Override
    public String region() {
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
        String instanceTypeDefaultValue = "m1.xlarge";
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

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");

        Map<String, Object> parameters = new HashMap<>();

        String defaultNetId = "999e09bc-cf75-4a19-98fb-c0b4ddee6d93";
        String netIdParameter = getTestParameter().get("integrationtest.openstack.publicNetId");
        parameters.put("networkingOption", "self-service");
        parameters.put("publicNetId", netIdParameter == null ? defaultNetId : netIdParameter);
        network.setParameters(parameters);
        return network;
    }

    @Override
    public String getPlatform() {
        return OPENSTACK_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        return CREDNAME;
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