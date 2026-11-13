package com.sequenceiq.it.util.imagevalidation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.ImageContentValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.imagevalidation.PrewarmImageValidatorE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.java.ForceJavaVersionE2ETest;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.SdxUpgradeTests;
import com.sequenceiq.it.util.TestNGUtil;

@ExtendWith(MockitoExtension.class)
class ImageValidatorE2ETestUtilTest {

    @Mock
    private TestNGUtil testNGUtil;

    @Mock
    private CommonCloudProperties commonCloudProperties;

    @InjectMocks
    private ImageValidatorE2ETestUtil underTest;

    @Captor
    private ArgumentCaptor<Class<?>> testClassCaptor;

    @Captor
    private ArgumentCaptor<String> methodNameCaptor;

    @ParameterizedTest
    @MethodSource("imageValidationTypeAndCloudPlatform")
    void getSuitesTestMethodsExist(ImageValidationType imageValidationType, CloudPlatform cloudPlatform) throws NoSuchMethodException {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "all");
        ReflectionTestUtils.setField(underTest, "imageValidationType", imageValidationType);
        lenient().when(commonCloudProperties.getCloudProvider()).thenReturn(cloudPlatform.getDislayName());
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());
        doNothing().when(testNGUtil).addTestCase(any(), testClassCaptor.capture(), methodNameCaptor.capture());

        underTest.getSuites();

        for (int i = 0; i < testClassCaptor.getAllValues().size(); i++) {
            Class<?> testClass = testClassCaptor.getAllValues().get(i);
            String methodName = methodNameCaptor.getAllValues().get(i);
            if (Arrays.stream(testClass.getMethods()).noneMatch(method -> methodName.equalsIgnoreCase(method.getName()))) {
                fail(String.format("Non-existent test is part of imagevalidation suite: %s.%s", testClass.getSimpleName(), methodName));
            }
        }
    }

    @Test
    void getSuitesShouldNotAddTestCasesWithNone() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "none");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.RUNTIME);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());

        underTest.getSuites();

        verify(testNGUtil, never()).addTestCase(any(), any(), any());
    }

    @Test
    void getSuitesShouldAddAllTestCasesWithAll() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "all");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.RUNTIME);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());
        doNothing().when(testNGUtil).addTestCase(any(), testClassCaptor.capture(), methodNameCaptor.capture());

        underTest.getSuites();

        assertEquals(ImageValidatorE2ETestUtil.ALL_TESTS_FOR_RUNTIME.size() + 1, methodNameCaptor.getAllValues().size(), "All test methods should be added.");
        assertTrue(testClassCaptor.getAllValues().contains(PrewarmImageValidatorE2ETest.class));
        assertTrue(methodNameCaptor.getAllValues().contains("testCreateInternalSdxAndDistrox"));
        ImageValidatorE2ETestUtil.ALL_TESTS_FOR_RUNTIME.forEach((k, v) -> {
            assertTrue(testClassCaptor.getAllValues().contains(v));
            assertTrue(methodNameCaptor.getAllValues().contains(k));
        });
    }

    @Test
    void getSuitesShouldAddAllTestCasesWithAllFreeipa() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "all");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.FREEIPA);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());
        doNothing().when(testNGUtil).addTestCase(any(), testClassCaptor.capture(), methodNameCaptor.capture());

        underTest.getSuites();

        assertEquals(ImageValidatorE2ETestUtil.ALL_TESTS_FOR_FREEIPA.size() + 1, methodNameCaptor.getAllValues().size(), "All test methods should be added.");
        assertTrue(testClassCaptor.getAllValues().contains(PrewarmImageValidatorE2ETest.class));
        assertTrue(methodNameCaptor.getAllValues().contains("testCreateInternalSdxAndDistrox"));
        ImageValidatorE2ETestUtil.ALL_TESTS_FOR_FREEIPA.forEach((k, v) -> {
            assertTrue(testClassCaptor.getAllValues().contains(v));
            assertTrue(methodNameCaptor.getAllValues().contains(k));
        });
    }

    @Test
    void getSuitesShouldAddOnlyTestCasesWithPlusOptions() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "+testSDXUpgrade,+testPythonVersionsAreCorrect");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.RUNTIME);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());
        doNothing().when(testNGUtil).addTestCase(any(), testClassCaptor.capture(), methodNameCaptor.capture());

        underTest.getSuites();

        assertEquals(2, methodNameCaptor.getAllValues().size(), "Only the selected test methods should be added.");
        assertTrue(testClassCaptor.getAllValues().contains(SdxUpgradeTests.class));
        assertTrue(methodNameCaptor.getAllValues().contains("testSDXUpgrade"));
        assertTrue(testClassCaptor.getAllValues().contains(ImageContentValidatorE2ETest.class));
        assertTrue(methodNameCaptor.getAllValues().contains("testPythonVersionsAreCorrect"));
    }

    @Test
    void getSuitesShouldNotAddTestCasesWithMinusOption() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "-testSDXUpgrade,-testClusterProvisionWithForcedJavaVersion");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.RUNTIME);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());
        doNothing().when(testNGUtil).addTestCase(any(), testClassCaptor.capture(), methodNameCaptor.capture());

        underTest.getSuites();

        assertEquals(ImageValidatorE2ETestUtil.ALL_TESTS_FOR_RUNTIME.size() + 1 - 2,
                methodNameCaptor.getAllValues().size(),
                "None of the negated test methods should be added.");
        assertFalse(testClassCaptor.getAllValues().contains(SdxUpgradeTests.class));
        assertFalse(methodNameCaptor.getAllValues().contains("testSDXUpgrade"));
        assertFalse(testClassCaptor.getAllValues().contains(ForceJavaVersionE2ETest.class));
        assertFalse(methodNameCaptor.getAllValues().contains("testClusterProvisionWithForcedJavaVersion"));
    }

    @Test
    void getSuitesShouldNotAddTestCaseWithOnlySubStringEquality() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "+testSDXUpgradeContinuesWithSomething");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.RUNTIME);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());

        underTest.getSuites();

        verify(testNGUtil, never()).addTestCase(any(), any(), any());
    }

    @Test
    void getSuitesShouldNotSkipTestCaseWithOnlySubStringEquality() {
        ReflectionTestUtils.setField(underTest, "runAdditionalTests", "-testSDXUpgradeContinuesWithSomething");
        ReflectionTestUtils.setField(underTest, "imageValidationType", ImageValidationType.RUNTIME);
        when(testNGUtil.createSuite(any())).thenReturn(new XmlSuite());
        when(testNGUtil.createTest(any(), any(), anyBoolean())).thenReturn(new XmlTest());
        doNothing().when(testNGUtil).addTestCase(any(), testClassCaptor.capture(), methodNameCaptor.capture());

        underTest.getSuites();

        assertEquals(ImageValidatorE2ETestUtil.ALL_TESTS_FOR_RUNTIME.size() + 1, methodNameCaptor.getAllValues().size(), "All test methods should be added.");
        assertTrue(testClassCaptor.getAllValues().contains(PrewarmImageValidatorE2ETest.class));
        assertTrue(methodNameCaptor.getAllValues().contains("testCreateInternalSdxAndDistrox"));
        ImageValidatorE2ETestUtil.ALL_TESTS_FOR_RUNTIME.forEach((k, v) -> {
            assertTrue(testClassCaptor.getAllValues().contains(v));
            assertTrue(methodNameCaptor.getAllValues().contains(k));
        });
    }

    static Stream<Arguments> imageValidationTypeAndCloudPlatform() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (ImageValidationType imageValidationType : ImageValidationType.values()) {
            for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
                argumentBuilder.add(Arguments.of(imageValidationType, cloudPlatform));
            }
        }
        return argumentBuilder.build();
    }

}
