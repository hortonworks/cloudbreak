package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2EWithReusableResourcesTest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

/**
 * This test suite executes a number of SSH commands on instances to check image contents,
 * but also serves as a well documented example of implementing image validation test cases.
 * Ideally, the tests contained in this one should be able to execute against either FreeIPA, Runtime
 * or even base images.
 */
public class ImageContentValidatorE2ETest extends AbstractE2EWithReusableResourcesTest implements ImageValidatorE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageContentValidatorE2ETest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private SshJClientActions sshJClientActions;

    private ImageV4Response imageUnderValidation;

    @Override
    protected void setupClass(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        imageUnderValidation = imageValidatorE2ETestUtil.getImageUnderValidation(testContext).orElseThrow();
        Architecture architecture = Architecture.fromStringWithFallback(imageUnderValidation.getArchitecture());
        createEnvironmentWithFreeIpa(testContext, architecture);

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setDatabaseEngineVersion(testContext.getCloudProvider().getEmbeddedDbUpgradeSourceVersion());

        SdxTestDto sdxTestDto = testContext.given(SdxTestDto.class)
                .withImage(imageValidatorE2ETestUtil.getImageCatalogName(), imageUnderValidation.getUuid())
                .withCloudStorage()
                .withEmbeddedDatabase(sdxDatabaseRequest);

        sdxTestDto
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "There is a running Environment with FreeIPA and SDX",
            then = "Check that all the expected Python versions are installed")
    public void testPythonVersionsAreCorrect(TestContext testContext) {

        if (imageValidatorE2ETestUtil.isFreeIpaImageValidation()) {
            testPythonVersionsAreCorrectOnFreeIPA(testContext);
        } else {
            testPythonVersionsAreCorrectOnSdx(testContext);
        }
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "There is a running Environment with FreeIPA and SDX",
            then = "Check that all the expected Java versions are installed")
    public void testJavaVersionsAreCorrect(TestContext testContext) {

        if (imageValidatorE2ETestUtil.isFreeIpaImageValidation()) {
            testJavaVersionsAreCorrectOnFreeIPA(testContext);
        } else {
            testJavaVersionsAreCorrectOnSdx(testContext);
        }
    }

    private void testPythonVersionsAreCorrectOnFreeIPA(TestContext testContext) {
        throw new TestFailException("This test is not yet implemented for FreeIPA images!");
    }

    private void testJavaVersionsAreCorrectOnFreeIPA(TestContext testContext) {
        throw new TestFailException("This test is not yet implemented for FreeIPA images!");
    }

    private void testPythonVersionsAreCorrectOnSdx(TestContext testContext) {

        testContext.given(SdxTestDto.class)
            .then((tc, testDto, client) -> {
                Map<String, Pair<Integer, String>> results = executeCommandOnInstanceGroup(
                        testContext.get(SdxTestDto.class), "python3 --version");

                LOGGER.info("Python Versions reported by hosts:");
                results.forEach((key, value) -> {
                    assertEquals(0, value.getLeft());
                    LOGGER.info("Host {} has {}", key, value.getRight());
                    assertEquals(getDefaultPythonForOS(), extractPythonVersion(value.getRight()));
                });

                return testDto;
            }).validate();
    }

    private void testJavaVersionsAreCorrectOnSdx(TestContext testContext) {

        testContext.given(SdxTestDto.class)
                .then((tc, testDto, client) -> {
                    Map<String, Pair<Integer, String>> results = executeCommandOnInstanceGroup(
                            testContext.get(SdxTestDto.class), "java --version");

                    LOGGER.info("Java Versions reported by hosts:");
                    results.forEach((key, value) -> {
                        assertEquals(0, value.getLeft());
                        LOGGER.info("Host {} has {}", key, value.getRight());
                        String javaVersion = extractJavaVersion(value.getRight());
                        assertNotNull(javaVersion);
                        assertTrue(javaVersion.startsWith(getDefaultJavaForOS()));
                    });

                    return testDto;
                }).validate();
    }

    private String extractPythonVersion(String versionString) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)+)");
        Matcher m = p.matcher(versionString);
        return m.find() ? m.group(1) : null;
    }

    private String getDefaultPythonForOS() {
        return switch (OsType.getByOs(imageUnderValidation.getOs())) {
            case OsType.RHEL8 -> "3.6.8";
            case OsType.RHEL9 -> "3.9.21";
            default -> throw new TestFailException(String.format("OS of image %s is not supported by image validation.",
                    imageUnderValidation.getUuid()));
        };
    }

    private String extractJavaVersion(String versionString) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)+)");
        Matcher m = p.matcher(versionString);
        return m.find() ? m.group(1) : null;
    }

    private String getDefaultJavaForOS() {
        return switch (OsType.getByOs(imageUnderValidation.getOs())) {
            case OsType.RHEL8 -> "8.";
            case OsType.RHEL9 -> "17.";
            default -> throw new TestFailException(String.format("OS of image %s is not supported by image validation.",
                    imageUnderValidation.getUuid()));
        };
    }

    // Note: right now this is not working on FreeIPA instances, but fixing it is low priority, so for now this is left
    // as is.
    private void executeCommandOnInstances(FreeIpaTestDto freeIpaTestDto, String cmd) {

        for (String ip : freeIpaTestDto.getAllInstanceIps()) {
            LOGGER.info("Executing command on instance with IP {}", ip);
            LOGGER.info("Python version on host {} is {}.", ip, sshJClientActions.executeSshCommand(ip, cmd));
        }
    }

    // Note: sshjClientActions has a lot of utility methods to check stuff - new functions should be probably
    // added there too instead of implementing them here. That way they could be reusable...
    private Map<String, Pair<Integer, String>> executeCommandOnInstanceGroup(SdxTestDto sdxTestDto, String cmd) {
        try {
            Map<String, Pair<Integer, String>> results = sshJClientActions.executeSshCommandOnHost(
                    sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(), List.of(HostGroupType.MASTER.getName()),
                    "cloudbreak", null, commonCloudProperties.getDefaultPrivateKeyFile(), cmd, false);
            // We're validation that none of the hosts has returned a non-0 error code for the command
            results.forEach((key, value) -> {
                assertEquals(0, value.getLeft());
            });
            return results;
        } catch (Exception e) {
            LOGGER.error("Command execution failed with unexpected error", e);
            throw new TestFailException("Command execution failed with unexpected error: " + e.getMessage(), e);
        }
    }
}
