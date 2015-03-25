package com.sequenceiq.cloudbreak.controller.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.controller.json.StackRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StackJsonValidator implements ConstraintValidator<ValidStackRequest, StackRequest> {

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
    public boolean isValid(StackRequest json, ConstraintValidatorContext context) {
        for (String key : json.getParameters().keySet()) {
            if (!allowedParams.contains(key)) {
                context.buildConstraintViolationWithTemplate("Parameter is not allowed. Valid parameters are " + allowedParams)
                        .addPropertyNode("parameters")
                        .addBeanNode().inIterable().atKey(key)
                        .addConstraintViolation();
                return false;
            }
        }
        for (StackParam param : StackParam.values()) {
            if (json.getParameters().keySet().contains(param.getName())) {
                List<StackParam> paramByGroup = StackParam.getParamsByGroup(param.getGroup());
                for (StackParam stackParam : paramByGroup) {
                    if (json.getParameters().get(stackParam.getName()) == null) {
                        context.buildConstraintViolationWithTemplate(
                                String.format("The %s dependency is the %s field which is not represented in the json",
                                        param.getName(), stackParam.getName()))
                                .addPropertyNode("parameters")
                                .addBeanNode().inIterable().atKey(param.getName())
                                .addConstraintViolation();
                        return false;
                    }
                }
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
