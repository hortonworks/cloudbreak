package com.sequenceiq.cloudbreak.common.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@RunWith(MockitoJUnitRunner.class)
public class SimpleEnabledCloudPlatformServiceTest {

    public static final String AWS = "AWS";

    @InjectMocks
    private final ProviderPreferencesService underTest = new ProviderPreferencesService();

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
        ReflectionTestUtils.setField(underTest, ProviderPreferencesService.class, "enabledPlatforms", "", null);
        Set<String> actual = underTest.enabledPlatforms();

        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(AWS));
    }

    @Test
    public void testEnabledPlatformsWhenEnabledPlatformsIsNotEmpty() {
        ReflectionTestUtils.setField(underTest, ProviderPreferencesService.class, "enabledPlatforms", "AWS,PL1,PL2", null);
        Set<String> actual = underTest.enabledPlatforms();

        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(AWS, "PL1", "PL2"));
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

        @Override
        public String[] variants() {
            return new String[] {
                    name
            };
        }
    }
}
