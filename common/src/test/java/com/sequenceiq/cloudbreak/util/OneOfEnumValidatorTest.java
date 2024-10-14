package com.sequenceiq.cloudbreak.util;

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
        Assertions.assertFalse(underTest.isValid("UNSUPPORTED_TYPE", null));
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
}
