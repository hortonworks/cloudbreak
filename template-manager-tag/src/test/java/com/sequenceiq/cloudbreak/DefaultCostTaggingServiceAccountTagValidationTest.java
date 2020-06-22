package com.sequenceiq.cloudbreak;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.tag.AccountTagValidationFailed;
import com.sequenceiq.cloudbreak.tag.CentralTagUpdater;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.common.api.tag.model.Tags;

@ExtendWith(MockitoExtension.class)
public class DefaultCostTaggingServiceAccountTagValidationTest {

    @Mock
    private CentralTagUpdater centralTagUpdater;

    @InjectMocks
    private DefaultCostTaggingService underTest;

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsIsNullShouldThrowValidationFailed()
            throws AccountTagValidationFailed {
        underTest.prepareDefaultTags(tagRequest(null));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsIsEmptyShouldThrowValidationFailed()
            throws AccountTagValidationFailed {
        underTest.prepareDefaultTags(tagRequest(new Tags()));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsNotContainsAccountTagsKey()
            throws AccountTagValidationFailed {
        Tags userDefinedTags = new Tags(Map.of("aNotCollidingKey", "aValue"));

        underTest.prepareDefaultTags(tagRequest(userDefinedTags));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsContainsAccountTagsKey() {
        String collidingTagKey = "accountTagKey2";
        Tags userDefinedTags = new Tags(Map.of(collidingTagKey, "a colliding key's Value"));

        AccountTagValidationFailed exception = Assertions.assertThrows(AccountTagValidationFailed.class,
                () -> underTest.prepareDefaultTags(tagRequest(userDefinedTags)));

        Assertions.assertTrue(exception.getMessage().contains(collidingTagKey));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenUserDefinedResourceTagsContainsMultipleAccountTagsKey() {
        String collidingTagKey = "accountTagKey2";
        String collidingTagKey2 = "accountTagKey";
        Tags resourceTags = new Tags(Map.of(
                collidingTagKey, "a colliding key's Value",
                collidingTagKey2, "an other colliding key's value"));

        AccountTagValidationFailed exception = Assertions.assertThrows(AccountTagValidationFailed.class,
                () -> underTest.prepareDefaultTags(tagRequest(resourceTags)));

        Assertions.assertTrue(exception.getMessage().contains(collidingTagKey));
        Assertions.assertTrue(exception.getMessage().contains(collidingTagKey2));
    }

    @Test
    void prepareDefaultTagsWithResourceTagValidationWhenAccountTagsIsEmpty() throws AccountTagValidationFailed {
        String notCollidingTagKey = "tagKey2";
        String notCollidingTagKey2 = "tagKey";
        Tags resourceTags = new Tags(Map.of(
                notCollidingTagKey, "a not colliding key's Value",
                notCollidingTagKey2, "another not colliding key's value"));

        underTest.prepareDefaultTags(tagRequest(resourceTags, new Tags()));
    }

    private CDPTagGenerationRequest tagRequest(Tags userDefinedResourceTags) {
        return tagRequest(userDefinedResourceTags, null);
    }

    private CDPTagGenerationRequest tagRequest(Tags userDefinedResourceTags, Tags accountTags) {
        Tags defaultAccountTagKey = new Tags(Map.of(
                "accountTagKey", "accountTagValue",
                "accountTagKey2", "accountTagValue2"));

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
                .withAccountTags(Objects.requireNonNullElse(accountTags, defaultAccountTagKey))
                .build();
    }
}
