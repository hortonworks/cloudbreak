package com.sequenceiq.cloudbreak.controller.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.validation.SubnetValidator;
import com.sequenceiq.cloudbreak.validation.ValidSubnet;

@RunWith(MockitoJUnitRunner.class)
public class SubnetValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private SubnetValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidSubnet validSubnet;

    @Before
    public void setUp() {
        underTest.initialize(validSubnet);
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
    public void inValidSubnetEmptyReturnFalse() {
        Assert.assertFalse(underTest.isValid("", constraintValidatorContext));
    }

    @Test
    public void inValidSubnetNetmaskMissingWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0", constraintValidatorContext));
    }

    @Test
    public void inValidSubnetNonRfc1918WillReturnFalse() {
        Assert.assertFalse(underTest.isValid("172.32.0.0/12", constraintValidatorContext));
    }

    @Test
    public void inValidSubnetNetmaskHighWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0/42", constraintValidatorContext));
    }

}