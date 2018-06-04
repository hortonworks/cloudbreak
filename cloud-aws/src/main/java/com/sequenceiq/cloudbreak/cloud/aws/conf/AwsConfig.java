package com.sequenceiq.cloudbreak.cloud.aws.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class AwsConfig {

    @Value("${cb.aws.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.aws.tag.key.min.length:1}")
    private Integer minKeyLength;

    @Value("${cb.aws.tag.key.max.length:127}")
    private Integer maxKeyLength;

    @Value("${cb.aws.tag.key.validator:^(?!aws)([\\w\\d+-=._:/@\\s]+)$}")
    private String keyValidator;

    @Value("${cb.aws.tag.value.min.length:1}")
    private Integer minValueLength;

    @Value("${cb.aws.tag.value.max.length:255}")
    private Integer maxValueLength;

    @Value("${cb.aws.tag.value.validator:^(?!aws)([\\w\\d+-=._:/@\\s]+)$}")
    private String valueValidator;

    @Bean(name = "AwsTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, minKeyLength, maxKeyLength, keyValidator, minValueLength, maxValueLength, valueValidator);
    }
}
