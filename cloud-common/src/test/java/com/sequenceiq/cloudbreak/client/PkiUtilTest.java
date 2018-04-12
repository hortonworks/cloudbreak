package com.sequenceiq.cloudbreak.client;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PkiUtilTest {

    @Test
    public void generateCert() throws Exception {
        KeyPair identityKey = PkiUtil.generateKeypair();
        KeyPair signKey = PkiUtil.generateKeypair();

        X509Certificate cert = PkiUtil.cert(identityKey, "192.168.99.100", signKey);

        String pk = PkiUtil.convert(identityKey.getPrivate());
        String cer = PkiUtil.convert(cert);

        Assert.assertNotNull(pk);
        Assert.assertNotNull(cer);

    }
}
