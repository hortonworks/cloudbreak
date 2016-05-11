package com.sequenceiq.cloudbreak.controller.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
        BDDMockito.given(constraintValidatorContext.buildConstraintViolationWithTemplate(Matchers.anyString())).willReturn(getConstraintViolationBuilder());
    }

    @Test
    public void validSubnetReturnTrue() {
        Assert.assertTrue(underTest.isValid("10.0.0.0/16", constraintValidatorContext));
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
    public void inValidSubnetInvalidStartWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0/24", constraintValidatorContext));
    }

    @Test
    public void inValidSubnetNetmaskHighWillReturnFalse() {
        Assert.assertFalse(underTest.isValid("0.0.0.0/42", constraintValidatorContext));
    }

}