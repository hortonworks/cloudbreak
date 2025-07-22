package com.sequenceiq.cloudbreak.util;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ValidateUnwrappedValue;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

public class OneOfEnumValidatorTest {

    private OneOfEnumValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new OneOfEnumValidator();
        underTest.initialize(ReflectionUtils.findField(ValidatorTest.class, "validatorEnum").getAnnotation(OneOfEnum.class));
    }

    @Test
    void testIsValidSuccess() {
        Assertions.assertTrue(underTest.isValid(ValidatorEnum.GP2.name(), null));
    }

    @Test
    void testIsValid() {
        ConstraintValidatorContext constraintValidatorContext = new ConstraintValidatorContextImpl(
                null,
                PathImpl.createPathFromString("status"),
                new DummyConstraintDescriptor(),
                null,
                ExpressionLanguageFeatureLevel.NONE,
                ExpressionLanguageFeatureLevel.NONE
        );
        Assertions.assertFalse(underTest.isValid("UNSUPPORTED_TYPE", constraintValidatorContext));
    }

    class ValidatorTest {
        @OneOfEnum(enumClass = ValidatorEnum.class)
        private String validatorEnum;

        public String getValidatorEnum() {
            return validatorEnum;
        }

        public void setValidatorEnum(String validatorEnum) {
            this.validatorEnum = validatorEnum;
        }
    }

    enum ValidatorEnum {
        STANDARD,
        GP2
    }

    private static class DummyAnnotation implements Annotation {

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "dummy";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return getClass();
        }
    }

    private static class DummyConstraintDescriptor implements ConstraintDescriptor<DummyAnnotation> {

        @Override
        public DummyAnnotation getAnnotation() {
            return null;
        }

        @Override
        public String getMessageTemplate() {
            return "";
        }

        @Override
        public Set<Class<?>> getGroups() {
            return Collections.emptySet();
        }

        @Override
        public Set<Class<? extends Payload>> getPayload() {
            return Collections.emptySet();
        }

        @Override
        public ConstraintTarget getValidationAppliesTo() {
            return ConstraintTarget.PARAMETERS;
        }

        @Override
        public List<Class<? extends ConstraintValidator<DummyAnnotation, ?>>> getConstraintValidatorClasses() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isReportAsSingleViolation() {
            return false;
        }

        @Override
        public ValidateUnwrappedValue getValueUnwrapping() {
            return null;
        }

        @Override
        public <U> U unwrap(Class<U> type) {
            return null;
        }
    }
}
