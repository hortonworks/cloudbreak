package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CommonTagValidator;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Component
public class AwsTagValidator extends CommonTagValidator {

    @Inject
    private AwsPlatformParameters platformParameters;

    private Pattern keyValidator;

    private Pattern valueValidator;

    @PostConstruct
    public void init() {
        keyValidator = Pattern.compile(platformParameters.tagSpecification().getKeyValidator());
        valueValidator = Pattern.compile(platformParameters.tagSpecification().getValueValidator());
    }

    @Override
    public TagSpecification getTagSpecification() {
        return platformParameters.tagSpecification();
    }

    @Override
    protected Pattern getKeyValidator() {
        return keyValidator;
    }

    @Override
    protected Pattern getValueValidator() {
        return valueValidator;
    }
}
