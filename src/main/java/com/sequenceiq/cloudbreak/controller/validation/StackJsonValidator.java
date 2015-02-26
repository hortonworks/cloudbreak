package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.StackJson;

@Component
public class StackJsonValidator implements ConstraintValidator<ValidStackRequest, StackJson> {

    @Autowired
    private ParametersRegexValidator regexValidator;

    private List<TemplateParam> stackParams = new ArrayList<>();
    private List<String> allowedParams = new ArrayList<>();

    @Override
    public void initialize(ValidStackRequest constraintAnnotation) {
        for (StackParam param : StackParam.values()) {
            stackParams.add(param);
            allowedParams.add(param.getName());
        }
    }

    @Override
    public boolean isValid(StackJson json, ConstraintValidatorContext context) {
        for (String key : json.getParameters().keySet()) {
            if (!allowedParams.contains(key)) {
                context.buildConstraintViolationWithTemplate("Parameter is not allowed. Valid parameters are " + allowedParams)
                        .addPropertyNode("parameters")
                        .addBeanNode().inIterable().atKey(key)
                        .addConstraintViolation();
                return false;
            }
        }
        Map<String, Object> params = new HashMap<>();
        params.putAll(json.getParameters());
        if (!regexValidator.validate(params, context, stackParams)) {
            return false;
        }
        return true;
    }
}
