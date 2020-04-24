package com.sequenceiq.it.cloudbreak.assertion.datalake;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;

public class SdxUpgradeTestAssertion {

    private SdxUpgradeTestAssertion() {

    }

    public static Assertion<SdxInternalTestDto, SdxClient> validateReasonContains(String reason) {
        return (testContext, entity, sdxClient) -> {
            SdxUpgradeRequest request = new SdxUpgradeRequest();
            request.setDryRun(true);
            SdxUpgradeResponse upgradeResponse =
                    sdxClient.getSdxClient().sdxUpgradeEndpoint().upgradeClusterByName(entity.getName(), request);
            assertNotNull(upgradeResponse);
            assertTrue(upgradeResponse.getReason().contains(reason));
            return entity;
        };
    }

    public static Assertion<SdxInternalTestDto, SdxClient> validateSucessfulUpgrade() {
        return (testContext, entity, sdxClient) -> {
            SdxUpgradeRequest request = new SdxUpgradeRequest();
            request.setLockComponents(true);
            request.setDryRun(true);
            SdxUpgradeResponse upgradeResponse =
                    sdxClient.getSdxClient().sdxUpgradeEndpoint().upgradeClusterByName(entity.getName(), request);
            assertNotNull(upgradeResponse);
            assertEquals("aaa778fc-7f17-4535-9021-515351df3691", upgradeResponse.getCurrent().getImageId());
            assertEquals(1583391600L, upgradeResponse.getCurrent().getCreated());
            assertEquals("bbbeadd2-5B95-4EC9-B300-13dc43208b64", upgradeResponse.getUpgradeCandidates().get(0).getImageId());
            assertEquals(1583877600L, upgradeResponse.getUpgradeCandidates().get(0).getCreated());
            return entity;
        };
    }
}
