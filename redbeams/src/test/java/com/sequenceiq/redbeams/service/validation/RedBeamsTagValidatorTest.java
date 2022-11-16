package com.sequenceiq.redbeams.service.validation;

import static org.mockito.Mockito.when;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.conf.AzureConfig;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureTagValidator;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(SpringExtension.class)
public class RedBeamsTagValidatorTest {

    @Inject
    private RedBeamsTagValidator underTest;

    @ParameterizedTest
    @MethodSource("negativeScenarios")
    public void testNegative(String tag, String value, String messagePortion) {
        ValidationResult result = underTest.validateTags(CloudConstants.AZURE, Map.of(tag, value));
        Assertions.assertTrue(result.hasError(), "tag validation should fail");
        Assertions.assertEquals(1, result.getErrors().size(), "tag validation should have one error only");
        Assertions.assertTrue(result.getErrors().get(0).contains(messagePortion));
    }

    @ParameterizedTest
    @MethodSource("positiveScenarios")
    public void testPositive(String tag, String value) {
        ValidationResult result = underTest.validateTags(CloudConstants.AZURE, Map.of(tag, value));
        Assertions.assertFalse(result.hasError(), "tag validation should pass");
    }

    public static Object[][] negativeScenarios() {
        return new Object[][] {

                { "",            "azure@cloudera.com prx:pfx:^!=-",  "too short"},
                {"azureprefix",  "azure@cloudera.com prx:pfx:^!=-",  "regular expression"},
                {"database_name", "",                                "too short"}
        };
    }

    public static Object[][] positiveScenarios() {
        return new Object[][] {

                { "azaccount",          "azure@cloudera.com prx:pfx:^!=-"},
                {"cod_database_name",   "hura"},
                {"cod_database_crn",    "hura"}
        };
    }

    @Configuration
    @Import({RedBeamsTagValidator.class,
            CloudPlatformConnectors.class,
            AzureConfig.class,
            AzureTagValidator.class
    })
    static class Config {

        @Inject
        AzureTagValidator azureTagValidator;

        @Bean
        CloudConnector cloud() {
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
