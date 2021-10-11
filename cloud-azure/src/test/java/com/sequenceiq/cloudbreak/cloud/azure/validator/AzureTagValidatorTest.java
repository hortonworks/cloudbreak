package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.conf.AzureConfig;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(SpringExtension.class)
class AzureTagValidatorTest {

    @Inject
    private AzureTagValidator tagValidatorUnderTest;

    @TestFactory
    Collection<DynamicTest> testFactoryOnTagValidator() {
        return Arrays.asList(
                // TAG KEY
                // min.length
                DynamicTest.dynamicTest("tag key is too short",
                        () -> testNegative("", "azure@cloudera.com prx:pfx:^!=-", "too short")),
                // max.length
                DynamicTest.dynamicTest("tag key is too long",
                        () -> testNegative(generateLongString(513), "azure@cloudera.com prx:pfx:^!=-", "too long")),
                // key.validator
                //    prefix
                DynamicTest.dynamicTest("tag key starts with restricted prefix, regular expression is printed",
                        () -> testNegative("azureprefix", "azure@cloudera.com prx:pfx:^!=-", "regular expression")),
                DynamicTest.dynamicTest("tag key starts with restricted prefix, regular expression is printed",
                        () -> testNegative("microsoftprefix", "azure@cloudera.com prx:pfx:^!=-", "regular expression")),
                DynamicTest.dynamicTest("tag key starts with restricted prefix, regular expression is printed",
                        () -> testNegative("windowsprefix", "azure@cloudera.com prx:pfx:^!=-", "regular expression")),
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
                DynamicTest.dynamicTest("tag key contains '/', regular expression is printed",
                        () -> testNegative("test/", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '\\', regular expression is printed",
                        () -> testNegative("test\\", "test", "regular expression")),
                DynamicTest.dynamicTest("tag key contains '?', regular expression is printed",
                        () -> testNegative("test?", "test", "regular expression")),
                //    valid
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("azprefix prx:pfx:^!=-", "azure@cloudera.com prx:pfx:^!=-")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("cod_database_name", "appletree")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("cod_database_crn", "appletree")),

                // TAG VALUE
                // min.length
                DynamicTest.dynamicTest("tag value is too short ",
                        () -> testNegative("test", "", "too short")),
                // max.length
                DynamicTest.dynamicTest("tag value is too long",
                        () -> testNegative("test", generateLongString(257), "too long")),
                // key.validator
                //   spaces
                DynamicTest.dynamicTest("tag value starts with space, regular expression is printed",
                        () -> testNegative("test", " startswithspace", "regular expression")),
                DynamicTest.dynamicTest("tag value ends with space, regular expression is printed",
                        () -> testNegative("test", "endswithspace ", "regular expression")),
                //    valid
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("test", "test")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive("test", "azure@cloudera.com prx:pfx:^!=-")),
                DynamicTest.dynamicTest("tag key is valid ",
                        () -> testPositive(
                                "test",
                                "crn:altus:iam:us-west-1:ab1abcd-abcd-abcd-abcf-abcd1234abcd:machineUser:test/abcd1234-1234-abcd-abcd-abcd1234abcd")
                )
        );
    }

    public String generateLongString(int length) {
        return "a".repeat(length);
    }

    public void testNegative(String tag, String value, String messagePortion) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        Assertions.assertTrue(result.hasError(), "tag validation should fail");
        Assertions.assertTrue(result.getErrors().size() == 1, "tag validation should have one error only");
        Assertions.assertTrue(result.getErrors().get(0).contains(messagePortion));
    }

    public void testPositive(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(Map.of(tag, value));
        Assertions.assertFalse(result.hasError(), "tag validation should pass");
    }

    @Configuration
    @Import({CloudPlatformConnectors.class,
            AzureConfig.class,
            AzureTagValidator.class,
            AzurePlatformParameters.class
    })
    static class Config {

        @Inject
        AzureTagValidator azureTagValidator;

        @Bean
        CloudConnector<Object> cloud() {
            PlatformParameters parameter = parameters();
            CloudConnector mock = Mockito.mock(CloudConnector.class);
            when(mock.parameters()).thenReturn(parameter);
            when(mock.platform()).thenReturn(Platform.platform(CloudConstants.AZURE));
            when(mock.variant()).thenReturn(Variant.variant(CloudConstants.AZURE));
            return mock;
        }

        @Bean
        PlatformParameters parameters() {
            PlatformParameters mock = Mockito.mock(PlatformParameters.class);
            when(mock.tagValidator()).thenReturn(azureTagValidator);
            return mock;
        }
    }
}