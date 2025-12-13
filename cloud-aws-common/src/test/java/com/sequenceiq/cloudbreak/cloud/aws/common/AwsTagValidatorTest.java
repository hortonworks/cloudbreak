package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.config.AwsConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyMultiplePreferPrivate;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(SpringExtension.class)
class AwsTagValidatorTest {

    @Inject
    private AwsTagValidator tagValidatorUnderTest;

    @TestFactory
    Collection<DynamicTest> testFactoryOnTagValidator() {
        return Arrays.asList(
                // TAG KEY
                // min.length
                DynamicTest.dynamicTest("tag key is too short",
                        () -> testEmptyTagKey("", "test")),
                // max.length
                DynamicTest.dynamicTest("tag key is too long",
                        () -> testNegative(generateLongString(128), "test", "too long")),
                // key.validator
                //    prefix
                DynamicTest.dynamicTest("tag key starts with restricted prefix, regular expression is printed",
                        () -> testNegative("awsprefix", "test", "regular expression")),
                //   spaces
                DynamicTest.dynamicTest("tag key starts with space, regular expression is printed",
                        () -> testNegative(" startswithspace", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key ends with space, regular expression is printed",
                        () -> testNegative("endswithspace ", "test", "regular expression")),
                //   non-allowed: , < > % & \ / ?
                DynamicTest.dynamicTest("tag key contains ',', regular expression is printed",
                        () -> testNegative("test,", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '<', regular expression is printed",
                        () -> testNegative("test<", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '>', regular expression is printed",
                        () -> testNegative("test>", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '%', regular expression is printed",
                        () -> testNegative("test%", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '&', regular expression is printed",
                        () -> testNegative("test&", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '\\', regular expression is printed",
                        () -> testNegative("test\\", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '?', regular expression is printed",
                        () -> testNegative("test?", "test", "regular expression")),
                //    valid
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("test + - = . _ : / @ 1234", "test")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("cod_database_name", "appletree")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("cod_database_crn", "appletree")),

                // TAG VALUE
                // min.length
                DynamicTest.dynamicTest("tag value is too short ",
                        () -> testEmptyTagKey("test", "")),
                // max.length
                DynamicTest.dynamicTest("tag value is too long",
                        () -> testNegative("test", generateLongString(256), "too long")),
                // key.validator
                //   spaces
                DynamicTest.dynamicTest("tag value starts with space, regular expression is printed",
                        () -> testNegative("test", " startswithspace", "regular expression")),
                DynamicTest.dynamicTest("tag value ends with space, regular expression is printed",
                        () -> testNegative("test", "endswithspace ", "regular expression")),
                //   non-allowed: , < > % & \ / ?
                DynamicTest.dynamicTest("tag value contains ',', regular expression is printed",
                        () -> testNegative("test", "test,", "regular expression")),
                DynamicTest.dynamicTest("tag value contains '<', regular expression is printed",
                        () -> testNegative("test", "test<", "regular expression")),
                DynamicTest.dynamicTest("tag value contains '>', regular expression is printed",
                        () -> testNegative("test", "test>", "regular expression")),
                DynamicTest.dynamicTest("tag value contains '%', regular expression is printed",
                        () -> testNegative("test", "test%", "regular expression")),
                DynamicTest.dynamicTest("tag value contains '&', regular expression is printed",
                        () -> testNegative("test", "test&", "regular expression")),
                DynamicTest.dynamicTest("tag value contains '\\', regular expression is printed",
                        () -> testNegative("test", "test\\", "regular expression")),
                DynamicTest.dynamicTest("tag value contains '?', regular expression is printed",
                        () -> testNegative("test", "test?", "regular expression")),
                //    valid
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("test -=._:/@ 1234", "test")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("test", "test"))
        );
    }

    public String generateLongString(int length) {
        return "a".repeat(length);
    }

    public void testNegative(String tag, String value, String messagePortion) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        assertTrue(result.hasError(), "tag validation should fail");
        assertTrue(result.getErrors().size() == 1, "tag validation should have one error only");
        assertTrue(result.getErrors().get(0).contains(messagePortion));
    }

    public void testEmptyTagKey(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        assertTrue(result.hasError(), "tag validation should fail");
        assertTrue(result.getErrors().size() == 2, "tag validation should have one error only");
        assertTrue(result.getErrors().get(0).contains("not well formatted"));
        assertTrue(result.getErrors().get(1).contains("too short"));
    }

    public void testPositive(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        assertFalse(result.hasError(), "tag validation should pass");
    }

    @Configuration
    @Import({CloudPlatformConnectors.class,
            AwsConfig.class,
            AwsTagValidator.class,
            AwsPlatformParameters.class,
            CloudbreakResourceReaderService.class,
            SubnetFilterStrategyMultiplePreferPrivate.class,
            SubnetSelectorService.class
    })
    static class Config {

        @Inject
        AwsTagValidator awsTagValidator;

        @Bean
        CloudConnector cloud() {
            PlatformParameters parameter = parameters();
            CloudConnector mock = mock(CloudConnector.class);
            when(mock.parameters()).thenReturn(parameter);
            when(mock.platform()).thenReturn(Platform.platform(CloudConstants.AWS));
            when(mock.variant()).thenReturn(Variant.variant(CloudConstants.AWS));
            return mock;
        }

        @Bean
        PlatformParameters parameters() {
            PlatformParameters mock = mock(PlatformParameters.class);
            when(mock.tagValidator()).thenReturn(awsTagValidator);
            return mock;
        }
    }
}

