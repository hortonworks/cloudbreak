package com.sequenceiq.cloudbreak.service.account;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@RunWith(MockitoJUnitRunner.class)
public class SimpleAccountPreferencesServiceTest {

    public static final String AWS = "AWS";

    public static final String OPENSTACK = "OPENSTACK";

    @InjectMocks
    private final SimpleAccountPreferencesService underTest = new SimpleAccountPreferencesService();

    @Spy
    private List<CloudConstant> cloudConstants = new ArrayList<>();

    @Before
    public void setup() {
        cloudConstants.addAll(Sets.newHashSet(createCloudConstant(AWS), createCloudConstant(OPENSTACK)));
    }

    private CloudConstant createCloudConstant(String name) {
        return new CloudConstant() {
            @Override
            public Platform platform() {
                return Platform.platform(name);
            }

            @Override
            public Variant variant() {
                return Variant.variant(name);
            }
        };
    }

    @Test
    public void testEnabledPlatformsWhenEnabledPlatformsIsEmpty() {
        ReflectionTestUtils.setField(underTest, SimpleAccountPreferencesService.class, "enabledPlatforms", "", null);
        Set<String> actual = underTest.enabledPlatforms();

        assertThat(actual, containsInAnyOrder(AWS, OPENSTACK));
    }

    @Test
    public void testEnabledPlatformsWhenEnabledPlatformsIsNotEmpty() {
        ReflectionTestUtils.setField(underTest, SimpleAccountPreferencesService.class, "enabledPlatforms", "AWS,PL1,PL2", null);
        Set<String> actual = underTest.enabledPlatforms();

        assertThat(actual, containsInAnyOrder(AWS, "PL1", "PL2"));
    }
}
