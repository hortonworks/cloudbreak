package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
class JavaVersionValidatorTest {

    private static final String ACCOUNT_ID = "account id";

    private static final int JAVA_VERSION_11 = 11;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private Image image;

    @InjectMocks
    private JavaVersionValidator victim;

    @Test
    public void shouldNotFailOnNullJavaVersion() {
        victim.validateImage(image, null, ACCOUNT_ID);
    }

    @Test
    public void shouldNotFailInCaseOfImageSupportsJavaVersionAndAccountEntitledToForceIt() {
        when(entitlementService.isForcedJavaVersionEnabled(ACCOUNT_ID)).thenReturn(true);
        when(image.getPackageVersions()).thenReturn(Map.of(String.format("java%d", JAVA_VERSION_11), "anyvalue"));

        victim.validateImage(image, JAVA_VERSION_11, ACCOUNT_ID);
    }

    @Test
    public void shoudlFailOnAccountNotEntitledToForceJavaVersion() {
        when(entitlementService.isForcedJavaVersionEnabled(ACCOUNT_ID)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, JAVA_VERSION_11, ACCOUNT_ID));

        assertEquals("Forcing java version is not supported in your account.", exception.getMessage());
    }

    @Test
    public void shoudlFailOnJavaVersionIsNotSupportedByTheImage() {
        when(entitlementService.isForcedJavaVersionEnabled(ACCOUNT_ID)).thenReturn(true);
        when(image.getPackageVersions()).thenReturn(Collections.emptyMap());
        when(image.getUuid()).thenReturn("imageuuid");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> victim.validateImage(image, JAVA_VERSION_11, ACCOUNT_ID));

        assertEquals("The 'imageuuid' image does not support java version 11 to be forced.", exception.getMessage());
    }
}