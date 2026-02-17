package com.sequenceiq.it.cloudbreak.assertion.datalake;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.sdx.api.model.SdxUpgradeRequest;
import com.sequenceiq.sdx.api.model.SdxUpgradeResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeShowAvailableImages;

public class SdxUpgradeTestAssertion {

    private SdxUpgradeTestAssertion() {

    }

    public static Assertion<SdxInternalTestDto, SdxClient> validateUnsuccessfulUpgrade(String reason) {
        return (testContext, entity, sdxClient) -> {
            SdxUpgradeRequest request = new SdxUpgradeRequest();
            request.setLockComponents(true);
            request.setDryRun(true);
            SdxUpgradeResponse upgradeResponse =
                    sdxClient.getDefaultClient(testContext).sdxUpgradeEndpoint().upgradeClusterByName(entity.getName(), request);
            assertNotNull(upgradeResponse);
            assertTrue("Expected: " + reason + " Actual: " + upgradeResponse.getReason(), upgradeResponse.getReason().contains(reason));
            return entity;
        };
    }

    public static Assertion<SdxInternalTestDto, SdxClient> validateUpgradeCandidateWithLockedComponentIsAvailable() {
        return (testContext, entity, sdxClient) -> {
            SdxUpgradeRequest request = new SdxUpgradeRequest();
            request.setLockComponents(true);
            request.setShowAvailableImages(SdxUpgradeShowAvailableImages.LATEST_ONLY);

            SdxUpgradeResponse upgradeResponse =
                    sdxClient.getDefaultClient(testContext).sdxUpgradeEndpoint().upgradeClusterByName(entity.getName(), request);
            assertNotNull(upgradeResponse);
            assertEquals(upgradeResponse.getCurrent().getImageId(), "aaa778fc-7f17-4535-9021-515351df3691");
            assertEquals(upgradeResponse.getCurrent().getCreated(), 1583391600L);
            assertEquals(upgradeResponse.getUpgradeCandidates().size(), 2);
            assertThat(upgradeResponse.getUpgradeCandidates(),
                    hasItem(allOf(
                            hasProperty("imageId", equalTo("445ccd4a-7882-4aa4-9e0d-04163828e1ac")),
                            hasProperty("created", equalTo(1583877601L)))));
            assertThat(upgradeResponse.getUpgradeCandidates(),
                    hasItem(allOf(
                            hasProperty("imageId", equalTo("bbbeadd2-5B95-4EC9-B300-13dc43208b64")),
                            hasProperty("created", equalTo(1583877600L)))));
            return entity;
        };
    }
}
