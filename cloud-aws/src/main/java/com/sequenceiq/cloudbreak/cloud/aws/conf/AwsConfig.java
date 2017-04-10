package com.sequenceiq.cloudbreak.cloud.aws.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class AwsConfig {

    @Value("${cb.aws.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.aws.tag.key.length:127}")
    private Integer keyLength;

    @Value("${cb.aws.tag.key.validator:^(?!aws)([\\w\\d+-=._:/@\\s]+)$}")
    private String keyValidator;

    @Value("${cb.aws.tag.value.length:255}")
    private Integer valueLength;

    @Value("${cb.aws.tag.value.validator:^(?!aws)([\\w\\d+-=._:/@\\s]+)$}")
    private String valueValidator;

    @Bean(name = "AwsTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, keyLength, keyValidator, valueLength, valueValidator);
    }
}
