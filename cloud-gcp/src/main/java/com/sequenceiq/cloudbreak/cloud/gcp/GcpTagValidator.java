package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CommonTagValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Component
public class GcpTagValidator extends CommonTagValidator {

    @Inject
    private GcpPlatformParameters platformParameters;

    private Pattern keyValidator;

    private Pattern valueValidator;

    @PostConstruct
    public void init() {
        keyValidator = Pattern.compile(platformParameters.tagSpecification().getKeyValidator());
        valueValidator = Pattern.compile(platformParameters.tagSpecification().getValueValidator());
    }

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        Map<String, String> labels = GcpLabelUtil.createLabelsFromTags(cloudStack);
        validate(platformParameters.tagSpecification(), labels);
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
