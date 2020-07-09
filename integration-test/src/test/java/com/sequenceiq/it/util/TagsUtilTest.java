package com.sequenceiq.it.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.common.api.tag.response.TagsResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

class TagsUtilTest {

    private static final String TEST_NAME = "testname";

    private static final TagsResponse DEFAULT_TAGS = new TagsResponse(Map.of(
            "owner", "whoever",
            "Cloudera-Environment-Resource-Name", "whatever",
            "Cloudera-Creator-Resource-Name", "whoever",
            "Cloudera-Resource-Name", "whatever"
    ));

    private final TagsUtil underTest = new TagsUtil();

    @Test
    void addTestNameTagShouldNotFailWhenTestDtoIsNotAbstractTestDto() {
        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);

        assertThatCode(() -> underTest.addTestNameTag(testDto, TEST_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    void addTestNameTagShouldNotFailWhenTestDtoDoesNotHaveTaggableRequest() {
        CloudbreakTestDto testDto = mock(AbstractTestDto.class);

        assertThatCode(() -> underTest.addTestNameTag(testDto, TEST_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    void addTestNameTagShouldAddTagToAbstractTestDtoWithTaggableRequest() {
        SdxTestDto testDto = new SdxTestDto(mock(TestContext.class));
        SdxClusterRequest request = new SdxClusterRequest();
        testDto.setRequest(request);

        underTest.addTestNameTag(testDto, TEST_NAME);

        assertThat(request.getTags().getTagValue(TagsUtil.TEST_NAME_TAG))
                .isEqualTo(TEST_NAME);
    }

    @Test
    void verifyTagsShouldNotFailWhenTestDtoIsNotAbstractTestDto() {
        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);

        assertThatCode(() -> underTest.verifyTags(testDto))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTagsShouldNotFailWhenTestDtoDoesNotHaveTaggedResponse() {
        CloudbreakTestDto testDto = mock(AbstractTestDto.class);

        assertThatCode(() -> underTest.verifyTags(testDto))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTagsShouldVerifyTagToAbstractTestDtoWithTaggedResponse() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(new TagsResponse(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME)));
        tags.setDefaults(DEFAULT_TAGS);
        response.setTags(tags);
        testDto.setResponse(response);

        assertThatCode(() -> underTest.verifyTags(testDto))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTagsShouldFailWhenAbstractTestDtoWithTaggedResponseDoesNotHaveAllNeededTags() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(new TagsResponse(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME)));
        Map<String, String> defaultTags = new HashMap<>(DEFAULT_TAGS.getAll());
        defaultTags.remove("owner");
        tags.setDefaults(new TagsResponse(defaultTags));
        response.setTags(tags);
        testDto.setResponse(response);

        assertThatThrownBy(() -> underTest.verifyTags(testDto))
                .hasMessageContaining(String.format(TagsUtil.MISSING_DEFAULT_TAG, "owner"))
                .matches(e -> !e.getMessage().contains(TagsUtil.MISSING_TEST_NAME_TAG_MESSAGE))
                .matches(e -> defaultTags.keySet().stream().noneMatch(tag -> e.getMessage().contains(tag)));
    }

    @Test
    void verifyTagsShouldFailWhenAbstractTestDtoWithTaggedResponseDoesNotHaveAnyNeededTags() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        testDto.setResponse(new StackV4Response());

        assertThatThrownBy(() -> underTest.verifyTags(testDto))
                .hasMessageContaining(TagsUtil.MISSING_TEST_NAME_TAG_MESSAGE)
                .satisfies(e ->
                        TagsUtil.DEFAULT_TAGS.forEach(tag ->
                                assertThat(e).hasMessageContaining(String.format(TagsUtil.MISSING_DEFAULT_TAG, tag))));
    }

}
