package com.sequenceiq.it.util;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.testng.asserts.SoftAssert;

import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class TagsUtil {

    static final String TEST_NAME_TAG = "test-name";

    static final String MISSING_TEST_NAME_TAG_MESSAGE =
            "TaggedResponse does not have the test name tag, please make sure that the corresponding request implements TaggableRequest";

    static final String MISSING_DEFAULT_TAG = "TaggedResponse is missing the [%s] default tag";

    static final List<String> DEFAULT_TAGS = List.of("owner", "Cloudera-Environment-Resource-Name", "Cloudera-Creator-Resource-Name", "Cloudera-Resource-Name");

    private static final int GCP_TAG_MAX_LENGTH = 63;

    private static final Logger LOGGER = LoggerFactory.getLogger(TagsUtil.class);

    public void addTestNameTag(CloudbreakTestDto testDto, String testName) {
        if (testDto instanceof AbstractTestDto) {
            Object request = ((AbstractTestDto<?, ?, ?, ?>) testDto).getRequest();
            if (request instanceof TaggableRequest) {
                addTags((TaggableRequest) request, TEST_NAME_TAG, testName);
            }
        }
    }

    public void verifyTags(CloudbreakTestDto testDto, TestContext testContext) {
        if (testDto instanceof AbstractTestDto) {
            AbstractTestDto<?, ?, ?, ?> abstractTestDto = (AbstractTestDto<?, ?, ?, ?>) testDto;

            if (abstractTestDto.getResponse() instanceof TaggedResponse) {
                verifyTags((TaggedResponse) abstractTestDto.getResponse(), testContext);
            }

            if (!CollectionUtils.isEmpty(abstractTestDto.getResponses())) {
                abstractTestDto.getResponses().stream()
                        .filter(TaggedResponse.class::isInstance)
                        .map(TaggedResponse.class::cast)
                        .forEach(taggedResponse -> verifyTags(taggedResponse, testContext));
            }
        }
    }

    private void addTags(TaggableRequest taggableRequest, String tagKey, String tagValue) {
        tagKey = applyLengthRestrictions(tagKey);
        tagValue = applyLengthRestrictions(tagValue);
        taggableRequest.addTag(tagKey, tagValue);
    }

    private String applyLengthRestrictions(String tag) {
        if (tag.length() > GCP_TAG_MAX_LENGTH) {
            tag = tag.substring(0, GCP_TAG_MAX_LENGTH - 1);
        }
        return tag;
    }

    public void verifyTags(TaggedResponse response, TestContext testContext) {
        SoftAssert softAssert = new SoftAssert();

        Log.log(LOGGER, format(" Verify test name tag: [%s] value is: [%s] Not Null! ", TEST_NAME_TAG, response.getTagValue(TEST_NAME_TAG)));
        softAssert.assertNotNull(response.getTagValue(TEST_NAME_TAG), MISSING_TEST_NAME_TAG_MESSAGE);
        Log.log(LOGGER, format(" Verify test name tag: [%s] value is: [%s] equals [%s] test method name! ", TEST_NAME_TAG,
                response.getTagValue(TEST_NAME_TAG), testContext.getTestMethodName().get()));
        softAssert.assertEquals(response.getTagValue(TEST_NAME_TAG), testContext.getTestMethodName().get(), String.format("Test name tag value is: [%s]" +
                " NOT equals [%s] test method name!", response.getTagValue(TEST_NAME_TAG), testContext.getTestMethodName().get()));
        DEFAULT_TAGS.forEach(tag -> {
            Log.log(LOGGER, format(" Verify default tag: [%s] value is: [%s] Not Null! ", tag, response.getTagValue(tag)));
            softAssert.assertNotNull(response.getTagValue(tag), String.format(MISSING_DEFAULT_TAG, tag));
            if (tag.equalsIgnoreCase("owner")) {
                Log.log(LOGGER, format(" Verify default tag: [%s] value is: [%s] equals [%s] acting user name! ", tag, response.getTagValue(tag),
                        testContext.getActingUserName()));
                softAssert.assertEquals(response.getTagValue(tag), testContext.getActingUserName(), String.format("Default tag: [%s] value is: [%s] " +
                        "NOT equals [%s] acting user name!", tag, response.getTagValue(tag), testContext.getActingUserName()));
            }
            if (tag.equalsIgnoreCase("Cloudera-Creator-Resource-Name")) {
                Log.log(LOGGER, format(" Verify default tag: [%s] value is: [%s] contains [%s] acting user name! ", tag, response.getTagValue(tag),
                        testContext.getActingUserName()));
                softAssert.assertTrue(response.getTagValue(tag).contains(testContext.getActingUserName()), String.format("Default tag: [%s] value is: [%s] " +
                        "NOT contains [%s] acting user name!", tag, response.getTagValue(tag), testContext.getActingUserName()));
            }
        });
        softAssert.assertAll();
    }

}
