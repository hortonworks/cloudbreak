package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
@EnableRetry
public class GcpConfig {

    @Value("${cb.gcp.tag.amount:64}")
    private Integer maxAmount;

    @Value("${cb.gcp.tag.key.min.length:1}")
    private Integer minKeyLength;

    @Value("${cb.gcp.tag.key.max.length:63}")
    private Integer maxKeyLength;

    @Value("${cb.gcp.tag.key.validator:^([a-z]+)([a-z\\d-_]+)$}")
    private String keyValidator;

    @Value("${cb.gcp.tag.value.min.length:1}")
    private Integer minValueLength;

    @Value("${cb.gcp.tag.value.max.length:63}")
    private Integer maxValueLength;

    @Value("${cb.gcp.tag.value.validator:^([a-z0-9]+)([a-z\\d-_]+)$}")
    private String valueValidator;

    @Bean(name = "GcpTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, minKeyLength, maxKeyLength, keyValidator, minValueLength, maxValueLength, valueValidator);
    }
}
