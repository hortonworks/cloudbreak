package com.sequenceiq.cloudbreak.controller.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.KerberosValidator;

@RunWith(MockitoJUnitRunner.class)
public class KerberosValidatorTest extends AbstractValidatorTest {

    @InjectMocks
    private KerberosValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Test
    public void testCbManaged() {
        KerberosRequest request = createCbManagedRequestWithoutType();
        request.setType(KerberosType.CB_MANAGED);

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testNotCbManaged() {
        KerberosRequest request = createCbManagedRequestWithoutType();
        request.setType(KerberosType.EXISTING_MIT);

        Assert.assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testCustom() {
        KerberosRequest request = createCustomRequestWithoutType();
        request.setType(KerberosType.CUSTOM);

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testNotCustom() {
        KerberosRequest request = createCustomRequestWithoutType();
        request.setType(KerberosType.EXISTING_MIT);

        Assert.assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testExistingMit() {
        KerberosRequest request = createExistingMitRequestWithoutType();
        request.setType(KerberosType.EXISTING_MIT);

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testNotExistingMit() {
        KerberosRequest request = createExistingMitRequestWithoutType();
        request.setType(KerberosType.CUSTOM);

        Assert.assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testExistingAd() {
        KerberosRequest request = createExistingAdRequestWithoutType();
        request.setType(KerberosType.EXISTING_AD);

        Assert.assertTrue(underTest.isValid(request, constraintValidatorContext));
    }

    @Test
    public void testNotExistingAd() {
        KerberosRequest request = createExistingAdRequestWithoutType();
        request.setType(KerberosType.CUSTOM);

        Assert.assertFalse(underTest.isValid(request, constraintValidatorContext));
    }

    private KerberosRequest createCbManagedRequestWithoutType() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("asdf");
        request.setPassword("pass");
        request.setTcpAllowed(true);
        request.setAdmin("admin");
        return request;
    }

    private KerberosRequest createCustomRequestWithoutType() {
        KerberosRequest request = new KerberosRequest();
        request.setKrb5Conf("{}");
        request.setPassword("pass");
        request.setTcpAllowed(true);
        request.setPrincipal("principal");
        request.setDescriptor("descriptor");
        return request;
    }

    private KerberosRequest createExistingMitRequestWithoutType() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("principal");
        request.setPassword("pass");
        request.setUrl("url");
        request.setAdminUrl("adminUrl");
        request.setRealm("realm");
        return request;
    }

    private KerberosRequest createExistingAdRequestWithoutType() {
        KerberosRequest request = createExistingMitRequestWithoutType();
        request.setLdapUrl("ldapUrl");
        request.setContainerDn("containerDn");
        return request;
    }
}