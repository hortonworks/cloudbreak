package com.sequenceiq.it.cloudbreak.newway.cloud;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

import java.util.HashMap;
import java.util.Map;

public class GcpCloudProvider extends CloudProviderHelper {
    public static final String GCP = "gcp";

    public static final String GCP_CAPITAL = "GCP";

    private static final String CREDENTIAL_DEFAULT_NAME = "its-gcp-credandsmoke-cred-ss";

    private static final String GCP_CLUSTER_DEFAULT_NAME = "gcp-cluster";

    public GcpCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential() {
        return Credential.isCreated()
                .withName(getCredentialName())
                .withDescription(CREDENTIAL_DEFAULT_DESCRIPTION)
                .withCloudPlatform(GCP_CAPITAL)
                .withParameters(gcpCredentialDetails());
    }

    @Override
    String availabilityZone() {
        String availabilityZone = "europe-west1-b";
        String availabilityZoneParam = getTestParameter().get("gcpAvailabilityZone");

        return availabilityZoneParam == null ? availabilityZone : availabilityZoneParam;
    }

    @Override
    public String region() {
        String region = "europe-west1";
        String regionParam = getTestParameter().get("gcpRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    public String availibilityZone() {
        String availibilityZone = "europe-west1-b";
        String availibilityZoneParam = getTestParameter().get("gcpAvailibilityZone");

        return availibilityZoneParam == null ? availibilityZone : availibilityZoneParam;
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
        String instanceTypeDefaultValue = "n1-standard-4";
        String instanceTypeParam = getTestParameter().get("gcpInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("gcpInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("gcpInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "pd-standard";
        String volumeTypeParam = getTestParameter().get("gcpInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        return t;
    }

    @Override
    public String getClusterName() {
        String clustername = getTestParameter().get("gcpClusterName");
        return clustername == null ? GCP_CLUSTER_DEFAULT_NAME : clustername;
    }

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");
        return network;
    }

    @Override
    public String getPlatform() {
        return GCP_CAPITAL;
    }

    @Override
    public String getCredentialName() {
        String credentialName = getTestParameter().get("gcpCredentialName");
        return credentialName == null ? CREDENTIAL_DEFAULT_NAME : credentialName;
    }

    public Map<String, Object> gcpCredentialDetails() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyP12File() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", "");

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyProjectId() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", "");
        map.put("serviceAccountId", getTestParameter().get("integrationtest.gcpcredential.serviceAccountId"));
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }

    public Map<String, Object> gcpCredentialDetailsEmptyServiceAccount() {
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", getTestParameter().get("integrationtest.gcpcredential.projectId"));
        map.put("serviceAccountId", "");
        map.put("serviceAccountPrivateKey", getTestParameter().get("integrationtest.gcpcredential.p12File")
                .substring(CloudProviderHelper.BEGIN_INDEX));

        return map;
    }
}