package com.sequenceiq.cloudbreak.cloud.mock;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@ExtendWith(MockitoExtension.class)
class MockPlatformResourcesTest {

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @InjectMocks
    private MockPlatformResources underTest = new MockPlatformResources();

    @BeforeEach
    void init() {
        when(cloudbreakResourceReaderService.resourceDefinition(anyString(), anyString())).thenReturn("{\"items\":[]}");
    }

    @Test
    void getDefaultRegionWhenNoDefaultFoundForMockProviderThenShouldReturnWithTheFirstElement() {
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1");
        underTest.init();
        CloudRegions regions = underTest.regions(extendedCloudCredential(new CloudCredential("crn", "mock", "account")),
        region("mock"), new HashMap<>(), true);
        assertEquals("USA", regions.getDefaultRegion());
    }

    @Test
    void getDefaultRegionWhenDefaultFoundForMockProviderThenShouldReturnWithDefaultElement() {
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-1,MOCK:Europe");
        underTest.init();
        CloudRegions regions = underTest.regions(extendedCloudCredential(new CloudCredential("crn", "mock", "account")),
                region("mock"), new HashMap<>(), true);
        assertEquals("Europe", regions.getDefaultRegion());
    }

    private ExtendedCloudCredential extendedCloudCredential(CloudCredential cloudCredential) {
        return new ExtendedCloudCredential(
                cloudCredential,
                "MOCK",
                "",
                "account",
                new ArrayList<>()
        );
    }
}