package com.sequenceiq.environment.environment.validation.validators;

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
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureTagValidator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(SpringExtension.class)
class TagValidatorTest {

    public static final String AZURE = "AZURE";

    public static final String BAD_KEY = "azureprefix";

    public static final String GOOD_VALUE = "azure@cloudera.com prx:pfx:<>&^%";

    public static final String GOOD_KEY = "azprefix";

    @Inject
    private TagValidator tagValidatorUnderTest;

    @TestFactory
    Collection<DynamicTest> testFactoryOnTagValidator() {
        return Arrays.asList(
                DynamicTest.dynamicTest("tag key is invalid",
                        () -> testNegative(BAD_KEY, GOOD_VALUE, "tag names are not well")),
                DynamicTest.dynamicTest("tag key is invalid, regular expression is printed",
                        () -> testNegative(BAD_KEY, GOOD_VALUE, "regular expression")),
                DynamicTest.dynamicTest("tag value is valid ",
                        () -> testPositive(GOOD_KEY, GOOD_VALUE)));
    }

    public void testNegative(String tag, String value, String messagePortion) {
        ValidationResult result = tagValidatorUnderTest.validateTags(AZURE, Map.of(tag, value));
        Assertions.assertTrue(result.hasError(), "tag validation should fail");
        Assertions.assertTrue(result.getErrors().size() == 1, "tag validation should have one error only");
        Assertions.assertTrue(result.getErrors().get(0).contains(messagePortion));
    }

    public void testPositive(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(AZURE, Map.of(tag, value));
        Assertions.assertFalse(result.hasError(), "tag validation should pass");
    }

    @Configuration
    @Import({TagValidator.class,
            CloudPlatformConnectors.class,
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
            when(mock.platform()).thenReturn(Platform.platform(AZURE));
            when(mock.variant()).thenReturn(Variant.variant(AZURE));
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