package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.validation.ConstraintValidatorContext;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;

public class AmbariKerberosDescriptorValidatorTest {

    private static final String EMPTY_DESCRIPTOR_JSON = "{\"kerberos-env\":{\"properties\":{\"kdc_type\":\"kdc-type\",\"kdc_hosts\":\"kdc-host-value\","
            + "\"admin_server_host\":\"admin-server-host-value\",\"realm\":\"realm-value\"}}}";

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private ValidAmbariKerberosDescriptor annotation;

    @InjectMocks
    private AmbariKerberosDescriptorValidator underTest;

    @Before
    public void setUp() {
        underTest = new AmbariKerberosDescriptorValidator();
        initMocks(this);
        underTest.initialize(annotation);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addConstraintViolation()).thenReturn(constraintValidatorContext);
    }

    @Test
    public void testBase64Validation() {
        AmbariKerberosDescriptor desc = new AmbariKerberosDescriptor();
        desc.setKrb5Conf(invalidBase64Krb5Conf());
        desc.setDescriptor(validDescriptor());

        assertFalse(underTest.isValid(desc, constraintValidatorContext));

        desc.setDescriptor(invalidBase64Descriptor());
        desc.setKrb5Conf(validKrb5Conf());

        assertFalse(underTest.isValid(desc, constraintValidatorContext));
    }

    @Test
    public void testKrb5ConfValidation() {
        AmbariKerberosDescriptor desc = new AmbariKerberosDescriptor();
        desc.setKrb5Conf(invalidKrb5ConfJson());
        desc.setDescriptor(validDescriptor());

        assertFalse(underTest.isValid(desc, constraintValidatorContext));
    }

    @Test
    public void testDescriptorValidation() {
        AmbariKerberosDescriptor desc = new AmbariKerberosDescriptor();
        desc.setKrb5Conf(validKrb5Conf());
        desc.setDescriptor(invalidDescriptorJson());

        assertFalse(underTest.isValid(desc, constraintValidatorContext));
    }

    @Test
    public void testValid() {
        AmbariKerberosDescriptor desc = new AmbariKerberosDescriptor();
        desc.setKrb5Conf(validKrb5Conf());
        desc.setDescriptor(validDescriptor());

        assertTrue(underTest.isValid(desc, constraintValidatorContext));
    }

    private String validKrb5Conf() {
        return Base64.encodeBase64String("{}".getBytes());
    }

    private String invalidKrb5ConfJson() {
        return Base64.encodeBase64String("asd".getBytes());
    }

    private String invalidBase64Krb5Conf() {
        return "{}";
    }

    private String validDescriptor() {
        return Base64.encodeBase64String(EMPTY_DESCRIPTOR_JSON.getBytes());
    }

    private String invalidDescriptorJson() {
        return Base64.encodeBase64String("asd".getBytes());
    }

    private String invalidBase64Descriptor() {
        return EMPTY_DESCRIPTOR_JSON;
    }

}
