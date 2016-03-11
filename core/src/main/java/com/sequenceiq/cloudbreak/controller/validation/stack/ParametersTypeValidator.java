package com.sequenceiq.cloudbreak.controller.validation.stack;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.controller.BadRequestException;

@Component
public class ParametersTypeValidator implements ParameterValidator {

    private static final String SEPARATOR = ", ";

    @Override
    public <O, E extends StackParamValidation> void validate(Map<String, O> parameters, List<E> paramsList) {
        for (E entry : paramsList) {
            Object param = parameters.get(entry.getName());
            if (param != null) {
                if (entry.getClazz().isEnum()) {
                    try {
                        entry.getClazz().getField(String.valueOf(parameters.get(entry.getName())));
                    } catch (NoSuchFieldException e) {
                        throw new BadRequestException(String.format("%s is not valid type. The valid fields are [%s]",
                                entry.getName(),
                                fieldList(entry.getClazz().getFields())));
                    }
                } else {
                    try {
                        entry.getClazz().getConstructor(parameters.get(entry.getName()).getClass()).newInstance(parameters.get(entry.getName()));
                    } catch (Exception e) {
                        try {
                            entry.getClazz().getConstructor(String.class).newInstance(parameters.get(entry.getName()).toString());
                        } catch (Exception ex) {
                            throw new BadRequestException(ex.getMessage());
                        }
                    }
                }
            }
        }
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
