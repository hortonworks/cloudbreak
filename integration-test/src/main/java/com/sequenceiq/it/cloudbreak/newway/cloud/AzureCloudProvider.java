package com.sequenceiq.it.cloudbreak.newway.cloud;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;

public class AzureCloudProvider extends CloudProviderHelper {
    public static final String AZURE = "azure";

    public static final String AZURE_CAPITAL = "AZURE";

    private static final String CREDNAME = "itsazurermcredandsmokecred";

    private static final String CREDDESC = "test credential";

    private static final String AZURE_CLUSTER_DEFAULT_NAME = "azure";

    public AzureCloudProvider(TestParameter testParameter) {
        super(testParameter);
    }

    @Override
    public CredentialEntity aValidCredential() {
        return Credential.isCreated()
                .withName(CREDNAME)
                .withDescription(CREDDESC)
                .withCloudPlatform(AZURE_CAPITAL)
                .withParameters(azureCredentialDetails());
    }

    @Override
    String availabilityZone() {
        return null;
    }

    @Override
    String region() {
        String region = "North Europe";
        String regionParam = getTestParameter().get("azureRegion");

        return regionParam == null ? region : regionParam;
    }

    @Override
    StackAuthenticationRequest stackauth() {
        StackAuthenticationRequest stackauth = new StackAuthenticationRequest();
        stackauth.setPublicKey(getTestParameter().get(INTEGRATIONTEST_PUBLIC_KEY_FILE).substring(BEGIN_INDEX));
        return stackauth;
    }

    @Override
    TemplateV2Request template() {
        TemplateV2Request t = new TemplateV2Request();
        String instanceTypeDefaultValue = "Standard_D3_v2";
        String instanceTypeParam = getTestParameter().get("azureInstanceType");
        t.setInstanceType(instanceTypeParam == null ? instanceTypeDefaultValue : instanceTypeParam);

        int volumeCountDefault = 1;
        String volumeCountParam = getTestParameter().get("azureInstanceVolumeCount");
        t.setVolumeCount(volumeCountParam == null ? volumeCountDefault : Integer.parseInt(volumeCountParam));

        int volumeSizeDefault = 100;
        String volumeSizeParam = getTestParameter().get("azureInstanceVolumeSize");
        t.setVolumeSize(volumeSizeParam == null ? volumeSizeDefault : Integer.parseInt(volumeSizeParam));

        String volumeTypeDefault = "Standard_LRS";
        String volumeTypeParam = getTestParameter().get("azureInstanceVolumeType");
        t.setVolumeType(volumeTypeParam == null ? volumeTypeDefault : volumeTypeParam);

        Map<String, Object> params = new HashMap<>();
        params.put("encrypted", "false");
        params.put("managedDisk", "true");
        t.setParameters(params);
        return t;
    }

    @Override
    public String getClusterDefaultName() {
        String clustername = getTestParameter().get("azureClusterName");
        return clustername == null ? AZURE_CLUSTER_DEFAULT_NAME : clustername;
    }

    Map<String, Object> azureCredentialDetails() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("accessKey", getTestParameter().get("integrationtest.azurermcredential.accessKey"));
        map.put("secretKey", getTestParameter().get("integrationtest.azurermcredential.secretKey"));
        map.put("subscriptionId", getTestParameter().get("integrationtest.azurermcredential.subscriptionId"));
        map.put("tenantId", getTestParameter().get("integrationtest.azurermcredential.tenantId"));

        return map;
    }

    @Override
    NetworkV2Request network() {
        NetworkV2Request network = new NetworkV2Request();
        network.setSubnetCIDR("10.0.0.0/16");
        return network;
    }
}