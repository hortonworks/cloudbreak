package com.sequenceiq.it.cloudbreak.v2;

import java.util.Set;

import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeyResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformSshKeysResponse;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;

public class ExistingSshKeySelectionTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({"credentialName", "region", "availabilityZone", "selectedKeyName"})
    public void testSshKeySelection(@Optional("") String credentialName, @Optional("") String region, @Optional("") String availabilityZone,
            String selectedKeyName) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        credentialName = StringUtils.hasText(credentialName) ? credentialName : itContext.getContextParam(CloudbreakV2Constants.CREDENTIAL_NAME);
        region = StringUtils.hasText(region) ? region : itContext.getContextParam(CloudbreakV2Constants.REGION);
        availabilityZone = StringUtils.hasText(availabilityZone) ? availabilityZone : itContext.getContextParam(CloudbreakV2Constants.AVAILABILTYZONE);
        PlatformResourceRequestJson resourceRequestJson = new PlatformResourceRequestJson();
        resourceRequestJson.setCredentialName(credentialName);
        resourceRequestJson.setRegion(region);
        resourceRequestJson.setAvailabilityZone(availabilityZone);
        // WHEN
        PlatformSshKeysResponse response = getCloudbreakClient().connectorV1Endpoint().getCloudSshKeys(resourceRequestJson);
        // THEN
        Set<PlatformSshKeyResponse> regionKeys = response.getSshKeys().get(region);
        Assert.assertNotNull(regionKeys, "keys cannot be null for " + region);
        java.util.Optional<PlatformSshKeyResponse> selected = regionKeys.stream().filter(rk -> rk.getName().equals(selectedKeyName)).findFirst();
        Assert.assertTrue(selected.isPresent(), "the sshkey list doesn't contain [" + selectedKeyName + ']');
        getItContext().putContextParam(CloudbreakV2Constants.SSH_PUBLICKEY_ID, selected.get().getName());
    }
}
