package com.sequenceiq.cloudbreak.tomcat;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.concurrent.CommonExecutorServiceFactory;
import com.sequenceiq.cloudbreak.concurrent.ConcurrencyLimitDecorator;

@Configuration
public class TomcatConfig {

    @Value("${server.tomcat.threads.max:200}")
    private int maximumTomcatThreads;

    @Inject
    private CommonExecutorServiceFactory commonExecutorServiceFactory;

    @Bean
    @ConditionalOnProperty(prefix = "spring.threads.virtual", name = "enabled", havingValue = "true")
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setTomcatProtocolHandlerCustomizers(Lists.newArrayList(protocolHandler -> protocolHandler.setExecutor(
                commonExecutorServiceFactory.newVirtualThreadExecutorService("tomcat", "tomcat",
                        List.of(new ConcurrencyLimitDecorator(maximumTomcatThreads))))));
    }
}
