package com.sequenceiq.environment.credential.service;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialCertificate;

@Service
public class AzureCredentialCertificateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialCertificateService.class);

    private static final int AZURE_CREDENTIAL_CERT_VALIDITY_YEAR = 1;

    public AzureCredentialCertificate generate() {
        KeyPair identityKey = PkiUtil.generateKeypair();
        KeyPair signKey = PkiUtil.generateKeypair();
        X509Certificate cert = PkiUtil.cert(identityKey, "cloudbreak", signKey, AZURE_CREDENTIAL_CERT_VALIDITY_YEAR);

        AzureCredentialCertificate azureCredentialCertificate = new AzureCredentialCertificate();
        azureCredentialCertificate.setId("UUID");
        azureCredentialCertificate.setStatus("ACTIVE");
        azureCredentialCertificate.setPrivateKey(PkiUtil.convertPrivateKeyToPKCSPEM(identityKey.getPrivate()));
        azureCredentialCertificate.setExpiration(cert.getNotAfter().getTime());
        azureCredentialCertificate.setCertificate(PkiUtil.convert(cert));

        return azureCredentialCertificate;
    }

}
