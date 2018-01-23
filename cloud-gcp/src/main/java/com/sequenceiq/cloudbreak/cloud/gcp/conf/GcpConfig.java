package com.sequenceiq.cloudbreak.cloud.gcp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class GcpConfig {

    @Value("${cb.gcp.tag.amount:64}")
    private Integer maxAmount;

    @Value("${cb.gcp.tag.key.length:63}")
    private Integer keyLength;

    @Value("${cb.gcp.tag.key.validator:^([a-z]+)([a-z\\d-]+)$}")
    private String keyValidator;

    @Value("${cb.gcp.tag.value.length:63}")
    private Integer valueLength;

    @Value("${cb.gcp.tag.value.validator:^([a-z0-9]+)([a-z\\d-]+)$}")
    private String valueValidator;

    @Bean(name = "GcpTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, keyLength, keyValidator, valueLength, valueValidator);
    }
}
