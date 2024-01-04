package com.sequenceiq.it.cloudbreak.testcase.e2e.java;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJavaVersionActions;

public class ForceJavaVersionE2ETest extends AbstractE2ETest {

    private static final int JAVA_VERSION = 11;

    @Inject
    private SshJavaVersionActions sshJavaVersionActions;

    @Override
    protected void setupTest(TestContext testContext) {
        // The SafeLogic CryptoComply for Java should be installed on Cloudbreak images. This is validated by default in this test project.
        // So the SafeLogic validation should be disabled for this test suite.
        testContext.skipSafeLogicValidation();

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running environment ",
            when = "create a SDX and DataHub clusters with forced java version ",
            then = "clusters are available and default java version is the forced one on all instances ")
    public void testClusterProvisionWithForcedJavaVersion(TestContext testContext) {
        testContext.given(SdxInternalTestDto.class).withJavaVersion(JAVA_VERSION);
        createDatalakeWithoutDatabase(testContext);
        validateJavaVersions(sshJavaVersionActions.getJavaMajorVersions(getSdxPrivateIpAddresses(testContext)));

        testContext.given(DistroXTestDto.class).withJavaVersion(JAVA_VERSION);
        createDefaultDatahubForExistingDatalake(testContext);
        validateJavaVersions(sshJavaVersionActions.getJavaMajorVersions(getDatahubPrivateIpAddresses(testContext)));
    }

    private Set<String> getSdxPrivateIpAddresses(TestContext context) {
        SdxInternalTestDto dto = context.get(SdxInternalTestDto.class);
        return dto.getResponse().getStackV4Response().getInstanceGroups().stream()
                .flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }

    private Set<String> getDatahubPrivateIpAddresses(TestContext context) {
        DistroXTestDto dto = context.get(DistroXTestDto.class);
        return dto.getResponse().getInstanceGroups().stream().flatMap(instanceGroupV4Response -> instanceGroupV4Response.getMetadata().stream())
                .map(InstanceMetaDataV4Response::getPrivateIp)
                .collect(Collectors.toSet());
    }

    private void validateJavaVersions(Map<String, Integer> javaVersionByInstanceIps) {
        String errorMessage = javaVersionByInstanceIps.entrySet().stream()
                .filter(entry -> !entry.getValue().equals(JAVA_VERSION))
                .map(entry -> String.format("Java version on '%s' is '%d'", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));

        if (!Strings.isNullOrEmpty(errorMessage)) {
            throw new TestFailException(errorMessage);
        }
    }
}
