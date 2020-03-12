package com.sequenceiq.it.cloudbreak.assertion.datalake;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertTrue;
import static org.junit.Assert.assertNotNull;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.UpgradeOptionV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class SdxUpgradeTestAssertion {

    private SdxUpgradeTestAssertion() {

    }

    public static Assertion<SdxInternalTestDto, SdxClient> validateReasonContains(String reason) {
        return (testContext, entity, sdxClient) -> {
            UpgradeOptionV4Response upgradeOptionsV4Response =
                    sdxClient.getSdxClient().sdxEndpoint().checkForUpgradeByName(entity.getName());
            assertNotNull(upgradeOptionsV4Response);
            assertTrue(upgradeOptionsV4Response.getReason().contains(reason));
            return entity;
        };
    }

    public static Assertion<SdxInternalTestDto, SdxClient> validateSucessfulUpgrade() {
        return (testContext, entity, sdxClient) -> {
            UpgradeOptionV4Response upgradeOptionsV4Response =
                    sdxClient.getSdxClient().sdxEndpoint().checkForUpgradeByName(entity.getName());
            assertNotNull(upgradeOptionsV4Response);
            assertEquals("aaa778fc-7f17-4535-9021-515351df3691", upgradeOptionsV4Response.getCurrent().getImageId());
            assertEquals(1583391600L, upgradeOptionsV4Response.getCurrent().getCreated());
            assertEquals("bbbeadd2-5B95-4EC9-B300-13dc43208b64", upgradeOptionsV4Response.getUpgrade().getImageId());
            assertEquals(1583877600L, upgradeOptionsV4Response.getUpgrade().getCreated());
            return entity;
        };
    }
}
