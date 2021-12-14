package com.sequenceiq.it.util;

import static java.lang.String.format;

import java.util.List;

import javax.inject.Inject;

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

    static final String TEST_NAME_TAG = "test-name";

    static final String MISSING_TEST_NAME_TAG_MESSAGE =
            "TaggedResponse does not have the test name tag, please make sure that the corresponding request implements TaggableRequest";

    static final String MISSING_DEFAULT_TAG = "TaggedResponse is missing the [%s] default tag";

    static final List<String> DEFAULT_TAGS = List.of("owner", "Cloudera-Environment-Resource-Name", "Cloudera-Creator-Resource-Name",
            "Cloudera-Resource-Name");

    static final String ACTING_USER_CRN_VALUE_FAILURE_PATTERN = "Default tag: [%s] value is: [%s] NOT equals [%s] acting user CRN!";

    static final String ACTING_USER_NAME_VALUE_FAILURE_PATTERN = "Default tag: [%s] value is: [%s] NOT equals [%s] acting user name! ";

    static final String TEST_NAME_TAG_VALUE_FAILURE_PATTERN = "Test name tag: [%s] value is: [%s] NOT equals [%s] test method name!";

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
                    Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value is: [%s] present! ", tag, tagValue));
                    softAssert.assertNotNull(tagValue, format(MISSING_DEFAULT_TAG, tag));
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

    private String sanitize(String value) {
        return value.split("@")[0].toLowerCase().replaceAll("[^\\w]", "-");
    }

    private boolean gcpLabelTransformedValue(String tagValue, String rawValue) {
        return tagValue.equals(gcpLabelUtil.transformLabelKeyOrValue(rawValue));
    }

    private void validateOwnerTag(TaggedResponse response, String tag, TestContext testContext) {
        String tagValue = response.getTagValue(tag);
        String actingUserName = testContext.getActingUserName();

        if (StringUtils.isNotEmpty(tagValue)) {
            if (tagValue.equals(actingUserName) || tagValue.equals(sanitize(actingUserName))) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user name! ", tag, tagValue, actingUserName));
            } else if (gcpLabelTransformedValue(tagValue, actingUserName)) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user name transformed to a GCP label value! ", tag,
                        tagValue, actingUserName));
            } else {
                String message = format(ACTING_USER_NAME_VALUE_FAILURE_PATTERN, tag, tagValue, actingUserName);
                LOGGER.error(message);
                throw new TestFailException(message);
            }
        } else {
            throw new TestFailException(format("[%s] tag validation is not possible, because of the tag value is empty or null!", tag));
        }
    }

    private void validateClouderaCreatorResourceNameTag(TaggedResponse response, String tag, TestContext testContext) {
        String tagValue = response.getTagValue(tag);
        String actingUser = testContext.getActingUserCrn().toString();

        if (StringUtils.isNotEmpty(tagValue)) {
            if (tagValue.equals(actingUser)) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user CRN! ", tag, tagValue, actingUser));
            } else if (gcpLabelTransformedValue(tagValue, actingUser)) {
                Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user CRN transformed to a GCP label value! ", tag, tagValue,
                        actingUser));
            } else {
                Crn actingUserCrn = java.util.Objects.requireNonNull(Crn.fromString(actingUser));
                Crn creatorCrn = java.util.Objects.requireNonNull(Crn.fromString(tagValue));

                if (crnEqualsWithoutConsideringPartition(actingUserCrn, creatorCrn)) {
                    Log.log(LOGGER, format(" PASSED:: Default tag: [%s] value: [%s] equals [%s] acting user CRN partitions! ", tag, creatorCrn,
                            actingUserCrn));
                } else {
                    String message = format(ACTING_USER_CRN_VALUE_FAILURE_PATTERN, tag, tagValue, actingUserCrn);
                    LOGGER.error(message);
                    throw new TestFailException(message);
                }
            }
        } else {
            throw new TestFailException(format("[%s] tag validation is not possible, because of the tag value is empty or null!", tag));
        }
    }

    private void validateTestNameTag(TaggedResponse response, TestContext testContext) {
        String tagValue = response.getTagValue(TEST_NAME_TAG);
        String testName = testContext.getTestMethodName().orElseThrow(() -> new TestFailException("Test method name cannot be found for tag validation!"));

        if (StringUtils.isNotEmpty(tagValue)) {
            testName = applyLengthRestrictions(testContext.getCloudProvider().getCloudPlatform(), testName);

            if (tagValue.equalsIgnoreCase(testName)) {
                Log.log(LOGGER, format(" PASSED:: [%s] tag value: [%s] equals [%s] test method name! ", TEST_NAME_TAG, tagValue, testName));
            } else {
                String message = format(TEST_NAME_TAG_VALUE_FAILURE_PATTERN, TEST_NAME_TAG, tagValue, testName);
                LOGGER.error(message);
                throw new TestFailException(message);
            }
        } else {
            throw new TestFailException(format("[%s] tag validation is not possible, because of the tag value is empty or null!", TEST_NAME_TAG));
        }
    }

    private boolean crnEqualsWithoutConsideringPartition(Crn actingUserCrn, Crn creatorCrn) {
        return Objects.equal(actingUserCrn.getService(), creatorCrn.getService())
                && Objects.equal(actingUserCrn.getRegion(), creatorCrn.getRegion())
                && Objects.equal(actingUserCrn.getAccountId(), creatorCrn.getAccountId())
                && Objects.equal(actingUserCrn.getResourceType(), creatorCrn.getResourceType())
                && Objects.equal(actingUserCrn.getResource(), creatorCrn.getResource());
    }
}
