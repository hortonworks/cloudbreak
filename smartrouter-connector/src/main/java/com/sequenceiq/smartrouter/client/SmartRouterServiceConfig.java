package com.sequenceiq.smartrouter.client;

import java.time.Duration;

import org.openapitools.jackson.nullable.JsonNullable;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyProxyClientFactory;

@Configuration
public class SmartRouterServiceConfig {

    static final String PROXY_WITH_TIMEOUT_HEADER = "Proxy-With-Timeout";

    @Value("${smartrouter.clusterProxy.serviceName:smart-router}")
    private String clusterProxyServiceName;

    @Value("${smartrouter.connect.timeout.sec:5}")
    private long connectTimeoutSec;

    @Value("${smartrouter.read.timeout.sec:30}")
    private long readTimeoutSec;

    /**
     * Cluster Proxy client factory with the Smart Router timeouts. Cluster Proxy is told the read timeout too, so that it does not cut the call earlier.
     * The bodies are written by {@link #smartRouterObjectMapper()}, which the merge patch requests rely on.
     */
    @Bean
    public ClusterProxyProxyClientFactory smartRouterClusterProxyProxyClientFactory(ClusterProxyConfiguration clusterProxyConfiguration,
            RestTemplateBuilder restTemplateBuilder) {
        Duration readTimeout = Duration.ofSeconds(readTimeoutSec);
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter(smartRouterObjectMapper());
        return new ClusterProxyProxyClientFactory(clusterProxyConfiguration, restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(connectTimeoutSec))
                .readTimeout(readTimeout)
                .defaultHeader(PROXY_WITH_TIMEOUT_HEADER, String.valueOf(readTimeout.toMillis()))
                // replaced in place to keep the converter order, e.g. String bodies are still written as they are
                .additionalCustomizers(restTemplate -> restTemplate.getMessageConverters()
                        .replaceAll(converter -> converter instanceof MappingJackson2HttpMessageConverter ? jsonConverter : converter)));
    }

    /**
     * A merge patch has to tell an untouched field from a cleared one: an unset field, either {@code null} or {@link JsonNullable#undefined()}, is left
     * out of the body, while a nullable field set to {@code null}, i.e. {@code JsonNullable.of(null)}, is sent as {@code null}.
     */
    private static ObjectMapper smartRouterObjectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .modulesToInstall(new JsonNullableModule())
                .build();
    }

    public String getClusterProxyServiceName() {
        return clusterProxyServiceName;
    }
}
