package com.sequenceiq.cloudbreak.common.service;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCostTaggingServiceTest {

    @Mock
    private Clock clock;

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareDefaultTagsForAWSShouldReturnAllDefaultMap() {
        long epochSeconds = 1526991986L;
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochSecond(epochSeconds));

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest(CloudConstants.AWS));

        Assert.assertEquals(6L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.OWNER.key()));
        Assert.assertEquals("apache1@apache.com", result.get(DefaultApplicationTag.owner.key()));
        Assert.assertEquals("1526991986", result.get(DefaultApplicationTag.CREATION_TIMESTAMP.key().toLowerCase()));
    }

    @Test
    public void testPrepareDefaultTagsForGCPShouldReturnAllDefaultMap() {
        long epochSeconds = 1526991986L;
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochSecond(epochSeconds));

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest(CloudConstants.GCP));

        Assert.assertEquals(5L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key().toLowerCase()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key().toLowerCase()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key().toLowerCase()));
        Assert.assertEquals("1526991986", result.get(DefaultApplicationTag.CREATION_TIMESTAMP.key().toLowerCase()));
        Assert.assertNull(result.get(DefaultApplicationTag.OWNER.key()));
    }

    @Test
    public void testPrepareDefaultTagsForAZUREWhenOwnerPresentedShouldReturnAllDefaultMap() {
        long epochSeconds = 1526991986L;
        when(clock.getCurrentInstant()).thenReturn(Instant.ofEpochSecond(epochSeconds));
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put(DefaultApplicationTag.OWNER.key(), "appletree");
        sourceMap.put(DefaultApplicationTag.owner.key(), "appletree");

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest(CloudConstants.AZURE, sourceMap));

        Assert.assertEquals(4L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
        Assert.assertEquals("1526991986", result.get(DefaultApplicationTag.CREATION_TIMESTAMP.key().toLowerCase()));
        Assert.assertNull(result.get(DefaultApplicationTag.OWNER.key()));
    }

    private CDPTagGenerationRequest tagRequest(String platform) {
        return tagRequest(platform, new HashMap<>());
    }

    private CDPTagGenerationRequest tagRequest(String platform, Map<String, String> sourceMap) {
        return CDPTagGenerationRequest.Builder.builder()
                .withEnvironmentCrn("environment-crn")
                .withCreatorCrn("creator-crn")
                .withResourceCrn("resource-crn")
                .withUserName("apache1@apache.com")
                .withPlatform(platform)
                .withAccountId("pepsi")
                .withIsInternalTenant(true)
                .withSourceMap(sourceMap)
                .build();
    }
}
