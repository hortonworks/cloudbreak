package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;

@RunWith(MockitoJUnitRunner.class)
public class MockPlatformResourcesTest {

    private MockPlatformResources underTest = new MockPlatformResources();

    @Test
    public void getDefaultRegionWhenNoDefaultFoundForMockProviderThenShouldReturnWithTheFirstElement() throws Exception {
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1");
        underTest.init();
        CloudRegions regions = underTest.regions(new CloudCredential(1L, "mock"), region("mock"), new HashMap<>());
        Assert.assertEquals("USA", regions.getDefaultRegion());
    }

    @Test
    public void getDefaultRegionWhenDefaultFoundForMockProviderThenShouldReturnWithDefaultElement() throws Exception {
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1,MOCK:Europe");
        underTest.init();
        CloudRegions regions = underTest.regions(new CloudCredential(1L, "mock"), region("mock"), new HashMap<>());
        Assert.assertEquals("Europe", regions.getDefaultRegion());
    }
}