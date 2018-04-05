package com.sequenceiq.cloudbreak.controller.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.validation.KerberosDescriptorValidator;
import com.sequenceiq.cloudbreak.validation.ValidKerberosDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class KerberosDescriptorValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private KerberosDescriptorValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidKerberosDescriptor validJson;

    @Before
    public void setUp() {
        underTest.initialize(validJson);
    }

    @Test
    public void testEmptyValue() {
        Assert.assertTrue(underTest.isValid("", constraintValidatorContext));
    }

    @Test
    public void testEmptyJsonValue() {
        Assert.assertFalse(underTest.isValid("{}", constraintValidatorContext));
    }

    @Test
    public void testMissingDescriptor() {
        Assert.assertFalse(underTest.isValid(
                "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"type\",\"kdc_hosts\":\"hosts\",\"admin_server_host\":\"server_host\"}}}",
                constraintValidatorContext));
    }

    @Test
    public void testFullDescriptor() {
        Assert.assertTrue(underTest.isValid(
                "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"type\",\"kdc_hosts\":\"hosts\",\"admin_server_host\":\"server_host\",\"realm\":\"realm\"}}}",
                constraintValidatorContext));
    }
}