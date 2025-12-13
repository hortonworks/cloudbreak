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
class RfcCompliantSubnetValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private SubnetValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidSubnet validRfcComplicantSubnet;

    @BeforeEach
    void setUp() {
        when(validRfcComplicantSubnet.value()).thenReturn(SubnetType.RFC_1918_COMPLIANT_ONLY);
        underTest.initialize(validRfcComplicantSubnet);
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
    void invalidSubnetNonRfc1918WillReturnFalse() {
        assertFalse(underTest.isValid("172.32.0.0/12", constraintValidatorContext));
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
    void anywhereWillReturnFalse() {
        assertFalse(underTest.isValid("0.0.0.0/0", constraintValidatorContext));
    }

}
