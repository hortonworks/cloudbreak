package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePlatformParameters;
import com.sequenceiq.cloudbreak.cloud.azure.conf.AzureConfig;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@ExtendWith(SpringExtension.class)
class AzureTagValidatorTest {

    public static final String VALID_KEY = "notstartwithazure";

    public static final String VALID_VALUE = "normal";

    public static final String BAD_KEY = "azure";

    public static final String BAD_VALUE = ".";

    @Inject
    AzureTagValidator azureTagValidatorUnderTest;

    @Test
    public void testExceptionShouldBeRaisedWhenValidationHasError() {
        CloudStack cs = new CloudStack(List.of(), null, null, Map.of(), Map.of(BAD_KEY, BAD_VALUE), null, null, null, null, null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> azureTagValidatorUnderTest.validate(null, cs));
    }

    @Test
    public void testValidationShouldPass() {
        CloudStack cs = new CloudStack(List.of(), null, null, Map.of(), Map.of(VALID_KEY, VALID_VALUE), null, null, null, null, null);
        Assertions.assertDoesNotThrow(() -> azureTagValidatorUnderTest.validate(null, cs));
    }

    @Configuration
    @Import({
            AzureTagValidator.class,
            AzureConfig.class,
            AzurePlatformParameters.class
    })
    static class Config {

    }

}