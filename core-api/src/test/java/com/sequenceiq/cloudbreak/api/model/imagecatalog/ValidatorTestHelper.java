package com.sequenceiq.cloudbreak.api.model.imagecatalog;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.common.model.annotations.IgnorePojoValidation;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorTestHelperConfiguration.class })
@IgnorePojoValidation
public class ValidatorTestHelper {

    @Autowired
    private Validator validator;

    protected List<String> getPropertyPaths(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getPropertyPath).map(Object::toString).collect(Collectors.toList());
    }

    protected List<String> getMessageTemplate(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessageTemplate).map(msg -> msg.replaceAll("([{}])", ""))
                .collect(Collectors.toList());
    }

    public Validator getValidator() {
        return validator;
    }
}
