package com.sequenceiq.cloudbreak.controller.validation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintValidatorContext;

import org.springframework.stereotype.Component;

@Component
public class ParametersTypeValidator extends AbstractParameterValidator {

    private static final String SEPARATOR = ", ";

    @Override
    public boolean validate(Map<String, Object> parameters, ConstraintValidatorContext context, List<TemplateParam> paramList) {
        boolean valid = true;
        for (TemplateParam entry : paramList) {
            Object param = parameters.get(entry.getName());
            if (param != null) {
                if (entry.getClazz().isEnum()) {
                    try {
                        entry.getClazz().getField(String.valueOf(parameters.get(entry.getName())));
                    } catch (NoSuchFieldException e) {
                        addParameterConstraintViolation(context, entry.getName(), String.format("%s is not valid type. The valid fields are [%s]",
                                entry.getName(),
                                fieldList(entry.getClazz().getFields())));
                        valid = false;
                    }
                } else if (!parameters.get(entry.getName()).getClass().isAssignableFrom(entry.getClazz())) {
                    addParameterConstraintViolation(context, entry.getName(), String.format("%s is not valid type.", entry.getName()));
                    valid = false;
                }
            }
        }
        return valid;
    }

    private String fieldList(Field[] fields) {
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            sb.append(field.getName());
            sb.append(SEPARATOR);
        }
        sb.replace(sb.toString().lastIndexOf(SEPARATOR), sb.toString().lastIndexOf(SEPARATOR) + 2, "");
        return sb.toString();
    }

    @Override
    public ValidatorType getValidatorType() {
        return ValidatorType.CLASS;
    }


}
