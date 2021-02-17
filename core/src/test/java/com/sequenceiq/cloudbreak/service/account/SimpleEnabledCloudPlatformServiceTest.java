package com.sequenceiq.cloudbreak.service.account;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.platform.PlatformConfig;

@RunWith(MockitoJUnitRunner.class)
public class SimpleEnabledCloudPlatformServiceTest {

    @Mock
    private PlatformConfig platformConfig;

    @InjectMocks
    private final PreferencesService underTest = new PreferencesService();

    @Spy
    private final List<CloudConstant> cloudConstants = new ArrayList<>();

    @Before
    public void setup() {
        cloudConstants.addAll(Sets.newHashSet(createCloudConstant(AWS)));
    }

    private CloudConstant createCloudConstant(String name) {
        return new TestCloudConstant(name);
    }

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

    private static class TestCloudConstant implements CloudConstant {
        private final String name;

        TestCloudConstant(String name) {
            this.name = name;
        }

        @Override
        public Platform platform() {
            return Platform.platform(name);
        }

        @Override
        public Variant variant() {
            return Variant.variant(name);
        }
    }
}
