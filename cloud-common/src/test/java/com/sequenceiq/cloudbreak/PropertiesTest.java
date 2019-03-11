package com.sequenceiq.cloudbreak;

import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.PropertiesTest.TestAppContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestAppContext.class)
public class PropertiesTest {

    @Value("${cb.enabledplatforms}")
    private String enabledPlatforms;

    @Test
    public void testEnabledPlatforms() {
        String[] platforms = enabledPlatforms.split(",");

        assertThat(platforms, arrayContainingInAnyOrder("AWS", "GCP", "OPENSTACK", "AZURE"));
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
