package com.sequenceiq.cloudbreak.cloud.openstack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class OpenStackConfig {

    @Value("${cb.openstack.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.openstack.tag.key.min.length:1}")
    private Integer minKeyLength;

    @Value("${cb.openstack.tag.key.max.length:127}")
    private Integer maxKeyLength;

    @Value("${cb.openstack.tag.key.validator:^(?!\\s)([\\w\\d\\-=._:/@\\s]+)(?<!\\s)$}")
    private String keyValidator;

    @Value("${cb.openstack.tag.value.min.length:1}")
    private Integer minValueLength;

    @Value("${cb.openstack.tag.value.max.length:255}")
    private Integer maxValueLength;

    @Value("${cb.openstack.tag.value.validator:^(?!\\s)([\\w\\d\\-=._:/@\\s]+)(?<!\\s)$}")
    private String valueValidator;

    @Bean(name = "OpenStackTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, minKeyLength, maxKeyLength, keyValidator, minValueLength, maxValueLength, valueValidator);
    }
}
