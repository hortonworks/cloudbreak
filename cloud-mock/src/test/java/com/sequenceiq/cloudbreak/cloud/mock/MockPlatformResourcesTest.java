package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@RunWith(MockitoJUnitRunner.class)
public class MockPlatformResourcesTest {

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @InjectMocks
    private MockPlatformResources underTest = new MockPlatformResources();

    @Before
    public void before() {
        when(cloudbreakResourceReaderService.resourceDefinition(anyString(), anyString())).thenReturn("{\"items\":[]}");
    }

    @Test
    public void getDefaultRegionWhenNoDefaultFoundForMockProviderThenShouldReturnWithTheFirstElement() {
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1");
        underTest.init();
        CloudRegions regions = underTest.regions(new CloudCredential("crn", "mock"), region("mock"), new HashMap<>(), true);
        Assert.assertEquals("USA", regions.getDefaultRegion());
    }

    @Test
    public void getDefaultRegionWhenDefaultFoundForMockProviderThenShouldReturnWithDefaultElement() {
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1,MOCK:Europe");
        underTest.init();
        CloudRegions regions = underTest.regions(new CloudCredential("crn", "mock"), region("mock"), new HashMap<>(), true);
        Assert.assertEquals("Europe", regions.getDefaultRegion());
    }
}