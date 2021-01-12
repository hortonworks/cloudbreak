package com.sequenceiq.cloudbreak;

import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.cloudbreak.tag.CentralTagUpdater;
import com.sequenceiq.cloudbreak.tag.DefaultApplicationTag;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.tag.TagPreparationObject;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCostTaggingServiceTest {

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Mock
    private CentralTagUpdater centralTagUpdater;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(centralTagUpdater.getTagText(any(TagPreparationObject.class), anyString())).then(returnsSecondArg());
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
    public void testPrepareDefaultTagsForAWSAndAdditionalTagsShouldReturnAllDefaultMapPlusTagsWhichAreNotEmpty() {
        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AWS", new HashMap<>(), new HashMap<>()));

        Assert.assertEquals(3L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
    }

    @Test
    public void testPrepareDefaultTagsForAZUREWhenOwnerPresentedShouldReturnAllDefaultMap() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put(DefaultApplicationTag.owner.key(), "appletree");

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AZURE", sourceMap, new HashMap<>()));

        Assert.assertEquals(3L, result.size());
        Assert.assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        Assert.assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        Assert.assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
    }

    @Test
    public void testMergeTagsShouldReturnWithAnUnion() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("", "apple1");
        envMap.put("apple2", "");
        envMap.put("apple3", "apple3");
        envMap.put("apple4", "apple4");
        Map<String, String> requestTag = new HashMap<>();
        requestTag.put("pear1", "");
        requestTag.put("", "pear2");
        requestTag.put("pear3", "pear3");
        requestTag.put("pear4", "pear4");

        Map<String, String> result = underTest.mergeTags(mergeRequest("AWS", envMap, requestTag));

        Assert.assertEquals(4L, result.size());
        Assert.assertEquals("pear3", result.get("pear3"));
        Assert.assertEquals("pear4", result.get("pear4"));
        Assert.assertEquals("apple3", result.get("apple3"));
        Assert.assertEquals("apple4", result.get("apple4"));
    }

    @Test
    public void testAccountTagUserTagEqualityNoError() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("apple1", "apple1");
        envMap.put("apple2", "apple2");
        envMap.put("owner", "owner");
        Map<String, String> requestTag = new HashMap<>();
        requestTag.put("pear1", "pear1");
        requestTag.put("owner", "owner");

        CDPTagGenerationRequest tagRequest = tagRequest("AWS", new HashMap<>(), envMap, requestTag);
        underTest.prepareDefaultTags(tagRequest);
    }

    @Test
    public void testAccountTagUserTagConflictGeneratesError() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("apple1", "apple1");
        envMap.put("apple2", "apple2");
        envMap.put("owner", "owner");
        Map<String, String> requestTag = new HashMap<>();
        requestTag.put("pear1", "pear1");
        requestTag.put("owner", "conflict");

        CDPTagGenerationRequest tagRequest = tagRequest("AWS", new HashMap<>(), envMap, requestTag);

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest);

        Assert.assertEquals(6L, result.size());
        Assert.assertEquals("owner", result.get("owner"));
        Assert.assertEquals("creator-crn", result.get("Cloudera-Creator-Resource-Name"));
        Assert.assertEquals("apple1", result.get("apple1"));
        Assert.assertEquals("resource-crn", result.get("Cloudera-Resource-Name"));
        Assert.assertEquals("environment-crn", result.get("Cloudera-Environment-Resource-Name"));
        Assert.assertEquals("apple2", result.get("apple2"));
    }

    private CDPTagGenerationRequest tagRequest(String platform) {
        return tagRequest(platform, new HashMap<>(), new HashMap<>());
    }

    private CDPTagGenerationRequest tagRequest(String platform, Map<String, String> sourceMap, Map<String, String> accountTags) {
        return tagRequest(platform, sourceMap, accountTags, new HashMap<>());
    }

    private CDPTagGenerationRequest tagRequest(String platform, Map<String, String> sourceMap,
        Map<String, String> accountTags, Map<String, String> userTags) {
        return CDPTagGenerationRequest.Builder.builder()
            .withEnvironmentCrn("environment-crn")
            .withCreatorCrn("creator-crn")
            .withResourceCrn("resource-crn")
            .withUserName("apache1@apache.com")
            .withPlatform(platform)
            .withAccountId("pepsi")
            .withIsInternalTenant(true)
            .withSourceMap(sourceMap)
            .withAccountTags(accountTags)
            .withUserDefinedTags(userTags)
            .build();
    }

    private CDPTagMergeRequest mergeRequest(String platform, Map<String, String> envMap, Map<String, String> requestTag) {
        return CDPTagMergeRequest.Builder.builder()
                .withPlatform(platform)
                .withEnvironmentTags(envMap)
                .withRequestTags(requestTag)
                .build();
    }
}
