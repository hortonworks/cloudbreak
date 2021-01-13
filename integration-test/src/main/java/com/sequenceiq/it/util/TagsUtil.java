package com.sequenceiq.it.util;

import static java.lang.String.format;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.asserts.SoftAssert;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class TagsUtil {

    static final String TEST_NAME_TAG = "test-name";

    static final String MISSING_TEST_NAME_TAG_MESSAGE =
            "TaggedResponse does not have the test name tag, please make sure that the corresponding request implements TaggableRequest";

    static final String MISSING_DEFAULT_TAG = "TaggedResponse is missing the [%s] default tag";

    static final List<String> DEFAULT_TAGS = List.of("owner", "Cloudera-Environment-Resource-Name", "Cloudera-Creator-Resource-Name",
            "Cloudera-Resource-Name");

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

    public void verifyTags(TaggedResponse response, TestContext testContext) {
        SoftAssert softAssert = new SoftAssert();

        try {
            validateTestNameTag(response, testContext);
            DEFAULT_TAGS.forEach(tag -> {
                if (tag.equalsIgnoreCase("owner")) {
                    validateOwnerTag(response, tag, testContext);
                } else if (tag.equalsIgnoreCase("Cloudera-Creator-Resource-Name")) {
                    validateClouderaCreatorResourceNameTag(response, tag, testContext);
                } else {
                    String tagValue = response.getTagValue(tag);
                    if (StringUtils.isEmpty(tagValue)) {
                        tagValue = response.getTagValue(tag.toLowerCase());
                    }
                    Log.log(LOGGER, format(" Default tag: [%s] value is: [%s] Not Null! ", tag, tagValue));
                    softAssert.assertNotNull(tagValue, String.format(MISSING_DEFAULT_TAG, tag));
                }
            });
            softAssert.assertAll();
        } catch (NullPointerException e) {
            LOGGER.error("Tag validation is not possible, because of response: {} throws: {}!", response, e.getMessage(), e);
            throw new TestFailException(String.format(" Tag validation is not possible, because of response: %s", response), e);
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

    private void validateOwnerTag(TaggedResponse response, String tag, TestContext testContext) {
        if (response.getTagValue(tag).equals(testContext.getActingUserName())
                || response.getTagValue(tag).equals(sanitize(testContext.getActingUserName()))) {
            Log.log(LOGGER, format(" Default tag: [%s] value is: [%s] equals [%s] acting user name! ", tag, response.getTagValue(tag),
                    testContext.getActingUserName()));
        } else if (gcpLabelTransformedValue(response.getTagValue(tag), testContext.getActingUserName())) {
            Log.log(LOGGER, format(" Default tag: [%s] value is: [%s] equals [%s] acting user name transformed to a GCP label value! ", tag,
                    response.getTagValue(tag), testContext.getActingUserName()));
        } else {
            LOGGER.error("Default tag: [{}] value is: [{}] NOT equals [{}] acting user name!", tag, response.getTagValue(tag),
                    testContext.getActingUserName());
            throw new TestFailException(String.format(" Default tag: [%s] value is: [%s] NOT equals [%s] acting user name! ",
                    tag, response.getTagValue(tag), testContext.getActingUserName()));
        }
    }

    private String sanitize(String value) {
        return value.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-");
    }

    private boolean gcpLabelTransformedValue(String tagValue, String rawValue) {
        return tagValue.equals(GcpLabelUtil.transformLabelKeyOrValue(rawValue));
    }

    private void validateClouderaCreatorResourceNameTag(TaggedResponse response, String tag, TestContext testContext) {
        Crn actingUserCrn = testContext.getActingUserCrn();

        String tagValue = response.getTagValue(tag);
        if (StringUtils.isEmpty(tagValue)) {
            tagValue = response.getTagValue(tag.toLowerCase());
        }
        if (gcpLabelTransformedValue(tagValue, actingUserCrn.toString())) {
            Log.log(LOGGER, format(" Default tag: [%s] value is: [%s] equals [%s] acting user CRN transformed to a GCP label value! ", tag, tagValue,
                    actingUserCrn));
        } else {
            Crn clouderaCreatorResourceName = Crn.fromString(tagValue);

            if (clouderaCreatorResourceName != null && clouderaCreatorResourceName.equals(actingUserCrn)) {
                Log.log(LOGGER, format(" Default tag: [%s] value is: [%s] equals [%s] acting user CRN! ", tag, clouderaCreatorResourceName, actingUserCrn));
            } else {
                LOGGER.error("Default tag: [{}] value is: [{}] NOT equals [{}] acting user CRN!", tag, clouderaCreatorResourceName, actingUserCrn);
                throw new TestFailException(String.format(" Default tag: [%s] value is: [%s] NOT equals [%s] acting user CRN! ", tag,
                        clouderaCreatorResourceName, actingUserCrn));
            }
        }
    }

    private void validateTestNameTag(TaggedResponse response, TestContext testContext) {
        if (response.getTagValue(TEST_NAME_TAG).toLowerCase().equals(testContext.getTestMethodName().get().toLowerCase())) {
            Log.log(LOGGER, format(" Test name tag: [%s] value is: [%s] equals [%s] test method name! ", TEST_NAME_TAG, response.getTagValue(TEST_NAME_TAG),
                    testContext.getTestMethodName().get()));
        } else {
            LOGGER.error("Test name tag: [{}] value is: [{}] NOT equals [{}] test method name!", TEST_NAME_TAG, response.getTagValue(TEST_NAME_TAG),
                    testContext.getTestMethodName().get());
            throw new TestFailException(String.format(" Test name tag: [%s] value is: [%s] NOT equals [%s] test method name! ", TEST_NAME_TAG,
                    response.getTagValue(TEST_NAME_TAG), testContext.getTestMethodName().get()));
        }
    }
}
