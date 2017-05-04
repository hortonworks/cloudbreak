package com.sequenceiq.cloudbreak.cloud.azure.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Configuration
public class AzureConfig {

    @Value("${cb.azure.tag.amount:15}")
    private Integer maxAmount;

    @Value("${cb.azure.tag.key.length:512}")
    private Integer keyLength;

    @Value("${cb.azure.tag.key.validator:^(?!microsoft|azure|windows)((?!,).)*$}")
    private String keyValidator;

    @Value("${cb.azure.tag.value.length:256}")
    private Integer valueLength;

    @Value("${cb.azure.tag.value.validator:^(?!microsoft|azure|windows)((?!,).)*$}")
    private String valueValidator;

    @Bean(name = "AzureTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, keyLength, keyValidator, valueLength, valueValidator);
    }
}
