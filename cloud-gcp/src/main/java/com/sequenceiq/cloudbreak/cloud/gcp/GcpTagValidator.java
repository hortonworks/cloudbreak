package com.sequenceiq.cloudbreak.cloud.gcp;

import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CommonTagValidator;
import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@Component
public class GcpTagValidator extends CommonTagValidator implements TagValidator {

    @Inject
    private GcpPlatformParameters platformParameters;

    @Inject
    private GcpLabelUtil gcpLabelUtil;

    private Pattern keyValidator;

    private Pattern valueValidator;

    @PostConstruct
    public void init() {
        keyValidator = Pattern.compile(platformParameters.tagSpecification().getKeyValidator());
        valueValidator = Pattern.compile(platformParameters.tagSpecification().getValueValidator());
    }

    @Override
    public TagSpecification getTagSpecification() {
        return  platformParameters.tagSpecification();
    }

    @Override
    protected Pattern getKeyValidator() {
        return keyValidator;
    }

    @Override
    protected Pattern getValueValidator() {
        return valueValidator;
    }

    @Override
    protected String transform(String tag) {
        return gcpLabelUtil.transformLabelKeyOrValue(tag);
    }
}
