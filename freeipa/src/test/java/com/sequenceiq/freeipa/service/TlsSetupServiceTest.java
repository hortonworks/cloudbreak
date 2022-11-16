package com.sequenceiq.freeipa.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.SecurityUtils;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.RestClientUtil;

class TlsSetupServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupServiceTest.class);

    @Test
    void setupTls() throws GeneralSecurityException, IOException {

        String cert = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDCjCCAfKgAwIBAgIRAPNT5xF7RBCKnH/2nfsw8mcwDQYJKoZIhvcNAQELBQAw\n" +
                "EjEQMA4GA1UEChMHZ2F0ZXdheTAeFw0yMjExMTUxNTM3MDBaFw0yNTAyMTcxNTM3\n" +
                "MDBaMBIxEDAOBgNVBAoTB2dhdGV3YXkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n" +
                "ggEKAoIBAQDdpbk5Qktgk22t3m5WB2HndzAsfjtMm3TKhVo15mmMFku0gubJk3t6\n" +
                "yUEDtwk5RU84TJiYXXOL9VrKL7RXZVmSQL/Iw24o6jDozxR77H9kcGmHq+qXfuWM\n" +
                "o6Cc4U7KrNEYS5gPq5zlbvIVp1QHZ6EauqACNBL9bSIw0gbZ74RZbqGM6L6UmEqv\n" +
                "CaoA4XnEIeuKSK0tjb7KplBxDLqssvm0FBgCEB43fiNbSIyj4Nxdnjh5v/R+1QYU\n" +
                "Z3CO6Q8BwDydd5HRzkjeNWpbrMfZR4M4qiLFnIIRF1T8WIUx3yk639h5n0vuLNj1\n" +
                "Rz73G7F/vaYfpSwwKSneA+uC0x91NimpAgMBAAGjWzBZMA4GA1UdDwEB/wQEAwID\n" +
                "qDAdBgNVHSUEFjAUBggrBgEFBQcDAgYIKwYBBQUHAwEwDAYDVR0TAQH/BAIwADAa\n" +
                "BgNVHREEEzARgglsb2NhbGhvc3SHBH8AAAEwDQYJKoZIhvcNAQELBQADggEBANCj\n" +
                "bKLFE1IvKkjte0DTSC3Ls/iu//mzi3L8fWvPojSXPiTfM5IIsX9VxseAbYnK2CSi\n" +
                "kYJAwK0MvfmjP/fLgrLZnSFpJ318aa0wc2r2+eV0xwdDQNs+5fZ70oolj6UPPcmp\n" +
                "UBgpr/QQOf0PReEYey37h+L789HEDeNvY2jGodQQgv1PRp8LIXLV4bC/1/9ua5Gz\n" +
                "9GODv+w6L/NNWzCW/BTw5S3JbGdpAcxqqN4daK8LZr5iP+tCzVgA9KoLh1PSlczj\n" +
                "ATG7JVyVDaB2xufzmlKmD3C/O/IXEtNLes4Hb6ckydoCmosQ5WjvwLuXQhXC3m0b\n" +
                "LE7itOek28NynbkRZbw=\n" +
                "-----END CERTIFICATE-----";
        String ip = "10.112.16.68";

        X509Certificate certificatePem = PkiUtil.fromCertificatePem(cert);
        LOGGER.info("Cert conversion from String was fine for domain: {}", certificatePem.getSubjectDN());

        ByteArrayInputStream certStream = new ByteArrayInputStream(cert.getBytes());

        X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(certStream);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = SecurityUtils.getDefaultKeyStore();
        keyStore.load(null);
        keyStore.setCertificateEntry("asdfaslmfas", x509Certificate);
//        SecurityUtils.loadKeyStore(keyStore, new ByteArrayInputStream(x509Certificate.getEncoded()), "changeit");
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers2 = trustManagerFactory.getTrustManagers();

        SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
        sslContext.init(null, trustManagers2, new SecureRandom());
        Client client = RestClientUtil.createClient(sslContext, true);


        WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, 9443));
        nginxTarget.path("/").request().get().close();

    }
}