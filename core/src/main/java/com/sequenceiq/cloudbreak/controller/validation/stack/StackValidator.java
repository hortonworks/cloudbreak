package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.service.stack.StackParameterService;

@Component
public class StackValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackValidator.class);

    @Inject
    private List<ParameterValidator> parameterValidators;

    @Inject
    private StackParameterService stackParameterService;

    public void validate(Map<String, String> params, List<StackParamValidation> stackParamValidations) {
        if (params != null && !params.isEmpty()) {
            for (ParameterValidator parameterValidator : parameterValidators) {
                parameterValidator.validate(params, stackParamValidations);
            }
        }
    }

}

