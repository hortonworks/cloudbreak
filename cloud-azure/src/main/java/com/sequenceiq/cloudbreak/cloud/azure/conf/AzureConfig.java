package com.sequenceiq.cloudbreak.cloud.azure.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class AzureConfig {

    @Value("${cb.azure.tag.amount:50}")
    private Integer maxAmount;

    @Value("${cb.azure.tag.key.min.length:1}")
    private Integer minKeyLength;

    @Value("${cb.azure.tag.key.max.length:512}")
    private Integer maxKeyLength;

    @Value("${cb.azure.tag.key.validator:^(?!microsoft|azure|windows|\\s)[^,<>%&\\\\/\\?]*(?<!\\s)$}")
    private String keyValidator;

    @Value("${cb.azure.tag.value.min.length:1}")
    private Integer minValueLength;

    @Value("${cb.azure.tag.value.max.length:256}")
    private Integer maxValueLength;

    @Value("${cb.azure.tag.value.validator:^(?!\\s).*(?<!\\s)$}")
    private String valueValidator;

    @Bean(name = "AzureTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, minKeyLength, maxKeyLength, keyValidator, minValueLength, maxValueLength, valueValidator);
    }
}
