package com.sequenceiq.cloudbreak.controller.validation

import javax.validation.ConstraintValidatorContext

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Matchers
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner

import com.sequenceiq.cloudbreak.validation.SubnetValidator
import com.sequenceiq.cloudbreak.validation.ValidSubnet

@RunWith(MockitoJUnitRunner::class)
class SubnetValidatorTest : AbstractValidatorTest() {

    @InjectMocks
    private val underTest: SubnetValidator? = null

    @Mock
    private val constraintValidatorContext: ConstraintValidatorContext? = null

    @Mock
    private val validSubnet: ValidSubnet? = null

    @Before
    fun setUp() {
        underTest!!.initialize(validSubnet)
        BDDMockito.given(constraintValidatorContext!!.buildConstraintViolationWithTemplate(Matchers.anyString())).willReturn(constraintViolationBuilder)
    }

    @Test
    fun validSubnet10Per8ReturnTrue() {
        Assert.assertTrue(underTest!!.isValid("10.0.0.0/8", constraintValidatorContext))
    }

    @Test
    fun validSubnet172Dot16Per12ReturnTrue() {
        Assert.assertTrue(underTest!!.isValid("172.16.0.0/12", constraintValidatorContext))
    }

    @Test
    fun validSubnet192Dot168Per16ReturnTrue() {
        Assert.assertTrue(underTest!!.isValid("192.168.0.0/16", constraintValidatorContext))
    }

    @Test
    fun validSubnetNullReturnTrue() {
        Assert.assertTrue(underTest!!.isValid(null, constraintValidatorContext))
    }

    @Test
    fun inValidSubnetEmptyReturnFalse() {
        Assert.assertFalse(underTest!!.isValid("", constraintValidatorContext))
    }

    @Test
    fun inValidSubnetNetmaskMissingWillReturnFalse() {
        Assert.assertFalse(underTest!!.isValid("0.0.0.0", constraintValidatorContext))
    }

    @Test
    fun inValidSubnetNonRfc1918WillReturnFalse() {
        Assert.assertFalse(underTest!!.isValid("172.32.0.0/12", constraintValidatorContext))
    }

    @Test
    fun inValidSubnetNetmaskHighWillReturnFalse() {
        Assert.assertFalse(underTest!!.isValid("0.0.0.0/42", constraintValidatorContext))
    }

}