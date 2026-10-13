package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.javaversion.JavaVersionPolicy;
import com.sequenceiq.cloudbreak.service.java.JavaVersionValidator;

@ExtendWith(MockitoExtension.class)
class JavaVersionValidatorTest {

    private static final int JAVA_VERSION_11 = 11;

    private static final int JAVA_VERSION_8 = 8;

    private static final int JAVA_VERSION_21 = 21;

    @Mock
    private JavaVersionPolicy javaVersionPolicy;

    @Mock
    private Image image;

    @InjectMocks
    private JavaVersionValidator victim;

    @Test
    public void shouldNotFailOnNullJavaVersion() {
        victim.validateImage(image, null, null);
    }

    @Test
    public void shouldNotFailInCaseOfImageSupportsJavaVersion() {
        doNothing().when(javaVersionPolicy).validateSupported(any(), anyInt());
        when(image.getPackageVersion(ImagePackageVersion.JAVA)).thenReturn("11");
        when(image.getPackageVersions()).thenReturn(Map.of(
                String.format("java%d", JAVA_VERSION_11), "anyvalue",
                "java", "11"));

        victim.validateImage(image, null, JAVA_VERSION_11);
    }

    @Test
    public void shouldNotFailInCaseOfImageSupportsJava21() {
        doNothing().when(javaVersionPolicy).validateSupported(any(), anyInt());
        when(image.getPackageVersion(ImagePackageVersion.JAVA)).thenReturn("21");
        when(image.getPackageVersions()).thenReturn(Map.of(
                String.format("java%d", JAVA_VERSION_21), "anyvalue",
                "java", "21"));

        victim.validateImage(image, "7.3.3", JAVA_VERSION_21);
    }

    @Test
    public void shouldFailInCaseOfImageDoesNotContainJava21() {
        doNothing().when(javaVersionPolicy).validateSupported(any(), anyInt());
        when(image.getUuid()).thenReturn("imageuuid");
        when(image.getPackageVersion(ImagePackageVersion.JAVA)).thenReturn("17");
        when(image.getPackageVersions()).thenReturn(Map.of(
                String.format("java%d", 17), "anyvalue",
                "java", "17"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, "7.3.2.100", JAVA_VERSION_21));

        assertEquals("The 'imageuuid' image does not contain Java version 21. Choose an image that contains it, or request a different Java version.",
                exception.getMessage());
    }

    @Test
    public void shouldFailInCaseOfJavaVersionNotSupportedByTheJavaVersionPolicy() {
        doThrow(new BadRequestException("java error")).when(javaVersionPolicy).validateSupported(any(), anyInt());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, null, JAVA_VERSION_11));

        assertEquals("java error", exception.getMessage());
    }

    @Test
    public void shoudlFailOnJavaVersionIsNotSupportedByTheImage() {
        doNothing().when(javaVersionPolicy).validateSupported(any(), anyInt());
        when(image.getUuid()).thenReturn("imageuuid");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, null, JAVA_VERSION_11));

        assertEquals("The 'imageuuid' image does not contain Java version 11. Choose an image that contains it, or request a different Java version.",
                exception.getMessage());
    }

    @Test
    public void shouldFailOnJavaVersionBecauseNotJava8WhenNoJavaMetadata() {
        doNothing().when(javaVersionPolicy).validateSupported(any(), anyInt());
        when(image.getUuid()).thenReturn("imageuuid");
        when(image.getPackageVersion(ImagePackageVersion.JAVA)).thenReturn("java8");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, null, JAVA_VERSION_11));

        assertEquals("The 'imageuuid' image does not contain Java version 11. Choose an image that contains it, or request a different Java version.",
                exception.getMessage());
    }

    @Test
    public void shouldNotFailOnJavaVersionBecauseJava8WhenNoJavaMetadata() {
        doNothing().when(javaVersionPolicy).validateSupported(any(), anyInt());

        victim.validateImage(image, null, JAVA_VERSION_8);
    }
}