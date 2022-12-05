package com.sequenceiq.cloudbreak.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;

@ExtendWith(MockitoExtension.class)
class PreferencesServiceTest {

    private static final String PLATFORM_AWS = "AWS";

    private static final String PLATFORM_GCP = "GCP";

    private static final String PLATFORM_AZURE = "AZURE";

    @Spy
    private ArrayList<CloudConstant> cloudConstants;

    @Mock
    private CloudConstant cloudConstantAws;

    @Mock
    private CloudConstant cloudConstantGcp;

    @Mock
    private CloudConstant cloudConstantAzure;

    @InjectMocks
    private PreferencesService victim;

    @BeforeEach
    void initTest() {
        cloudConstants.add(cloudConstantAzure);
        cloudConstants.add(cloudConstantAws);
        cloudConstants.add(cloudConstantGcp);
    }

    @Test
    void enabledPlatformsShouldComeFromCloudConstants() {
        when(cloudConstantAzure.platform()).thenReturn(Platform.platform(PLATFORM_AZURE));
        when(cloudConstantAws.platform()).thenReturn(Platform.platform(PLATFORM_AWS));
        when(cloudConstantGcp.platform()).thenReturn(Platform.platform(PLATFORM_GCP));

        Set<String> actual = victim.enabledPlatforms();
        Set<String> expected = Set.of(PLATFORM_AWS, PLATFORM_GCP, PLATFORM_AZURE);

        assertEquals(expected, actual);
    }

    @Test
    void enabledPlatformsShouldBeEmpty() {
        Field enabledGovPlatformsField = ReflectionUtils.findField(PreferencesService.class, "enabledGovPlatforms");
        ReflectionUtils.makeAccessible(enabledGovPlatformsField);
        ReflectionUtils.setField(enabledGovPlatformsField, victim, PLATFORM_AWS);

        Set<String> actual = victim.enabledPlatforms();
        Set<String> expected = Set.of();

        assertEquals(expected, actual);
    }

    @Test
    void enabledPlatformsShouldComeFromProperty() {
        Field enabledGovPlatformsField = ReflectionUtils.findField(PreferencesService.class, "enabledPlatforms");
        ReflectionUtils.makeAccessible(enabledGovPlatformsField);
        ReflectionUtils.setField(enabledGovPlatformsField, victim, PLATFORM_AWS + "," + PLATFORM_GCP);

        Set<String> actual = victim.enabledPlatforms();
        Set<String> expected = Set.of(PLATFORM_AWS, PLATFORM_GCP);

        assertEquals(expected, actual);
    }
}