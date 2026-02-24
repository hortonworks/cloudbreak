package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.TestUpgradeCandidateProvider;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxUpgradeReplaceVms;

public class DistroXRhel9ImageTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRhel9ImageTest.class);

    private static final String VERSION_7_3_1 = "7.3.1";

    private static final String VERSION_7_3_2 = "7.3.2";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestUpgradeCandidateProvider testUpgradeCandidateProvider;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "data lake upgrade is started from 7.3.1 redhat8 to 7.3.2 redhat9",
            then = "the upgrade completes succesfully and the instances are running on redhat9 images")
    public void testDataLakeUpgradeFrom731Rhel8To732Rhel9(TestContext testContext) {
        String distrox = resourcePropertyProvider().getName();
        Pair<String, String> sourceAndCandidate = testUpgradeCandidateProvider.getDistroUpgradeSourceAndCandidate(testContext, VERSION_7_3_2,
                Architecture.X86_64, OsType.RHEL8, OsType.RHEL9, false);
        String rhel9Image = sourceAndCandidate.getRight();

        testContext.given(SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(VERSION_7_3_1)
                .withClusterShape(SdxClusterShape.LIGHT_DUTY)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(SdxUpgradeTestDto.class)
                .withImageId(rhel9Image)
                .withReplaceVms(SdxUpgradeReplaceVms.ENABLED)
                .given(SdxTestDto.class)
                .when(sdxTestClient.upgrade())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(validateOSAndImageForDataLake(OsType.RHEL9, rhel9Image))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "data lake and data hub creation is started with 7.3.2",
            then = "clusters are created with redhat9 os.")
    public void testDataLakeAndDataHubIsCreatedWithRedhat9ArchitectureOn732(TestContext testContext) {
        String distrox = resourcePropertyProvider().getName();

        testContext.given(SdxTestDto.class)
                .withCloudStorage()
                .withRuntimeVersion(VERSION_7_3_2)
                .withClusterShape(SdxClusterShape.LIGHT_DUTY)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .then(validateOSAndImageForDataLake(OsType.RHEL9, null))
                .given(distrox, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(VERSION_7_3_2))
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(validateOSForDataHub(OsType.RHEL9))
                .validate();
    }

    private static Assertion<SdxTestDto, SdxClient> validateOSAndImageForDataLake(OsType osType, String expectedImageId) {
        return (tc, dto, client) -> {
            validateOSAndImage(osType, expectedImageId, dto.getResponse().getStackV4Response().getImage());
            return dto;
        };
    }

    private static Assertion<DistroXTestDto, CloudbreakClient> validateOSForDataHub(OsType osType) {
        return (tc, dto, client) -> {
            validateOSAndImage(osType, null, dto.getResponse().getImage());
            return dto;
        };
    }

    private static void validateOSAndImage(OsType osType, String expectedImageId, StackImageV4Response image) {
        Log.log(LOGGER, format(" Image Catalog Name: %s ", image.getCatalogName()));
        Log.log(LOGGER, format(" Image Catalog URL: %s ", image.getCatalogUrl()));
        Log.log(LOGGER, format(" Image ID: %s ", image.getId()));

        if (OsType.getByOs(image.getOs()) != osType) {
            throw new TestFailException(String.format("The image os %s does not match, expected %s", image.getOs(), osType));
        }

        if (expectedImageId != null && !expectedImageId.equals(image.getId())) {
            throw new TestFailException(String.format("Expected %s image but current image is %s", expectedImageId, image.getId()));
        }
    }
}
