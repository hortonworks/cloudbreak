package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

public class OneOfEnumValidatorTest {

    private OneOfEnumValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new OneOfEnumValidator();
        underTest.initialize(ReflectionUtils.findField(DiskUpdateRequest.class, "volumeType").getAnnotation(OneOfEnum.class));
    }

    @Test
    void testIsValidSuccess() {
        Assertions.assertTrue(underTest.isValid(SupportedVolumeType.GP2.name(), null));
    }

    @Test
    void testIsValid() {
        Assertions.assertFalse(underTest.isValid("UNSUPPORTED_TYPE", null));
    }
}
