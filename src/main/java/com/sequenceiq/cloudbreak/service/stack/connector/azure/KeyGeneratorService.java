package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import sun.security.x509.X509CertImpl;


@Component
public class KeyGeneratorService {

    public void generateKey(String user, AzureCredential azureCredential, String alias, String path) throws Exception {
        String command = StringUtils.join(new String[]{
                "keytool",
                "-genkeypair",
                "-alias", alias,
                "-keyalg", "RSA",
                "-keystore", path,
                "-keysize", "2048",
                "-keypass", AzureStackUtil.DEFAULT_JKS_PASS,
                "-storepass", AzureStackUtil.DEFAULT_JKS_PASS,
                "-dname", "cn=" + user + azureCredential.getPostFix() + ",ou=engineering,o=company,c=US"
        }, " ");
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
    }

    public void generateSshKey(String path) throws IOException, CertificateException {
        String certAndKey = FileReaderUtils.readFileFromPathToString(path + ".pem");
        byte[] certBytes = parseDERFromPEM(certAndKey, "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
        X509Certificate cert = generateCertificateFromDER(certBytes);
        File file = new File(path + ".cer");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        ((X509CertImpl) cert).derEncode(fileOutputStream);
    }

    protected static byte[] parseDERFromPEM(String pem, String beginDelimiter, String endDelimiter) {
        String[] tokens = pem.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

}
