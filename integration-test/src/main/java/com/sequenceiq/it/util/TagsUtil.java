package com.sequenceiq.it.util;

import static java.lang.String.format;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.testng.asserts.SoftAssert;

import com.google.common.base.Objects;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.tag.response.TaggedResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class TagsUtil {

    public static final String REUSABLE_RESOURCE_TAG_PREFIX = "reusable-for";

    static final String TEST_NAME_TAG = "test-name";

    static final String MISSING_TEST_NAME_TAG_MESSAGE =
            "TaggedResponse does not have the test name tag, please make sure that the corresponding request implements TaggableRequest";

    static final String MISSING_DEFAULT_TAG = "TaggedResponse is missing the [%s] default tag";

    static final List<String> DEFAULT_TAGS = List.of("owner", "Cloudera-Environment-Resource-Name", "Cloudera-Creator-Resource-Name",
            "Cloudera-Resource-Name");

    static final String ACTING_USER_CRN_VALUE_FAILURE_PATTERN = "Default tag: [%s] value: [%s] NOT equals [%s] acting user CRN!";

    static final String ACTING_USER_NAME_VALUE_FAILURE_PATTERN = "Default tag: [%s] value: [%s] NOT equals [%s] acting user name! ";

    static final String TEST_NAME_TAG_VALUE_FAILURE_PATTERN = "Test name tag: [%s] value: [%s] NOT equals [%s] test method name!";

    static final String TAG_VALUE_IS_NULL_FAILURE_PATTERN = "[%s] tag validation is not possible, because of the tag value is empty or null!";

    private static final int TAGS_MAX_LENGTH = 128;

    private static final Logger LOGGER = LoggerFactory.getLogger(TagsUtil.class);

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    public void addTestNameTag(CloudPlatform cloudPlatform, CloudbreakTestDto testDto, String testName) {
        if (testDto instanceof AbstractTestDto) {
            Object request = ((AbstractTestDto<?, ?, ?, ?>) testDto).getRequest();
            if (request instanceof TaggableRequest) {
                addTags(cloudPlatform, (TaggableRequest) request, TEST_NAME_TAG, testName);
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
            validateTestNameTag(getTagValueFromResponse(response, TEST_NAME_TAG), TEST_NAME_TAG, testContext);
            DEFAULT_TAGS.forEach(tagKey -> {
                String tagValue = getTagValueFromResponse(response, tagKey);
                if (tagKey.equalsIgnoreCase("owner")) {
                    validateOwnerTag(tagValue, tagKey, testContext);
                } else if (tagKey.equalsIgnoreCase("Cloudera-Creator-Resource-Name")) {
                    validateClouderaCreatorResourceNameTag(tagValue, tagKey, testContext);
                } else {
                    if (StringUtils.isNotBlank(tagValue)) {
                        Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] is present! ", tagKey, tagValue));
                    }
                    softAssert.assertNotNull(tagValue, format(MISSING_DEFAULT_TAG, tagKey));
                }
            });
            softAssert.assertAll();
        } catch (NullPointerException e) {
            LOGGER.error("Tag validation is not possible, because of response: {} throws: {}!", response, e.getMessage(), e);
            throw new TestFailException(format(" Tag validation is not possible, because of response: %s", response), e);
        }
    }

    private void addTags(CloudPlatform cloudPlatform, TaggableRequest taggableRequest, String tagKey, String tagValue) {
        tagKey = applyLengthRestrictions(cloudPlatform, tagKey);
        tagValue = applyLengthRestrictions(cloudPlatform, tagValue);
        taggableRequest.addTag(tagKey, tagValue);
    }

    private String applyLengthRestrictions(CloudPlatform cloudPlatform, String tag) {
        if (CloudPlatform.GCP.equals(cloudPlatform) || tag.length() > TAGS_MAX_LENGTH) {
            tag = gcpLabelUtil.transformLabelKeyOrValue(tag);
        }
        return tag;
    }

    private String sanitize(String tagValue) {
        return tagValue.split("@")[0].toLowerCase(Locale.ROOT).replaceAll("[^\\w]", "-");
    }

    private boolean gcpLabelTransformedValue(String tagValue, String rawValue) {
        return tagValue.equals(gcpLabelUtil.transformLabelKeyOrValue(rawValue));
    }

    private void validateOwnerTag(String tagValue, String tagKey, TestContext testContext) {
        String actingUserName = testContext.getActingUserName();

        if (StringUtils.isNotBlank(tagValue)) {
            if (tagValue.equals(actingUserName) || tagValue.equals(sanitize(actingUserName))) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user name! ", tagKey, tagValue,
                        actingUserName));
            } else if (gcpLabelTransformedValue(tagValue, actingUserName)) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user name transformed to a GCP label value! ", tagKey,
                        tagValue, actingUserName));
            } else {
                String message = format(ACTING_USER_NAME_VALUE_FAILURE_PATTERN, tagKey, tagValue, actingUserName);
                LOGGER.error(message);
                throw new TestFailException(message);
            }
        } else {
            String message = format(TAG_VALUE_IS_NULL_FAILURE_PATTERN, tagKey);
            LOGGER.error(message);
            throw new TestFailException(message);
        }
    }

    private void validateClouderaCreatorResourceNameTag(String tagValue, String tagKey, TestContext testContext) {
        String actingUserCrnString = getActingUserCrn(testContext);

        if (StringUtils.isNotBlank(tagValue) && StringUtils.isNotBlank(actingUserCrnString)) {
            if (tagValue.equals(actingUserCrnString)) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user CRN! ", tagKey, tagValue, actingUserCrnString));
            } else if (gcpLabelTransformedValue(tagValue, actingUserCrnString)) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user CRN transformed to a GCP label value! ", tagKey,
                        tagValue, actingUserCrnString));
            } else {
                Crn actingUserCrn = java.util.Objects.requireNonNull(Crn.fromString(actingUserCrnString));
                Crn creatorCrn = java.util.Objects.requireNonNull(Crn.fromString(tagValue));

                if (crnEqualsWithoutConsideringPartition(actingUserCrn, creatorCrn)) {
                    Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user CRN partitions! ", tagKey, creatorCrn,
                            actingUserCrn));
                } else {
                    String message = format(ACTING_USER_CRN_VALUE_FAILURE_PATTERN, tagKey, tagValue, actingUserCrn);
                    LOGGER.error(message);
                    throw new TestFailException(message);
                }
            }
        } else {
            throw new TestFailException(format("[%s] tag validation is not possible, because of either the tag value [%s] or acting user Crn [%s]" +
                    " is empty or null!", tagKey, tagValue, actingUserCrnString));
        }
    }

    private void validateTestNameTag(String tagValue, String tagKey, TestContext testContext) {
        String testName = testContext.getTestMethodName().orElseThrow(() -> new TestFailException("Test method name cannot be found for tag validation!"));

        if (StringUtils.isNotBlank(tagValue)) {
            testName = applyLengthRestrictions(testContext.getCloudPlatform(), testName);

            if (tagValue.equalsIgnoreCase(testName)) {
                Log.log(LOGGER, format(" PASSED:: [%s] tag value: [%s] equals [%s] test method name! ", tagKey, tagValue, testName));
            } else if (tagValue.contains(REUSABLE_RESOURCE_TAG_PREFIX)) {
                Log.log(LOGGER, format(" PASSED:: [%s] tag value: [%s] contains [%s], skipping exact match with [%s] test method name! ",
                        tagKey, tagValue, REUSABLE_RESOURCE_TAG_PREFIX, testName));
            } else {
                String message = format(TEST_NAME_TAG_VALUE_FAILURE_PATTERN, tagKey, tagValue, testName);
                LOGGER.error(message);
                throw new TestFailException(message);
            }
        } else {
            String message = format(TAG_VALUE_IS_NULL_FAILURE_PATTERN, tagKey);
            LOGGER.error(message);
            throw new TestFailException(message);
        }
    }

    private boolean crnEqualsWithoutConsideringPartition(Crn actingUserCrn, Crn creatorCrn) {
        return Objects.equal(actingUserCrn.getService(), creatorCrn.getService())
                && Objects.equal(actingUserCrn.getRegion(), creatorCrn.getRegion())
                && Objects.equal(actingUserCrn.getAccountId(), creatorCrn.getAccountId())
                && Objects.equal(actingUserCrn.getResourceType(), creatorCrn.getResourceType())
                && Objects.equal(actingUserCrn.getResource(), creatorCrn.getResource());
    }

    private String getTagValueFromResponse(TaggedResponse response, String tagKey) {
        String tagValue = response.getTagValue(tagKey);

        if (StringUtils.isBlank(tagValue)) {
            tagValue = response.getTagValue(tagKey.toLowerCase(Locale.ROOT));
        }
        return tagValue;
    }

    private String getActingUserCrn(TestContext testContext) {
        try {
            return testContext.getActingUserCrn().toString();
        } catch (NullPointerException e) {
            return null;
        }
    }
}
