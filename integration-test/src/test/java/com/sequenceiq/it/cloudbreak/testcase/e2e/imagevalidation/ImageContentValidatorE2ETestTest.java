package com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcomes;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.it.util.imagevalidation.ImageValidatorE2ETestUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@ExtendWith(MockitoExtension.class)
public class ImageContentValidatorE2ETestTest {

    private static final String NOT_IMPLEMENTED_MESSAGE = "This test is not yet implemented for FreeIPA images!";

    private static final String PYTHON_3_VERSION_COMMAND = "python3 --version";

    private static final String PYTHON_3_9_21_RESULT = "Python 3.9.21";

    private static final String PYTHON_3_9_25_RESULT = "Python 3.9.25";

    private static final Map<String, String> PYTHON_PACKAGE_VERSIONS = Map.of("python", "3.9", "python39", "3.9.21");

    private static final String JAVA_VERSION_COMMAND = "java -version";

    private static final String OPENJDK_VERSION_1_8_0_512 = "openjdk version \"1.8.0_512\"";

    private static final String OPENJDK_VERSION_17_0_10_2024_01_16_LTS = "openjdk version \"17.0.10\" 2024-01-16 LTS";

    private static final String OPENJDK_VERSION_17_0_12_2024_07_16_LTS = "openjdk version \"17.0.12\" 2024-07-16 LTS";

    private static final Map<String, String> JAVA8_PACKAGE_VERSIONS = Map.of("java", "8", "java8", "1.8.0_512", "java17", "17.0.12");

    private static final Map<String, String> JAVA17_PACKAGE_VERSIONS = Map.of("java", "17", "java8", "1.8.0_512", "java17", "17.0.12");

    private static final String REDHAT_VERSION_COMMAND = "cat /etc/redhat-release";

    private static final String REDHAT_9_6_RESULT = "Red Hat Enterprise Linux release 9.6 (Plow)";

    private static final String REDHAT9_8_RESULT = "Red Hat Enterprise Linux release 9.8";

    private static final Map<String, String> OS_VERSION_PACKAGE_VERSION = Map.of("os-version", "9.6");

    @Mock
    private TestContext testContext;

    @Mock
    private SdxTestDto sdxTestDto;

    @Mock
    private ImageValidatorE2ETestUtil imageValidatorE2ETestUtil;

    @Mock
    private SshJClientActions sshJClientActions;

    @Mock
    private ImageV4Response imageUnderValidation;

    @InjectMocks
    private ImageContentValidatorE2ETest imageContentValidatorE2ETest;

    @Test
    void testPythonVersionsAreCorrectFailsOnFreeIpa() {
        when(imageValidatorE2ETestUtil.isFreeIpaImageValidation()).thenReturn(true);

        TestFailException exception = assertThrows(TestFailException.class, () -> imageContentValidatorE2ETest.testPythonVersionsAreCorrect(testContext));

        assertEquals(NOT_IMPLEMENTED_MESSAGE, exception.getMessage());
        verify(sshJClientActions, never()).executeSshCommandOnHost(any(), any(), anyString(), eq(false));
    }

    @Test
    void testPythonVersionsAreCorrectWhenDetailedVersionMatches() {
        setUpTestContext(PYTHON_3_VERSION_COMMAND, PYTHON_3_9_21_RESULT, PYTHON_PACKAGE_VERSIONS);

        assertDoesNotThrow(() -> imageContentValidatorE2ETest.testPythonVersionsAreCorrect(testContext));
    }

    @Test
    void testPythonVersionsAreCorrectFailsWhenTheHostDiffers() {
        setUpTestContext(PYTHON_3_VERSION_COMMAND, PYTHON_3_9_25_RESULT, PYTHON_PACKAGE_VERSIONS);

        AssertionError error = assertThrows(AssertionError.class, () -> imageContentValidatorE2ETest.testPythonVersionsAreCorrect(testContext));

        assertEquals("expected: <3.9.21> but was: <3.9.25>", error.getMessage());
    }

    @Test
    void testPythonVersionsAreCorrectFailsWhenMetadataIsMissing() {
        setUpTestContext(PYTHON_3_VERSION_COMMAND, PYTHON_3_9_21_RESULT, Map.of());

        AssertionError error = assertThrows(AssertionError.class, () -> imageContentValidatorE2ETest.testPythonVersionsAreCorrect(testContext));

        assertEquals("Expected version from metadata can't be null. ==> expected: not <null>", error.getMessage());
    }

    @Test
    void testPythonVersionsAreCorrectFailsWhenPackageIsMissing() {
        setUpTestContext(PYTHON_3_VERSION_COMMAND, "command not found: python3", PYTHON_PACKAGE_VERSIONS);

        AssertionError error = assertThrows(AssertionError.class, () -> imageContentValidatorE2ETest.testPythonVersionsAreCorrect(testContext));

        assertEquals("Actual version can't be null. ==> expected: not <null>", error.getMessage());
    }

