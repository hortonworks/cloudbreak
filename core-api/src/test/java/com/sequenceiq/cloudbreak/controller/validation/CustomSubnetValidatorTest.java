package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.validation.SubnetType;
import com.sequenceiq.cloudbreak.validation.SubnetValidator;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

@ExtendWith(MockitoExtension.class)
class CustomSubnetValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private SubnetValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidSubnet validCustomSubnet;

    @BeforeEach
    void setUp() {
        when(validCustomSubnet.value()).thenReturn(SubnetType.CUSTOM);
        underTest.initialize(validCustomSubnet);
    }

    @Test
    void validSubnet10Per8ReturnTrue() {
        assertTrue(underTest.isValid("10.0.0.0/8", constraintValidatorContext));
    }

    @Test
    void validSubnet172Dot16Per12ReturnTrue() {
        assertTrue(underTest.isValid("172.16.0.0/12", constraintValidatorContext));
    }

    @Test
    void validSubnet192Dot168Per16ReturnTrue() {
        assertTrue(underTest.isValid("192.168.0.0/16", constraintValidatorContext));
    }

    @Test
    void validSubnetNullReturnTrue() {
        assertTrue(underTest.isValid(null, constraintValidatorContext));
    }

    @Test
    void invalidSubnetEmptyReturnFalse() {
        assertFalse(underTest.isValid("", constraintValidatorContext));
    }

    @Test
    void invalidSubnetNetmaskMissingWillReturnFalse() {
        assertFalse(underTest.isValid("0.0.0.0", constraintValidatorContext));
    }

    @Test
    void nonRfc1918WillReturnTrue() {
        assertTrue(underTest.isValid("172.32.0.0/12", constraintValidatorContext));
    }

    @Test
    void invalidSubnetWillReturnFalse() {
        assertFalse(underTest.isValid("10.0.0.1/24", constraintValidatorContext));
    }

    @Test
    void invalidSubnetNetmaskHighWillReturnFalse() {
        assertFalse(underTest.isValid("0.0.0.0/42", constraintValidatorContext));
    }

    @Test
    void invalidIpWillReturnFalse() {
        assertFalse(underTest.isValid("256.0.0.0/32", constraintValidatorContext));
    }

    @Test
    void validIpWillReturnTrue() {
        assertTrue(underTest.isValid("100.0.0.0/32", constraintValidatorContext));
    }

    @Test
    void anywhereWillReturnTrue() {
        assertTrue(underTest.isValid("0.0.0.0/0", constraintValidatorContext));
    }
}
