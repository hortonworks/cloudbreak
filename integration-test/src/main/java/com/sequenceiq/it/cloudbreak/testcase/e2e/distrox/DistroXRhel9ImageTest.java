package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.DistroXUpgradeReplaceVms;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.TestUpgradeCandidateProvider;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXRhel9ImageTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXRhel9ImageTest.class);

    private static final String RHEL9_MIN_RUNTIME_VERSION = "7.3.2";

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
            when = "RHEL9 Data Lake creation and RHEL8 to RHEL9 Data Hub upgrade is started",
            then = "all operation completes succesfully")
    public void testRhel9DataLakeCreationAndRhel8ToRhel9DataHubUpgrade(TestContext testContext) {
        String runtimeVersion = getRuntimeVersion();
        String distrox = resourcePropertyProvider().getName();
        Pair<String, String> sourceAndCandidate = testUpgradeCandidateProvider.getDistroUpgradeSourceAndCandidate(testContext, runtimeVersion,
                Architecture.X86_64, OsType.RHEL8, OsType.RHEL9);
        String rhel8Image = sourceAndCandidate.getLeft();
        String rhel9Image = sourceAndCandidate.getRight();

        testContext.given(SdxTestDto.class)
                .withCloudStorage()
                .withImageId(rhel9Image)
                .withClusterShape(SdxClusterShape.LIGHT_DUTY)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(DistroXImageTestDto.class)
                .withImageId(rhel8Image)
                .given(distrox, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(runtimeVersion))
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(DistroXUpgradeTestDto.class)
                .withImageId(rhel9Image)
                .withReplaceVms(DistroXUpgradeReplaceVms.ENABLED)
                .withLockComponents(true)
                .given(distrox, DistroXTestDto.class)
                .when(distroXTestClient.upgrade())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(validateOSDistroAndImage(OsType.RHEL9, rhel9Image))
                .validate();
    }

    private String getRuntimeVersion() {
        return new VersionComparator().compare(() -> commonClusterManagerProperties().getRuntimeVersion(), () -> RHEL9_MIN_RUNTIME_VERSION) < 0
                ? RHEL9_MIN_RUNTIME_VERSION
                : commonClusterManagerProperties().getRuntimeVersion();
    }

    private static Assertion<DistroXTestDto, CloudbreakClient> validateOSDistroAndImage(OsType osType, String expectedImageId) {
        return (tc, dto, client) -> {
            StackImageV4Response image = dto.getResponse().getImage();
            Log.log(LOGGER, format(" Image Catalog Name: %s ", image.getCatalogName()));
            Log.log(LOGGER, format(" Image Catalog URL: %s ", image.getCatalogUrl()));
            Log.log(LOGGER, format(" Image ID: %s ", image.getId()));

            if (OsType.getByOs(image.getOs()) != osType) {
                throw new TestFailException(String.format("The image os %s does not match, expected %s", image.getOs(), osType));
            }

            if (!expectedImageId.equals(image.getId())) {
                throw new TestFailException(String.format("Expected %s image but current image is %s", expectedImageId, image.getId()));
            }
            return dto;
        };
    }
}
