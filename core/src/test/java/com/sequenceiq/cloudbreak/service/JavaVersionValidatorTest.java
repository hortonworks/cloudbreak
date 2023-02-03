package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;

@ExtendWith(MockitoExtension.class)
class JavaVersionValidatorTest {

    private static final String ACCOUNT_ID = "account id";

    private static final int JAVA_VERSION_11 = 11;

    @Mock
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @Mock
    private Image image;

    @InjectMocks
    private JavaVersionValidator victim;

    @Test
    public void shouldNotFailOnNullJavaVersion() {
        victim.validateImage(image, null, ACCOUNT_ID);
    }

    @Test
    public void shouldNotFailInCaseOfImageSupportsJavaVersion() {
        when(virtualMachineConfiguration.getSupportedJavaVersions()).thenReturn(List.of(JAVA_VERSION_11));
        when(image.getPackageVersions()).thenReturn(Map.of(String.format("java%d", JAVA_VERSION_11), "anyvalue"));

        victim.validateImage(image, JAVA_VERSION_11, ACCOUNT_ID);
    }

    @Test
    public void shouldFailInCaseOfJavaVersionNotSupportedByTheVirtualMachineConfiguration() {
        when(virtualMachineConfiguration.getSupportedJavaVersions()).thenReturn(List.of());

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, JAVA_VERSION_11, ACCOUNT_ID));

        assertEquals("Java version 11 is not supported.", exception.getMessage());
    }

    @Test
    public void shoudlFailOnJavaVersionIsNotSupportedByTheImage() {
        when(virtualMachineConfiguration.getSupportedJavaVersions()).thenReturn(List.of(JAVA_VERSION_11));
        when(image.getPackageVersions()).thenReturn(Collections.emptyMap());
        when(image.getUuid()).thenReturn("imageuuid");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, JAVA_VERSION_11, ACCOUNT_ID));

        assertEquals("The 'imageuuid' image does not support java version 11 to be forced.", exception.getMessage());
    }
}