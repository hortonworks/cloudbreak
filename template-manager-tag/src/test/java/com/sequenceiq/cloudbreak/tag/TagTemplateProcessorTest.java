package com.sequenceiq.cloudbreak.tag;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;

@ExtendWith(MockitoExtension.class)
class TagTemplateProcessorTest {

    private static final String ACCOUNT_ID = "anAccountId";

    private static final String RESOURCE_CRN = "aResourceCrn";

    private static final String USER_ID = "aUser@cldr.com";

    private static final String USER_CRN = "aUserCrn";

    @InjectMocks
    private TagTemplateProcessor underTest;

    @BeforeEach
    void setUp() {
        Clock clock = new Clock();
        ReflectionTestUtils.setField(underTest, "clock", clock);
    }

    @Test
    void testProcessWhenTemplateIsEmptyString() throws IOException {
        String emptyString = "";

        String actual = underTest.process(emptyString, getTagPreparationObject());

        Assertions.assertEquals(emptyString, actual);
    }

    @Test
    void testProcessWhenModelDoesNotHavePropertyThatIsReferencedFromTemplate() throws IOException {
        String notExistingExpressionReference = "{{{notExistingProperty}}}";

        String actual = underTest.process(notExistingExpressionReference, getTagPreparationObject());

        Assertions.assertEquals(notExistingExpressionReference, actual);
    }

    @Test
    void testProcessWhenTemplateContainsSampleText() throws IOException {
        String anExampleTextValue = "anExampleTextValue";

        String actual = underTest.process(anExampleTextValue, getTagPreparationObject());

        Assertions.assertEquals(anExampleTextValue, actual);
    }

    @Test
    void testProcessWhenModelDoesHavePropertyThatIsReferencedFromTemplateButNull() throws IOException {
        String accountIdReference = "{{{accountId}}}";
        TagPreparationObject model = TagPreparationObject.Builder
                .builder()
                .build();

        String actual = underTest.process(accountIdReference, model);

        Assertions.assertEquals(accountIdReference, actual);
    }

    @Test
    void testProcessWhenModelDoesHavePropertyThatIsReferencedFromTemplateButEmptyString() throws IOException {
        String accountIdReference = "{{{accountId}}}";
        String emptyString = "";
        TagPreparationObject model = TagPreparationObject.Builder
                .builder()
                .withAccountId(emptyString)
                .build();

        String actual = underTest.process(accountIdReference, model);

        Assertions.assertEquals(emptyString, actual);
    }

    @Test
    void testProcessWhenModelDoesHaveMultiplePropertiesThatIsReferencedFromTemplate() throws IOException {
        String valuePattern = "%s and %s on %s";
        String templateWithMultipleReference = String.format(valuePattern, "{{{accountId}}}", "{{{userName}}}", "{{{cloudPlatform}}}");
        TagPreparationObject model = getTagPreparationObject();

        String actual = underTest.process(templateWithMultipleReference, model);

        String expected = String.format(valuePattern, model.getAccountId(), model.getUserName(), model.getCloudPlatform());
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "accountId", "cloudPlatform", "resourceCrn", "userName", "userCrn" })
    void testProcessWhenModelDoesHavePropertyThatIsReferencedFromTemplateShouldReplaceExpressionWithPropertyValue(String propertyName) throws IOException {
        String notExistingExpressionReference = "{{{" + propertyName + "}}}";
        TagPreparationObject tagPreparationObject = getTagPreparationObject();

        String actual = underTest.process(notExistingExpressionReference, tagPreparationObject);

        Assertions.assertTrue(StringUtils.isNotEmpty(actual));
        Object expected = ReflectionTestUtils.getField(tagPreparationObject, propertyName);
        Assertions.assertEquals(String.valueOf(expected), actual);
    }

    private TagPreparationObject getTagPreparationObject() {
        return TagPreparationObject.Builder.builder()
                .withAccountId(ACCOUNT_ID)
                .withCloudPlatform(CloudPlatform.MOCK.name())
                .withResourceCrn(RESOURCE_CRN)
                .withUserName(USER_ID)
                .withUserCrn(USER_CRN)
                .build();
    }
}