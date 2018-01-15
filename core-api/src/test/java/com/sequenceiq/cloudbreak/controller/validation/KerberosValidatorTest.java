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

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.validation.KerberosValidator;
import com.sequenceiq.cloudbreak.validation.ValidKerberos;

@RunWith(MockitoJUnitRunner.class)
public class KerberosValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private KerberosValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidKerberos validJson;

    @Before
    public void setUp() {
        underTest.initialize(validJson);
        BDDMockito.given(constraintValidatorContext.buildConstraintViolationWithTemplate(Matchers.anyString())).willReturn(getConstraintViolationBuilder());
    }

    @Test
    public void testCloudbreakManaged() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("mk");
        request.setAdmin("adm");
        request.setPassword("pwd");

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testCloudbreakManagedMissing() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("mk");
        request.setAdmin("adm");

        Assert.assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testCloudbreakManagedPlusField() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("mk");
        request.setAdmin("adm");
        request.setPassword("pwd");

        request.setPrincipal("prnc");

        Assert.assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testExisting() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("prnc");
        request.setPassword("pwd");
        request.setUrl("url");
        request.setAdminUrl("admurl");
        request.setRealm("rlm");
        request.setLdapUrl("ldpurl");
        request.setContainerDn("cntrdn");

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testCustom() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("prnc");
        request.setPassword("pwd");
        request.setDescriptor("{}");
        request.setKrb5Conf("{}");

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }
}