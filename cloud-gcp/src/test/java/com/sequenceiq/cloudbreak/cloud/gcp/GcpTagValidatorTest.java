package com.sequenceiq.cloudbreak.cloud.gcp;

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
import com.sequenceiq.cloudbreak.cloud.gcp.conf.GcpConfig;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(SpringExtension.class)
class GcpTagValidatorTest {

    @Inject
    private GcpTagValidator tagValidatorUnderTest;

    @TestFactory
    Collection<DynamicTest> testFactoryOnTagValidator() {
        return Arrays.asList(
                // TAG KEY
                // min.length
                DynamicTest.dynamicTest("tag key is too short",
                        () -> testEmptyTagKey("", "test")),
                // max.length
                // NOTE: anything above 63 is truncated by GcpLabelUtil
                DynamicTest.dynamicTest("tag key is too long",
                        () -> testPositive(generateLongString(64), "test")),
                // key.validator
                //    prefix
                // NOTE: GcpLabelUtil converts it to lower-case
                DynamicTest.dynamicTest("tag key starts with upper-case, regular expression is printed",
                        () -> testPositive("Atest", "test")),
                DynamicTest.dynamicTest("tag key starts with number, regular expression is printed",
                        () -> testNegative("1test", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key starts with special char, regular expression is printed",
                        () -> testNegative("-test", "test", "regular expression")),
                //   spaces
                DynamicTest.dynamicTest("tag key starts with space, regular expression is printed",
                        () -> testNegative(" startswithspace", "test", "regular expression")),
                // NOTE: GcpLabelUtil converts spaces to hyphens
                DynamicTest.dynamicTest("tag key ends with space, regular expression is printed",
                        () -> testPositive("endswithspace ", "test")),
                //    non-word characters
                // NOTE: GcpLabelUtil removes any non-word characters (\w)
                DynamicTest.dynamicTest("tag key contains ',', it gets removed",
                        () -> testPositive("test,", "test")),
                DynamicTest.dynamicTest("tag key contains '<', it gets removed",
                        () -> testPositive("test<", "test")),
                DynamicTest.dynamicTest("tag key contains '>', it gets removed",
                        () -> testPositive("test>", "test")),
                DynamicTest.dynamicTest("tag key contains '%', it gets removed",
                        () -> testPositive("test%", "test")),
                DynamicTest.dynamicTest("tag key contains '&', it gets removed",
                        () -> testPositive("test&", "test")),
                DynamicTest.dynamicTest("tag key contains '/', it gets removed",
                        () -> testPositive("test/", "test")),
                DynamicTest.dynamicTest("tag key contains '\\', it gets removed",
                        () -> testPositive("test\\", "test")),
                DynamicTest.dynamicTest("tag key contains '?', it gets removed",
                        () -> testPositive("test?", "test")),
                //    valid
                DynamicTest.dynamicTest("tag key is valid",
                        () -> testPositive("gcp prefix-_", "test")),
                DynamicTest.dynamicTest("tag key is valid",
                        () -> testPositive("cod_database_name", "appletree")),
                DynamicTest.dynamicTest("tag key is valid",
                        () -> testPositive("cod_database_crn", "appletree")),

                // TAG VALUE
                // min.length
                DynamicTest.dynamicTest("empty tag value is allowed",
                        () -> testPositive("test", "")),
                // max.length
                // NOTE: anything above 63 is truncated by GcpLabelUtil
                DynamicTest.dynamicTest("tag value is too long, gets truncated",
                        () -> testPositive("test", generateLongString(64))),
                // key.validator
                //   spaces
                // NOTE: GcpLabelUtil converts spaces to hyphens
                DynamicTest.dynamicTest("tag value starts with space, gets repaced",
                        () -> testPositive("test", " startswithspace")),
                // NOTE: GcpLabelUtil converts spaces to hyphens
                DynamicTest.dynamicTest("tag value ends with space, gets replaced",
                        () -> testPositive("test", "endswithspace ")),
                //   non-allowed: , < > % & \ / ?
                DynamicTest.dynamicTest("tag value contains ',', it gets removed",
                        () -> testPositive("test", "test,")),
                DynamicTest.dynamicTest("tag value contains '<', it gets removed",
                        () -> testPositive("test", "test<")),
                DynamicTest.dynamicTest("tag value contains '>', it gets removed",
                        () -> testPositive("test", "test>")),
                DynamicTest.dynamicTest("tag value contains '%', it gets removed",
                        () -> testPositive("test", "test%")),
                DynamicTest.dynamicTest("tag value contains '&', it gets removed",
                        () -> testPositive("test", "test&")),
                DynamicTest.dynamicTest("tag value contains '/', it gets removed",
                        () -> testPositive("test", "test/")),
                DynamicTest.dynamicTest("tag value contains '\\', it gets removed",
                        () -> testPositive("test", "test\\")),
                DynamicTest.dynamicTest("tag value contains '?', it gets removed",
                        () -> testPositive("test", "test?")),
                //    valid
                DynamicTest.dynamicTest("tag key is valid",
                        () -> testPositive("test", "test-_")),
                DynamicTest.dynamicTest("tag key is valid",
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

    public void testPositive(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        assertFalse(result.hasError(), "tag validation should pass");
    }

    public void testEmptyTagKey(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        assertTrue(result.hasError(), "tag validation should fail");
        assertTrue(result.getErrors().size() == 2, "tag validation should have one error only");
        assertTrue(result.getErrors().get(0).contains("not well formatted"));
        assertTrue(result.getErrors().get(1).contains("too short"));
    }

    @Configuration
    @Import({CloudPlatformConnectors.class,
            GcpConfig.class,
            GcpTagValidator.class,
            GcpPlatformParameters.class,
            CloudbreakResourceReaderService.class,
            GcpLabelUtil.class
    })
    static class Config {

        @Inject
        GcpTagValidator gcpTagValidator;

        @Bean
        CloudConnector cloud() {
            PlatformParameters parameter = parameters();
            CloudConnector mock = mock(CloudConnector.class);
            when(mock.parameters()).thenReturn(parameter);
            when(mock.platform()).thenReturn(Platform.platform(CloudConstants.GCP));
            when(mock.variant()).thenReturn(Variant.variant(CloudConstants.GCP));
            return mock;
        }

        @Bean
        PlatformParameters parameters() {
            PlatformParameters mock = mock(PlatformParameters.class);
            when(mock.tagValidator()).thenReturn(gcpTagValidator);
            return mock;
        }
    }

}
