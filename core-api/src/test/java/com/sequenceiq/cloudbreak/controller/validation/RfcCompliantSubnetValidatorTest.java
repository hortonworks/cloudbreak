package com.sequenceiq.cloudbreak.controller.validation;

import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.validation.SubnetType;
import com.sequenceiq.cloudbreak.validation.SubnetValidator;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

@RunWith(MockitoJUnitRunner.class)
public class RfcCompliantSubnetValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private SubnetValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidSubnet validRfcComplicantSubnet;

    @Before
    public void setUp() {
        when(validRfcComplicantSubnet.value()).thenReturn(SubnetType.RFC_1918_COMPLIANT_ONLY);
        underTest.initialize(validRfcComplicantSubnet);
    }

    @Test
    public void validSubnet10Per8ReturnTrue() {
        Assert.assertTrue(underTest.isValid("10.0.0.0/8", constraintValidatorContext));
    }

    @Test
    public void validSubnet172Dot16Per12ReturnTrue() {
        Assert.assertTrue(underTest.isValid("172.16.0.0/12", constraintValidatorContext));
    }

    @Test
    public void validSubnet192Dot168Per16ReturnTrue() {
        Assert.assertTrue(underTest.isValid("192.168.0.0/16", constraintValidatorContext));
    }

    @Test
    public void validSubnetNullReturnTrue() {
        Assert.assertTrue(underTest.isValid(null, constraintValidatorContext));
    }

    @Test
    public void invalidSubnetEmptyReturnFalse() {
        Assert.assertFalse(underTest.isValid("", constraintValidatorContext));
    }

    @Test
    public void invalidSubnetNetmaskMissingWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0", constraintValidatorContext));
    }

    @Test
    public void invalidSubnetNonRfc1918WillReturnFalse() {
        Assert.assertFalse(underTest.isValid("172.32.0.0/12", constraintValidatorContext));
    }

    @Test
    public void invalidSubnetWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("10.0.0.1/24", constraintValidatorContext));
    }

    @Test
    public void invalidSubnetNetmaskHighWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0/42", constraintValidatorContext));
    }

    @Test
    public void anywhereWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0/0", constraintValidatorContext));
    }

}
