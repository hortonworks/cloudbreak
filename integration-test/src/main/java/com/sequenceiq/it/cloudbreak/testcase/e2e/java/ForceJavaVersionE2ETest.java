package com.sequenceiq.it.cloudbreak.testcase.e2e.java;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJavaVersionActions;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETest;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;

public class ForceJavaVersionE2ETest extends AbstractE2ETest implements ImageValidatorE2ETest {

    @Value("#{'${integrationtest.java.supportedVersions}'.split(',')}")
    private List<Integer> supportedJavaVersions;

    @Inject
    private SshJavaVersionActions sshJavaVersionActions;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Override
    protected void setupTest(TestContext testContext) {

        // The SafeLogic CryptoComply for Java should be installed on Cloudbreak images. This is validated by default in this test project.
        // So the SafeLogic validation should be disabled for this test suite.
        testContext.skipSafeLogicValidation();

        imageValidatorE2ETestUtil.setupTest(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running environment ",
            when = "create a SDX and DataHub clusters with forcing all supported java versions ",
            then = "clusters are available and default java version is the forced one on all instances ")
    public void testClusterProvisionWithForcedJavaVersion(TestContext testContext) {

        // do not test java versions that are not supported by the image - these will fail anyway
        if (supportedJavaVersions.size() > 1) {
            ImageV4Response imageUnderValidation = imageValidatorE2ETestUtil.getImageUnderValidation(testContext).orElseThrow();
            supportedJavaVersions.removeIf(jdkVersion -> jdkNotSupportedByImage(imageUnderValidation, jdkVersion));
        }

        Integer sdxJavaVersion = supportedJavaVersions.getFirst();
        testContext.given(SdxInternalTestDto.class).withJavaVersion(sdxJavaVersion);
        createDatalakeWithoutDatabase(testContext);
        Map<String, Integer> javaMajorVersions = sshJavaVersionActions.getJavaMajorVersions(getSdxPrivateIpAddresses(testContext));
        validateJavaVersions(sdxJavaVersion, javaMajorVersions);

        // create and validate distrox with all remaining supported java versions, but use the same java as sdx if only a single version is supported
        if (supportedJavaVersions.size() > 1) {
            supportedJavaVersions.remove(sdxJavaVersion);
        }

        Map<Integer, DistroXTestDto> distroXTestDtos = getDistroxTestDtos(testContext, supportedJavaVersions);
        distroXTestDtos.forEach(this::createDistroxWithJavaVersion);
        distroXTestDtos.forEach(this::awaitAndValidateDistroxWithJavaVersion);
    }

    private boolean jdkNotSupportedByImage(ImageV4Response imageUnderValidation, Integer jdkMajorVersion) {

        logger.info(String.format("Checking if JDK %d is supported by image with ID %s!", jdkMajorVersion, imageUnderValidation.getUuid()));
        imageUnderValidation.getPackageVersions().forEach(this::listPackageInfo);
        boolean jdkIsSupported = imageUnderValidation.getPackageVersions().containsKey("java".concat(jdkMajorVersion.toString()));
        if (!jdkIsSupported) {
            logger.warn(String.format("Skipping test with enforced JDK %d as it is not supported by image with ID %s!",
                    jdkMajorVersion, imageUnderValidation.getUuid()));
        }
        return !jdkIsSupported;
    }

    private void listPackageInfo(String name, String version) {
        logger.info(String.format("%s has version %s", name, version));
    }

    private Set<String> getSdxPrivateIpAddresses(TestContext context) {
        SdxInternalTestDto dto = context.get(SdxInternalTestDto.class);
        return dto.getResponse().getStackV4Response().getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }

    private Map<Integer, DistroXTestDto> getDistroxTestDtos(TestContext testContext, List<Integer> supportedJavaVersions) {
        Map<Integer, DistroXTestDto> distroXTestDtos = new HashMap<>();
        supportedJavaVersions.forEach(javaVersion -> distroXTestDtos.put(javaVersion, getDistroxTestDto(testContext, javaVersion)));
        return distroXTestDtos;
    }

    private DistroXTestDto getDistroxTestDto(TestContext testContext, Integer javaVersion) {
        return testContext.given(getDistroxKey(javaVersion).getKey(), DistroXTestDto.class).withJavaVersion(javaVersion);
    }

    private RunningParameter getDistroxKey(Integer javaVersion) {
        return key("distrox-with-java" + javaVersion);
    }

    private void createDistroxWithJavaVersion(Integer javaVersion, DistroXTestDto distroXTestDto) {
        distroXTestDto
                .when(distroXTestClient.create(), getDistroxKey(javaVersion))
                .validate();
    }

    private void awaitAndValidateDistroxWithJavaVersion(Integer javaVersion, DistroXTestDto distroXTestDto) {
        distroXTestDto
                .await(STACK_AVAILABLE, getDistroxKey(javaVersion))
                .awaitForHealthyInstances()
                .validate();
        Map<String, Integer> javaMajorVersions = sshJavaVersionActions.getJavaMajorVersions(getDatahubPrivateIpAddresses(distroXTestDto));
        validateJavaVersions(javaVersion, javaMajorVersions);
    }

    private Set<String> getDatahubPrivateIpAddresses(DistroXTestDto dto) {
        return dto.getResponse().getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }

    private void validateJavaVersions(Integer expectedVersion, Map<String, Integer> javaVersionByInstanceIps) {
        String errorMessage = javaVersionByInstanceIps.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(expectedVersion))
                .map(entry -> String.format("Java version on machine '%s' is JDK%d instead of JDK%d", entry.getKey(), entry.getValue(), expectedVersion))
                .collect(Collectors.joining(", "));

        if (!Strings.isNullOrEmpty(errorMessage)) {
            throw new TestFailException(errorMessage);
        }
    }
}
