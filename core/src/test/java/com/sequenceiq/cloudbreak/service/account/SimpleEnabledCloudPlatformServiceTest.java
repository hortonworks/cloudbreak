package com.sequenceiq.cloudbreak.service.account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.platform.PlatformConfig;

@RunWith(MockitoJUnitRunner.class)
public class SimpleEnabledCloudPlatformServiceTest {

    @Mock
    private PlatformConfig platformConfig;

    @InjectMocks
    private final PreferencesService underTest = new PreferencesService();

    @Test
    public void testEnabledPlatformsWhenEnabledPlatformsIsEmpty() {
        when(platformConfig.getAllPossiblePlatforms()).thenReturn(Collections.emptySet());
        Set<CloudPlatform> actual = underTest.getAllPossiblePlatforms();

        assertThat(actual, empty());
    }

    @Test
    public void testEnabledPlatformsWhenEnabledPlatformsIsNotEmpty() {
        when(platformConfig.getAllPossiblePlatforms()).thenReturn(Set.of(CloudPlatform.AWS, CloudPlatform.GCP));
        Set<CloudPlatform> actual = underTest.getAllPossiblePlatforms();

        assertThat(actual, containsInAnyOrder(CloudPlatform.AWS, CloudPlatform.GCP));
    }
}
