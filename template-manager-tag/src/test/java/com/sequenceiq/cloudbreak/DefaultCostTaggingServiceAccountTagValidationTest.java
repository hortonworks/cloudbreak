package com.sequenceiq.cloudbreak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CentralTagUpdater;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.tag.TagPreparationObject;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;

@ExtendWith(MockitoExtension.class)
class DefaultCostTaggingServiceAccountTagValidationTest {

    @Mock
    private CentralTagUpdater centralTagUpdater;

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @BeforeEach
    void before() {
        lenient().when(centralTagUpdater.getTagText(any(TagPreparationObject.class), anyString())).then(returnsSecondArg());
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsIsNullShouldThrowValidationFailed()
            throws AccountTagValidationFailed {
        underTest.prepareDefaultTags(tagRequest(null));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsIsEmptyShouldThrowValidationFailed()
            throws AccountTagValidationFailed {
        underTest.prepareDefaultTags(tagRequest(new HashMap<>()));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsNotContainsAccountTagsKey()
            throws AccountTagValidationFailed {
        Map<String, String> userDefinedTags = Map.of("aNotCollidingKey", "aValue");

        underTest.prepareDefaultTags(tagRequest(userDefinedTags));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsContainsAccountTagsKey() {
        when(centralTagUpdater.getTagText(any(TagPreparationObject.class), anyString())).then(returnsSecondArg());
        String collidingTagKey = "accountTagKey2";
        Map<String, String> userDefinedTags = Map.of(collidingTagKey, "a colliding key's Value");

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest(userDefinedTags));

        assertEquals(result.get("accountTagKey2"), "accountTagValue2");
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsContainsMultipleAccountTagsKey() {
        when(centralTagUpdater.getTagText(any(TagPreparationObject.class), anyString())).then(returnsSecondArg());
        String collidingTagKey = "accountTagKey2";
        String collidingTagKey2 = "accountTagKey";
        Map<String, String> resourceTags = Map.of(
                collidingTagKey, "a colliding key's Value",
                collidingTagKey2, "an other colliding key's value");

        Map<String, String> result = underTest.prepareDefaultTags(tagRequest(resourceTags));

        assertEquals(result.get("accountTagKey2"), "accountTagValue2");
        assertEquals(result.get("accountTagKey"), "accountTagValue");
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenAccountTagsIsEmpty() throws AccountTagValidationFailed {
        String notCollidingTagKey = "tagKey2";
        String notCollidingTagKey2 = "tagKey";
        Map<String, String> resourceTags = Map.of(
                notCollidingTagKey, "a not colliding key's Value",
                notCollidingTagKey2, "another not colliding key's value");

        underTest.prepareDefaultTags(tagRequest(resourceTags, new HashMap<>()));
    }

    private CDPTagGenerationRequest tagRequest(Map<String, String> userDefinedResourceTags) {
        return tagRequest(userDefinedResourceTags, null);
    }

    private CDPTagGenerationRequest tagRequest(Map<String, String> userDefinedResourceTags, Map<String, String> accountTags) {
        Map<String, String> defaultccountTagKey = Map.of("accountTagKey", "accountTagValue",
                "accountTagKey2", "accountTagValue2");

        return CDPTagGenerationRequest.Builder.builder()
                .withEnvironmentCrn("environment-crn")
                .withCreatorCrn("creator-crn")
                .withResourceCrn("resource-crn")
                .withUserName("apache1@apache.com")
                .withPlatform(CloudPlatform.MOCK.name())
                .withAccountId("pepsi")
                .withIsInternalTenant(true)
                .withSourceMap(new HashMap<>())
                .withUserDefinedTags(userDefinedResourceTags)
                .withAccountTags(Objects.requireNonNullElseGet(accountTags, () -> defaultccountTagKey))
                .build();
    }
}
