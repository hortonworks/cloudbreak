package com.sequenceiq.cloudbreak.cloud.yarn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class YarnConfig {
    // TODO: These values are taken from OpenStackConfig.
    //   Need to be double-checked for Y-cloud.
    @Value("${cb.yarn.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.yarn.tag.key.length:127}")
    private Integer keyLength;

    @Value("${cb.yarn.tag.key.validator:^([\\w\\d+-=._:/@\\s]+)$}")
    private String keyValidator;

    @Value("${cb.yarn.tag.value.length:255}")
    private Integer valueLength;

    @Value("${cb.yarn.tag.value.validator:^([\\w\\d+-=._:/@\\s]+)$}")
    private String valueValidator;

    @Bean(name = "YarnTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, keyLength, keyValidator, valueLength, valueValidator);
    }
}
