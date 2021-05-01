package com.sequenceiq.it.config;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.randname.RandomNameGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.util.StringUtils;
import org.testng.TestNG;

import com.sequenceiq.it.TestParameter;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.reporters.InMemoryReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sampler;

@Configuration
@ComponentScan(basePackages = { "com.sequenceiq.it", "com.sequenceiq.cloudbreak.cloud.gcp.util" })
@EnableConfigurationProperties
@EnableRetry
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class IntegrationTestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTestConfiguration.class);

    @Value("${mock.infrastructure.host:localhost}")
    private String mockInfrastructureHost;

    @Bean
    public static PropertyResourceConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public TestNG testNG() {
        return new TestNG();
    }

    @Bean
    public RandomNameGenerator nameGenerator() {
        int seed = (int) System.currentTimeMillis();
        LOGGER.info("Created random name generator with Seed: {}", seed);
        return new RandomNameGenerator(seed);
    }

    @Bean
    public TestParameter testParameter(Environment environment) {
        TestParameter testParameter = new TestParameter();
        LOGGER.info("Application.yml based parameters ::: ");
        testParameter.putAll(getAllKnownProperties(environment));
        return testParameter;
    }

    @Bean
    public io.opentracing.Tracer jaegerTracer() {
        // can't turn of jaeger any other way, skipping autoconfiguration does not work
        final Reporter reporter = new InMemoryReporter();
        final Sampler sampler = new ConstSampler(false);
        return new JaegerTracer.Builder("untraced-service")
                .withReporter(reporter)
                .withSampler(sampler)
                .build();
    }

    private Map<String, String> getAllKnownProperties(Environment env) {
        Map<String, String> rtn = new HashMap<>();
        if (env instanceof ConfigurableEnvironment) {
            for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource) {
                    LOGGER.info("processing property source ::: " + propertySource.getName());
                    for (String key : ((EnumerablePropertySource) propertySource).getPropertyNames()) {
                        String value = propertySource.getProperty(key).toString();
                        LOGGER.debug("{} = {}", key, value);
                        if (!StringUtils.isEmpty(value) && !rtn.containsKey(key)) {
                            rtn.put(key, propertySource.getProperty(key).toString());
                        }
                    }
                }
            }
        }
        return rtn;
    }
}
