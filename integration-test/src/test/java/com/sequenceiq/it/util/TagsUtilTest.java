package com.sequenceiq.it.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

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
    void verifyTestNameTagShouldNotFailWhenTestDtoIsNotAbstractTestDto() {
        CloudbreakTestDto testDto = mock(CloudbreakTestDto.class);

        assertThatCode(() -> underTest.verifyTestNameTag(testDto))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTestNameTagShouldNotFailWhenTestDtoDoesNotHaveTaggedResponse() {
        CloudbreakTestDto testDto = mock(AbstractTestDto.class);

        assertThatCode(() -> underTest.verifyTestNameTag(testDto))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTestNameTagShouldVerifyTagToAbstractTestDtoWithTaggedResponse() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        StackV4Response response = new StackV4Response();
        TagsV4Response tags = new TagsV4Response();
        tags.setUserDefined(new TagsResponse(Map.of(TagsUtil.TEST_NAME_TAG, TEST_NAME)));
        response.setTags(tags);
        testDto.setResponse(response);

        assertThatCode(() -> underTest.verifyTestNameTag(testDto))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyTestNameTagShouldFailWhenAbstractTestDtoWithTaggedResponseDoesNotHaveTestNameTag() {
        DistroXTestDto testDto = new DistroXTestDto(mock(TestContext.class));
        testDto.setResponse(new StackV4Response());

        assertThatThrownBy(() -> underTest.verifyTestNameTag(testDto))
                .hasMessageContaining(TagsUtil.MISSING_TEST_NAME_TAG_MESSAGE);
    }

}
