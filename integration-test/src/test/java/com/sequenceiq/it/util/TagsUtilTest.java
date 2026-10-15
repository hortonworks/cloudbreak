package com.sequenceiq.it.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@ExtendWith(MockitoExtension.class)
class TagsUtilTest {

    private static final String TEST_NAME = "testname";

    private static final String ACTING_USER_NAME = "whoever";

    private static final String OWNER_TAG_KEY = "owner";

    private static final String ACTING_USER_CRN = "crn:cdp:iam:us-west-1:qe-gcp:user:cloudbreak-qe@hortonworks.com";

    private static final String CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY = "Cloudera-Creator-Resource-Name";

    private static final Map<String, String> DEFAULT_TAGS = Map.of(
            OWNER_TAG_KEY, ACTING_USER_NAME,
            "Cloudera-Environment-Resource-Name", "whatever",
            CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY, ACTING_USER_CRN,
            "Cloudera-Resource-Name", "whatever"
    );

    @InjectMocks
    private TagsUtil underTest;

    @Mock
    private MockedTestContext testContext;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Test
    void addTestNameTagShouldNotFailWhenTestDtoIsNotAbstractTestDto() {
        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);
        when(testDto.getCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        assertThatCode(() -> underTest.addTestNameTag(testDto.getCloudPlatform(), testDto, TEST_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    void addTestNameTagShouldNotFailWhenTestDtoDoesNotHaveTaggedRequest() {
        CloudbreakTestDto testDto = mock(AbstractTestDto.class);
        when(testDto.getCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        assertThatCode(() -> underTest.addTestNameTag(testDto.getCloudPlatform(), testDto, TEST_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    void addTestNameTagShouldAddTagToAbstractTestDtoWithTaggableRequest() {
        SdxTestDto testDto = new SdxTestDto(mock(TestContext.class));
        SdxClusterRequest request = new SdxClusterRequest();
        testDto.setRequest(request);
        when(testContext.getCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        underTest.addTestNameTag(testContext.getCloudPlatform(), testDto, TEST_NAME);

        assertThat(request.getTags().get(TagsUtil.TEST_NAME_TAG))
                .isEqualTo(TEST_NAME);
    }

    @Test
    void verifyTagsShouldNotFailWhenTestDtoIsNotAbstractTestDto() {
        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);

        assertThatCode(() -> underTest.verifyTags(testDto, testContext))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTagsShouldNotFailWhenTestDtoDoesNotHaveTaggedResponse() {
        CloudbreakTestDto testDto = mock(AbstractTestDto.class);

        assertThatCode(() -> underTest.verifyTags(testDto, testContext))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTagsShouldVerifyTagToAbstractTestDtoWithTaggedResponse() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME));
        tags.setDefaults(DEFAULT_TAGS);
        response.setTags(tags);
        testDto.setResponse(response);
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));
        when(testContext.getActingUserName()).thenReturn(ACTING_USER_NAME);
        when(testContext.getActingUserCrn()).thenReturn(Crn.fromString(ACTING_USER_CRN));

        assertThatCode(() -> underTest.verifyTags(testDto, testContext))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTagsShouldFailAbstractTestDtoDoesNotHaveAllNeededTags() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME));
        Map<String, String> defaultTags = new HashMap<>(DEFAULT_TAGS);
        defaultTags.remove("owner");
        tags.setDefaults(defaultTags);
        response.setTags(tags);
        testDto.setResponse(response);
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));
        when(testContext.getActingUserName()).thenReturn(ACTING_USER_NAME);

        String expectedMsg = String.format(TagsUtil.TAG_VALUE_IS_NULL_FAILURE_PATTERN, OWNER_TAG_KEY);
        assertThatThrownBy(() -> underTest.verifyTags(testDto, testContext))
                .hasMessageContaining(expectedMsg)
                .matches(e -> !e.getMessage().contains(TagsUtil.MISSING_TEST_NAME_TAG_MESSAGE))
                .matches(e -> defaultTags.keySet().stream().noneMatch(tag -> e.getMessage().contains(tag)));
    }

    @Test
    void verifyTagsShouldFailTestContextHasNullAsActingCrn() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME));
        tags.setDefaults(DEFAULT_TAGS);
        response.setTags(tags);
        testDto.setResponse(response);
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));
        when(testContext.getActingUserName()).thenReturn(ACTING_USER_NAME);
        when(testContext.getActingUserCrn()).thenReturn(null);

        String expectedMsg = String.format("[%s] tag validation is not possible, because of either the tag value [%s] or acting user Crn [%s]" +
                " is empty or null!", CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY, ACTING_USER_CRN, "null");
        assertThatThrownBy(() -> underTest.verifyTags(testDto, testContext))
                .hasMessageContaining(expectedMsg)
                .matches(e -> !e.getMessage().contains(TagsUtil.MISSING_TEST_NAME_TAG_MESSAGE));
    }

    @Test
    void verifyTagsShouldFailAbstractTestDtoDoesNotHaveAnyNeededTags() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        testDto.setResponse(new StackV4Response());
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));

        String expectedMessage = String.format(TagsUtil.TAG_VALUE_IS_NULL_FAILURE_PATTERN, TagsUtil.TEST_NAME_TAG);
        assertThatThrownBy(() -> underTest.verifyTags(testDto, testContext))
                .hasMessageContaining(expectedMessage);
    }

    @Test
    void verifyCreatorWithAltusCrnButTestContextHasCdpCrn() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME));
        Map<String, String> defaultTags = new HashMap<>(DEFAULT_TAGS);
        defaultTags.put(CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY, "crn:altus:iam:us-west-1:qe-gcp:user:cloudbreak-qe@hortonworks.com");
        tags.setDefaults(defaultTags);
        response.setTags(tags);
        testDto.setResponse(response);
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));
        when(testContext.getActingUserName()).thenReturn(ACTING_USER_NAME);
        when(testContext.getActingUserCrn()).thenReturn(Crn.fromString(ACTING_USER_CRN));
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn(CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY);

        assertThatCode(() -> underTest.verifyTags(testDto, testContext))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyCreatorWithCdpCrnButTestContextHasAltusCrn() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME));
        tags.setDefaults(DEFAULT_TAGS);
        response.setTags(tags);
        testDto.setResponse(response);
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));
        when(testContext.getActingUserName()).thenReturn(ACTING_USER_NAME);
        when(testContext.getActingUserCrn()).thenReturn(Crn.fromString("crn:altus:iam:us-west-1:qe-gcp:user:cloudbreak-qe@hortonworks.com"));
        when(gcpLabelUtil.transformLabelKeyOrValue(anyString())).thenReturn(CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY);

        assertThatCode(() -> underTest.verifyTags(testDto, testContext))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyCreatorAsGcpLabelTransformedValue() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME));
        Map<String, String> defaultTags = new HashMap<>(DEFAULT_TAGS);
        defaultTags.put(CLOUDERA_CREATOR_RESOURCE_NAME_TAG_KEY, ACTING_USER_CRN);
        tags.setDefaults(defaultTags);
        response.setTags(tags);
        testDto.setResponse(response);
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_NAME));
        when(testContext.getActingUserName()).thenReturn(ACTING_USER_NAME);
        when(testContext.getActingUserCrn()).thenReturn(Crn.fromString(ACTING_USER_CRN));

        assertThatCode(() -> underTest.verifyTags(testDto, testContext))
                .doesNotThrowAnyException();
    }
}
