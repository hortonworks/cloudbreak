package com.sequenceiq.cloudbreak.cloud.aws.conf;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetSelectorStrategy;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetSelectorStrategyType;

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

    @Value("${cb.aws.tag.value.validator:^(?!aws|\\s)([\\w\\d+-=._:/@\\s]+)$}")
    private String valueValidator;

    @Inject
    private List<SubnetSelectorStrategy> subnetSelectorStrategies;

    @Bean(name = "AwsTagSpecification")
    public TagSpecification getTagSpecification() {
        return new TagSpecification(maxAmount, minKeyLength, maxKeyLength, keyValidator, minValueLength, maxValueLength, valueValidator);
    }

    @Bean
    public Map<SubnetSelectorStrategyType, SubnetSelectorStrategy> subnetSelectorStrategies() {
        ImmutableMap.Builder<SubnetSelectorStrategyType, SubnetSelectorStrategy> builder = new ImmutableMap.Builder<>();

        for (SubnetSelectorStrategy subnetSelectorStrategy : subnetSelectorStrategies) {
            builder.put(subnetSelectorStrategy.getType(), subnetSelectorStrategy);
        }
        return builder.build();
    }

}
