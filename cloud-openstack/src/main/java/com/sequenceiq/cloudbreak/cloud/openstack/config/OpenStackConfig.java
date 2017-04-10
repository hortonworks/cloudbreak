package com.sequenceiq.cloudbreak.cloud.openstack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class OpenStackConfig {

    @Value("${cb.openstack.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.openstack.tag.key.length:127}")
    private Integer keyLength;

    @Value("${cb.openstack.tag.key.validator:^([\\w\\d+-=._:/@\\s]+)$}")
    private String keyValidator;

    @Value("${cb.openstack.tag.value.length:255}")
    private Integer valueLength;

    @Value("${cb.openstack.tag.value.validator:^([\\w\\d+-=._:/@\\s]+)$}")
    private String valueValidator;

    @Bean(name = "OpenStackTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, keyLength, keyValidator, valueLength, valueValidator);
    }
}
