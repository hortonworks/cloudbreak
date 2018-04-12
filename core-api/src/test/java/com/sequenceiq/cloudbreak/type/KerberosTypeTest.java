package com.sequenceiq.cloudbreak.type;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;

@RunWith(MockitoJUnitRunner.class)
public class KerberosTypeTest {

    @Test
    public void testCloudbreakManaged() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("mk");
        request.setAdmin("adm");
        request.setPassword("pwd");

        Assert.assertEquals(KerberosType.CB_MANAGED, KerberosType.valueOf(request));
    }

    @Test
    public void testCloudbreakManagedMissing() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("mk");
        request.setAdmin("adm");

        Assert.assertNull(KerberosType.valueOf(request));
    }

    @Test
    public void testCloudbreakManagedPlusField() {
        KerberosRequest request = new KerberosRequest();
        request.setMasterKey("mk");
        request.setAdmin("adm");
        request.setPassword("pwd");

        request.setPrincipal("prnc");

        Assert.assertNull(KerberosType.valueOf(request));
    }

    @Test
    public void testExistingAd() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("prnc");
        request.setPassword("pwd");
        request.setUrl("url");
        request.setAdminUrl("admurl");
        request.setRealm("rlm");
        request.setLdapUrl("ldpurl");
        request.setContainerDn("cntrdn");

        Assert.assertEquals(KerberosType.EXISTING_AD, KerberosType.valueOf(request));
    }

    @Test
    public void testExistingMit() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("prnc");
        request.setPassword("pwd");
        request.setUrl("url");
        request.setAdminUrl("admurl");
        request.setRealm("rlm");

        Assert.assertEquals(KerberosType.EXISTING_MIT, KerberosType.valueOf(request));
    }

    @Test
    public void testExistingMitPlusField() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("prnc");
        request.setPassword("pwd");
        request.setUrl("url");
        request.setAdminUrl("admurl");
        request.setRealm("rlm");

        request.setLdapUrl("ldpurl");

        Assert.assertNull(KerberosType.valueOf(request));
    }

    @Test
    public void testCustom() {
        KerberosRequest request = new KerberosRequest();
        request.setPrincipal("prnc");
        request.setPassword("pwd");
        request.setDescriptor("{}");
        request.setKrb5Conf("{}");

        Assert.assertEquals(KerberosType.CUSTOM, KerberosType.valueOf(request));
    }
}
