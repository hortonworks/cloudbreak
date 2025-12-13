package com.sequenceiq.cloudbreak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tag.CentralTagUpdater;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.tag.TagPreparationObject;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.common.model.DefaultApplicationTag;

@ExtendWith(MockitoExtension.class)
class DefaultCostTaggingServiceTest {

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Mock
    private CentralTagUpdater centralTagUpdater;

    @BeforeEach
    public void before() {
        lenient().when(centralTagUpdater.getTagText(any(TagPreparationObject.class), anyString())).then(returnsSecondArg());
    }

    @Test
    void testPrepareDefaultTagsForAWSShouldReturnAllDefaultMap() {
        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AWS"));

        assertEquals(4L, result.size());
        assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
        assertEquals("resource-id", result.get(DefaultApplicationTag.RESOURCE_ID.key()));
    }

    @Test
    void testPrepareDefaultTagsForAWSAndAdditionalTagsShouldReturnAllDefaultMapPlusTagsWhichAreNotEmpty() {
        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AWS", new HashMap<>(), new HashMap<>()));

        assertEquals(4L, result.size());
        assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
        assertEquals("resource-id", result.get(DefaultApplicationTag.RESOURCE_ID.key()));
    }

    @Test
    void testPrepareDefaultTagsForAZUREWhenOwnerPresentedShouldReturnAllDefaultMap() {
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put(DefaultApplicationTag.owner.key(), "appletree");

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest("AZURE", sourceMap, new HashMap<>()));

        assertEquals(4L, result.size());
        assertEquals("environment-crn", result.get(DefaultApplicationTag.ENVIRONMENT_CRN.key()));
        assertEquals("creator-crn", result.get(DefaultApplicationTag.CREATOR_CRN.key()));
        assertEquals("resource-crn", result.get(DefaultApplicationTag.RESOURCE_CRN.key()));
        assertEquals("resource-id", result.get(DefaultApplicationTag.RESOURCE_ID.key()));
    }

    @Test
    void testMergeTagsShouldReturnWithAnUnion() {
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

        assertEquals(4L, result.size());
        assertEquals("pear3", result.get("pear3"));
        assertEquals("pear4", result.get("pear4"));
        assertEquals("apple3", result.get("apple3"));
        assertEquals("apple4", result.get("apple4"));
    }

    @Test
    void testAccountTagUserTagEqualityNoError() {
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
    void testAccountTagUserTagConflictGeneratesError() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put("apple1", "apple1");
        envMap.put("apple2", "apple2");
        envMap.put("owner", "owner");
        Map<String, String> requestTag = new HashMap<>();
        requestTag.put("pear1", "pear1");
        requestTag.put("owner", "conflict");

        CDPTagGenerationRequest tagRequest = tagRequest("AWS", new HashMap<>(), envMap, requestTag);

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest);

        assertEquals(7L, result.size());
        assertEquals("owner", result.get("owner"));
        assertEquals("creator-crn", result.get("Cloudera-Creator-Resource-Name"));
        assertEquals("apple1", result.get("apple1"));
        assertEquals("resource-crn", result.get("Cloudera-Resource-Name"));
        assertEquals("resource-id", result.get("Cloudera-Resource-ID"));
        assertEquals("environment-crn", result.get("Cloudera-Environment-Resource-Name"));
        assertEquals("apple2", result.get("apple2"));
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
            .withResourceId("resource-id")
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
