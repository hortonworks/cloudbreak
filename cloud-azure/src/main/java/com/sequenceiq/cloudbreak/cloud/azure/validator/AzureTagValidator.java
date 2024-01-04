package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CommonTagValidator;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Component
public class AzureTagValidator extends CommonTagValidator {

    @Inject
    @Qualifier("AzureTagSpecification")
    private TagSpecification tagSpecification;

    private Pattern keyValidator;

    private Pattern valueValidator;

    @PostConstruct
    public void init() {
        keyValidator = Pattern.compile(tagSpecification.getKeyValidator());
        valueValidator = Pattern.compile(tagSpecification.getValueValidator());
    }

    @Override
    public TagSpecification getTagSpecification() {
        return tagSpecification;
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
