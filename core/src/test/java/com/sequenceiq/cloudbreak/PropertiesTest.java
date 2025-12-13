package com.sequenceiq.cloudbreak;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;

import com.sequenceiq.cloudbreak.PropertiesTest.TestAppContext;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = TestAppContext.class)
class PropertiesTest {

    @Value("${cb.enabledplatforms}")
    private String enabledPlatforms;

    @Test
    void testEnabledPlatforms() {
        String[] platforms = enabledPlatforms.split(",");

        assertThat(platforms, Matchers.arrayContainingInAnyOrder("AWS", "GCP", "AZURE", "MOCK", "YARN"));
    }

    @Configuration
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    value = {}
            ))
    @PropertySource("classpath:application.yml")
    static class TestAppContext {
        @Value("cb.enabledplatforms")
        private String enabledPlatforms;

        public String getVar() {
            return enabledPlatforms;
        }
    }
}
