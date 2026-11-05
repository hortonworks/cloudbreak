package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2EWithReusableResourcesTest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;
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
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Inject
    private SshJClientActions sshJClientActions;

    private ImageV4Response imageUnderValidation;

    @Override
    protected void setupClass(TestContext testContext) {
        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
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
            given = "There is a running Environment with FreeIPA and Data Lake",
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
            given = "There is a running Environment with FreeIPA and Data Lake",
            then = "Check that the default Java version is the expected one")
    public void testJavaVersionsAreCorrect(TestContext testContext) {

        if (imageValidatorE2ETestUtil.isFreeIpaImageValidation()) {
            testJavaVersionsAreCorrectOnFreeIPA(testContext);
        } else {
            testJavaVersionsAreCorrectOnSdx(testContext);
        }
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "There is a running Environment with FreeIPA and Data Lake",
            then = "Check that the default OS version is the expected one")
    public void testOsVersionsAreCorrect(TestContext testContext) {

        if (imageValidatorE2ETestUtil.isFreeIpaImageValidation()) {
            testOsVersionsAreCorrectOnFreeIPA(testContext);
        } else {
            testOsVersionsAreCorrectOnSdx(testContext);
        }
    }

    private void testPythonVersionsAreCorrectOnFreeIPA(TestContext testContext) {
        throw new TestFailException("This test is not yet implemented for FreeIPA images!");
    }

    private void testJavaVersionsAreCorrectOnFreeIPA(TestContext testContext) {
        throw new TestFailException("This test is not yet implemented for FreeIPA images!");
    }

    private void testOsVersionsAreCorrectOnFreeIPA(TestContext testContext) {
        throw new TestFailException("This test is not yet implemented for FreeIPA images!");
    }

    private void testPythonVersionsAreCorrectOnSdx(TestContext testContext) {
        testVersionsAreCorrectOnSdx(testContext, "python", "python3 --version");
    }

    private void testJavaVersionsAreCorrectOnSdx(TestContext testContext) {
        testVersionsAreCorrectOnSdx(testContext, "java", "java -version");
    }

    private void testOsVersionsAreCorrectOnSdx(TestContext testContext) {
        testVersionsAreCorrectOnSdx(testContext, "os-version", "cat /etc/redhat-release");
    }

    private void testVersionsAreCorrectOnSdx(TestContext testContext, String packageName, String cmd) {
        testContext.given(SdxTestDto.class)
                .then((tc, testDto, client) -> {
                    SshCommandOutcomes results = executeCommandOnInstanceGroup(testContext.get(SdxTestDto.class), cmd);

                    String expectedVersion = extractSemanticVersion(getVersionFromMetadata(packageName));
                    LOGGER.info("Expected {} version: {}", packageName, expectedVersion);
                    LOGGER.info("{} Versions reported by hosts:", Character.toUpperCase(packageName.charAt(0)) + packageName.substring(1));
                    results.forEach(result -> {
                        LOGGER.info("Host {} has {}", result.instanceIp(), result.commandOutput());
                        String actualVersion = extractSemanticVersion(result.commandOutput());
                        assertNotNull(actualVersion, "Actual version can't be null.");
                        assertNotNull(expectedVersion, "Expected version from metadata can't be null.");
                        assertEquals(expectedVersion, actualVersion);
                    });

                    return testDto;
                }).validate();
    }

    private String extractSemanticVersion(String versionString) {
        Pattern p = Pattern.compile("(\\d+(?:\\.\\d+)+(?:[._]\\d+)?)");
        Matcher m = p.matcher(versionString);
        return m.find() ? m.group(1) : null;
    }

    private String getVersionFromMetadata(String packageName) {
        String version = imageUnderValidation.getPackageVersions().getOrDefault(packageName, "");
        String packageNameWithMajorVersion = packageName + version.replace(".", "");
        if (imageUnderValidation.getPackageVersions().containsKey(packageNameWithMajorVersion)) {
            version = imageUnderValidation.getPackageVersions().get(packageNameWithMajorVersion);
        }
        return version;
    }

    // Note: sshjClientActions has a lot of utility methods to check stuff - new functions should be probably
    // added there too instead of implementing them here. That way they could be reusable...
    private SshCommandOutcomes executeCommandOnInstanceGroup(SdxTestDto sdxTestDto, String cmd) {
        return sshJClientActions.executeSshCommandOnHost(sdxTestDto.getResponse().getStackV4Response().getInstanceGroups(),
                List.of(HostGroupType.MASTER.getName()), cmd, false).requireAllSucceeded("Executing '" + cmd + "'");
    }
}
