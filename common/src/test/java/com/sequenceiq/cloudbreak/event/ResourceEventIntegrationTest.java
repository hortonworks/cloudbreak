package com.sequenceiq.cloudbreak.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ResourceEventIntegrationTest.TestAppContext.class)
public class ResourceEventIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceEventIntegrationTest.class);

    @Named("resourceEventIntegrationTestMessageSource")
    @Inject
    private MessageSource messageSource;

    @ParameterizedTest(name = "underTest={0}")
    @EnumSource(ResourceEvent.class)
    void getMessageTest(ResourceEvent underTest) {
        String key = underTest.getMessage();

        String message = messageSource.getMessage(key, null, Locale.getDefault());

        assertThat(message).isNotBlank();
        LOGGER.info("ResourceEvent.{} message validation success: {}={}", underTest.name(), key, message);
    }

    @Configuration
    @EnableConfigurationProperties
    static class TestAppContext {

        @Bean(name = "resourceEventIntegrationTestMessageSource")
        public MessageSource messageSource() {
            ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
            messageSource.setBasename("messages/messages");
            return messageSource;
        }

    }

}
