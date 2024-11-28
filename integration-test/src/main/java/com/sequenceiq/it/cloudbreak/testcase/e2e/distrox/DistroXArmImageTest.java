package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static java.lang.String.format;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXArmImageTest extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXArmImageTest.class);

    private static final String ARM64_MIN_RUNTIME_VERSION = "7.3.1";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent with arm64 architecture",
            then = "the DistroX's stack and image should have arm64 architecture")
    public void testDistroXWithArm64ImageCanBeCreatedSuccessfully(TestContext testContext) {
        String runtimeVersion = new VersionComparator().compare(() -> commonClusterManagerProperties().getRuntimeVersion(), () -> ARM64_MIN_RUNTIME_VERSION) < 0
                ? ARM64_MIN_RUNTIME_VERSION
                : commonClusterManagerProperties().getRuntimeVersion();
        createDatalakeWithVersion(testContext, runtimeVersion);

        String distrox = resourcePropertyProvider().getName();

        testContext
                .given(distrox, DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getDataEngDistroXBlueprintName(runtimeVersion))
                .withArchitecture(Architecture.ARM64)
                .when(distroXTestClient.create(), key(distrox))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> {
                    Architecture stackArchitecture = Architecture.fromStringWithFallback(dto.getResponse().getArchitecture());
                    if (stackArchitecture != Architecture.ARM64) {
                        throw new TestFailException(String.format("The stack architecture %s does not match, expected arm64", stackArchitecture));
                    }

                    StackImageV4Response image = dto.getResponse().getImage();
                    Log.log(LOGGER, format(" Image Catalog Name: %s ", image.getCatalogName()));
                    Log.log(LOGGER, format(" Image Catalog URL: %s ", image.getCatalogUrl()));
                    Log.log(LOGGER, format(" Image ID: %s ", image.getId()));

                    if (Architecture.fromStringWithFallback(image.getArchitecture()) != Architecture.ARM64) {
                        throw new TestFailException(String.format("The image architecture %s does not match, expected arm64", image.getArchitecture()));
                    }
                    return dto;
                })
                .validate();
    }
}
