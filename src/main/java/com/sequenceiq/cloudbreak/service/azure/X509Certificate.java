package com.sequenceiq.cloudbreak.service.azure;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import com.google.common.base.Strings;


public class X509Certificate {

    public static final int RADIX = 16;
    public static final int SIZE = 40;
    private Certificate cert;

    public X509Certificate(String path) throws FileNotFoundException, CertificateException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        cert = certificateFactory.generateCertificate(new FileInputStream(path));
    }

    public String getSha1Fingerprint() throws CertificateEncodingException, NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
        messageDigest.update(cert.getEncoded());
        String digest = new BigInteger(1, messageDigest.digest()).toString(RADIX);
        return Strings.padStart(digest, SIZE, '0');
    }

    public byte[] getPem() throws CertificateEncodingException {
        return org.apache.commons.codec.binary.Base64.encodeBase64(cert.getEncoded());
    }

    public Boolean isAttached() {
        return false;
    }
}
