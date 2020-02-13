package com.sequenceiq.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.tag.DefaultApplicationTag;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCostTaggingServiceTest {

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareDefaultTagsForAWSShouldReturnAllDefaultMap() {
        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AWS"));

        Assert.assertEquals(3L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
    }

    @Test
    public void testPrepareDefaultTagsForGCPShouldReturnAllDefaultMap() {
        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("GCP"));

        Assert.assertEquals(3L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key().toLowerCase()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key().toLowerCase()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key().toLowerCase()));
        Assert.assertNull(result.get(DefaultApplicationTag.OWNER.key()));
    }

    @Test
    public void testPrepareDefaultTagsForAZUREWhenOwnerPresentedShouldReturnAllDefaultMap() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put(DefaultApplicationTag.OWNER.key(), "appletree");
        sourceMap.put(DefaultApplicationTag.owner.key(), "appletree");

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AZURE", sourceMap));

        Assert.assertEquals(3L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
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
                .withAccountTags(new HashMap<>())
                .build();
    }
}