    @Test
    void testJavaVersionsAreCorrectFailsOnFreeIpa() {
        when(imageValidatorE2ETestUtil.isFreeIpaImageValidation()).thenReturn(true);

        TestFailException exception = assertThrows(TestFailException.class, () -> imageContentValidatorE2ETest.testJavaVersionsAreCorrect(testContext));

        assertEquals(NOT_IMPLEMENTED_MESSAGE, exception.getMessage());
        verify(sshJClientActions, never()).executeSshCommandOnHost(any(), any(), anyString(), eq(false));
    }

    @Test
    void testJavaVersionsAreCorrectWithJava17() {
        setUpTestContext(JAVA_VERSION_COMMAND, OPENJDK_VERSION_17_0_12_2024_07_16_LTS, JAVA17_PACKAGE_VERSIONS);

        assertDoesNotThrow(() -> imageContentValidatorE2ETest.testJavaVersionsAreCorrect(testContext));
    }

    @Test
    void testJavaVersionsAreCorrectWithJava8() {
        setUpTestContext(JAVA_VERSION_COMMAND, OPENJDK_VERSION_1_8_0_512, JAVA8_PACKAGE_VERSIONS);

        assertDoesNotThrow(() -> imageContentValidatorE2ETest.testJavaVersionsAreCorrect(testContext));
    }

    @Test
    void testJavaVersionsAreCorrectFailsWhenPatchVersionDiffers() {
        setUpTestContext(JAVA_VERSION_COMMAND, OPENJDK_VERSION_17_0_10_2024_01_16_LTS, JAVA17_PACKAGE_VERSIONS);

        AssertionError error = assertThrows(AssertionError.class, () -> imageContentValidatorE2ETest.testJavaVersionsAreCorrect(testContext));

        assertEquals("expected: <17.0.12> but was: <17.0.10>", error.getMessage());
    }

    @Test
    void testJavaVersionsAreCorrectFailsWhenDefaultJavaDiffers() {
        setUpTestContext(JAVA_VERSION_COMMAND, OPENJDK_VERSION_17_0_12_2024_07_16_LTS, JAVA8_PACKAGE_VERSIONS);

        AssertionError error = assertThrows(AssertionError.class, () -> imageContentValidatorE2ETest.testJavaVersionsAreCorrect(testContext));

        assertEquals("expected: <1.8.0_512> but was: <17.0.12>", error.getMessage());
    }

    @Test
    void testOsVersionsAreCorrectFailsOnFreeIpa() {
        when(imageValidatorE2ETestUtil.isFreeIpaImageValidation()).thenReturn(true);

        TestFailException exception = assertThrows(TestFailException.class, () -> imageContentValidatorE2ETest.testOsVersionsAreCorrect(testContext));

        assertEquals(NOT_IMPLEMENTED_MESSAGE, exception.getMessage());
        verify(sshJClientActions, never()).executeSshCommandOnHost(any(), any(), anyString(), eq(false));
    }

    @Test
    void testOsVersionsAreCorrectWhenVersionMatches() {
        setUpTestContext(REDHAT_VERSION_COMMAND, REDHAT_9_6_RESULT, OS_VERSION_PACKAGE_VERSION);

        assertDoesNotThrow(() -> imageContentValidatorE2ETest.testOsVersionsAreCorrect(testContext));
    }

    @Test
    void testOsVersionsAreCorrectFailsWhenVersionDiffers() {
        setUpTestContext(REDHAT_VERSION_COMMAND, REDHAT9_8_RESULT, OS_VERSION_PACKAGE_VERSION);

        AssertionError error = assertThrows(AssertionError.class, () -> imageContentValidatorE2ETest.testOsVersionsAreCorrect(testContext));

        assertEquals("expected: <9.6> but was: <9.8>", error.getMessage());
    }

    private void setUpTestContext(String command, String commandResult, Map<String, String> packageVersions) {
        when(imageValidatorE2ETestUtil.isFreeIpaImageValidation()).thenReturn(false);
        when(testContext.given(SdxTestDto.class)).thenReturn(sdxTestDto);
        when(testContext.get(SdxTestDto.class)).thenReturn(sdxTestDto);
        when(sdxTestDto.then(any(Assertion.class))).thenAnswer(invocation -> {
            Assertion<SdxTestDto, SdxClient> assertion = invocation.getArgument(0);
            return assertion.doAssertion(testContext, sdxTestDto, null);
        });
        List<InstanceGroupV4Response> instanceGroups = List.of(new InstanceGroupV4Response());
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setInstanceGroups(instanceGroups);
        SdxClusterDetailResponse sdxClusterDetailResponse = new SdxClusterDetailResponse();
        sdxClusterDetailResponse.setStackV4Response(stackV4Response);
        when(sdxTestDto.getResponse()).thenReturn(sdxClusterDetailResponse);
        List<SshCommandOutcome> outcomes = List.of(SshCommandOutcome.executed("10.0.0.1", 0, commandResult));
        when(sshJClientActions.executeSshCommandOnHost(instanceGroups, List.of(HostGroupType.MASTER.getName()), command, false))
                .thenReturn(new SshCommandOutcomes(outcomes));
        when(imageUnderValidation.getPackageVersions()).thenReturn(packageVersions);
    }
}