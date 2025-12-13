package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

import io.micrometer.core.instrument.MeterRegistry;

@ExtendWith(SpringExtension.class)
class TagValidatorTest {

    @Inject
    private TagValidator tagValidatorUnderTest;

    @MockBean(name = "azureClientThreadPool")
    private ExecutorService executorServicel;

    @TestFactory
    Collection<DynamicTest> testFactoryOnTagValidator() {
        return Arrays.asList(
                DynamicTest.dynamicTest("tag key is invalid",
                        () -> testNegative("", "azure@cloudera.com prx:pfx:^!=-", "too short")),
                DynamicTest.dynamicTest("tag key is invalid, regular expression is printed",
                        () -> testNegative("azureprefix", "azure@cloudera.com prx:pfx:^!=-", "regular expression")),
                DynamicTest.dynamicTest("tag value is valid ",
                        () -> testPositive("azprefix", "azure@cloudera.com prx:pfx:^!=-")),
                DynamicTest.dynamicTest("tag value is valid ",
                        () -> testPositive("cod_database_name", "appletree")),
                DynamicTest.dynamicTest("tag value is valid ",
                        () -> testPositive("cod_database_crn", "appletree"))
        );
    }

    public void testNegative(String tag, String value, String messagePortion) {
        ValidationResult result = tagValidatorUnderTest.validateTags(CloudConstants.AZURE, Map.of(tag, value));
        assertTrue(result.hasError(), "tag validation should fail");
        assertEquals(1, result.getErrors().size(), "tag validation should have one error only");
        assertTrue(result.getErrors().get(0).contains(messagePortion));
    }

    public void testPositive(String tag, String value) {
        ValidationResult result = tagValidatorUnderTest.validateTags(CloudConstants.AZURE, Map.of(tag, value));
        assertFalse(result.hasError(), "tag validation should pass");
    }

    @Configuration
    @Import({TagValidator.class,
            CloudPlatformConnectors.class,
            AzureConfig.class,
            AzureTagValidator.class
    })
    static class Config {

        @Inject
        AzureTagValidator azureTagValidator;

        @MockBean
        private MeterRegistry meterRegistry;

        @Bean
        CloudConnector cloud() {
            PlatformParameters parameter = parameters();
            CloudConnector mock = mock(CloudConnector.class);
            when(mock.parameters()).thenReturn(parameter);
            when(mock.platform()).thenReturn(Platform.platform(CloudConstants.AZURE));
            when(mock.variant()).thenReturn(Variant.variant(CloudConstants.AZURE));
            return mock;
        }

        @Bean
        PlatformParameters parameters() {
            PlatformParameters mock = mock(PlatformParameters.class);
            when(mock.tagValidator()).thenReturn(azureTagValidator);
            return mock;
        }

        @Bean
        public CommonExecutorServiceFactory commonExecutorServiceFactory() {
            CommonExecutorServiceFactory commonExecutorServiceFactory = mock(CommonExecutorServiceFactory.class);
            when(commonExecutorServiceFactory.newThreadPoolExecutorService(any(), any(), anyInt(), anyInt(), anyLong(), any(), any(), any(), any())).thenReturn(
                    Executors.newCachedThreadPool());
            return commonExecutorServiceFactory;
        }
    }
}
