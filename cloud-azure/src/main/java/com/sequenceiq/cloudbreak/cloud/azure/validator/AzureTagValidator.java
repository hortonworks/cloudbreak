package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CommonTagValidator;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePlatformParameters;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class AzureTagValidator extends CommonTagValidator {

    @Inject
    private AzurePlatformParameters platformParameters;

    private Pattern keyValidator;

    private Pattern valueValidator;

    @PostConstruct
    public void init() {
        keyValidator = Pattern.compile(platformParameters.tagSpecification().getKeyValidator());
        valueValidator = Pattern.compile(platformParameters.tagSpecification().getValueValidator());
    }

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        validate(platformParameters.tagSpecification(), cloudStack.getTags());
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
