package com.sequenceiq.it.cloudbreak.v2;

import java.util.Set;

import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.PlatformResourceParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformSshKeysV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class ExistingSshKeySelectionTest extends AbstractCloudbreakIntegrationTest {
    @Test
    @Parameters({"credentialName", "region", "availabilityZone", "selectedKeyName"})
    public void testSshKeySelection(@Optional("") String credentialName, @Optional("") String region, @Optional("") String availabilityZone,
            String selectedKeyName) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        credentialName = StringUtils.hasText(credentialName) ? credentialName : itContext.getContextParam(CloudbreakV2Constants.CREDENTIAL_NAME);
        region = StringUtils.hasText(region) ? region : itContext.getContextParam(CloudbreakV2Constants.REGION);
        availabilityZone = StringUtils.hasText(availabilityZone) ? availabilityZone : itContext.getContextParam(CloudbreakV2Constants.AVAILABILTYZONE);
        PlatformResourceParameters resourceRequestJson = new PlatformResourceParameters();
        resourceRequestJson.setCredentialName(credentialName);
        resourceRequestJson.setRegion(region);
        resourceRequestJson.setAvailabilityZone(availabilityZone);
        // WHEN
        PlatformSshKeysV4Response cloudSshKeys = getCloudbreakClient().connectorV4Endpoint().getCloudSshKeys(workspaceId, resourceRequestJson);
        // THEN
        Set<PlatformSshKeyV4Response> regionKeys = cloudSshKeys.getSshKeys().get(region);
        Assert.assertNotNull(regionKeys, "keys cannot be null for " + region);
        java.util.Optional<PlatformSshKeyV4Response> selected = regionKeys.stream().filter(rk -> rk.getName().equals(selectedKeyName)).findFirst();
        Assert.assertTrue(selected.isPresent(), "the sshkey list doesn't contain [" + selectedKeyName + ']');
        getItContext().putContextParam(CloudbreakV2Constants.SSH_PUBLICKEY_ID, selected.get().getName());
    }
}
