package com.sequenceiq.environment.credential.service;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.certificate.MessageDigestUtil;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.common.api.credential.AppCertificateStatus;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialCertificate;

@Service
public class AzureCredentialCertificateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialCertificateService.class);

    private static final int AZURE_CREDENTIAL_CERT_VALIDITY_YEAR = 1;

    public AzureCredentialCertificate generate() {
        LOGGER.info("Generate new Azure credential certificate with {} year(s) of validity", AZURE_CREDENTIAL_CERT_VALIDITY_YEAR);

        KeyPair identityKey = PkiUtil.generateKeypair();
        KeyPair signKey = PkiUtil.generateKeypair();
        X509Certificate cert = PkiUtil.cert(identityKey, "cloudbreak", signKey, AZURE_CREDENTIAL_CERT_VALIDITY_YEAR);

        AzureCredentialCertificate azureCredentialCertificate = new AzureCredentialCertificate();
        azureCredentialCertificate.setStatus(AppCertificateStatus.KEY_GENERATED);
        azureCredentialCertificate.setPrivateKey(PkiUtil.convertPrivateKeyToPKCSPEM(identityKey.getPrivate()));
        azureCredentialCertificate.setExpiration(cert.getNotAfter().getTime());
        String certificateString = PkiUtil.convert(cert);
        azureCredentialCertificate.setCertificate(certificateString);
        azureCredentialCertificate.setSha512(MessageDigestUtil.signatureSHA512(certificateString));

        LOGGER.info("Generated Azure certificate: {}", azureCredentialCertificate);

        return azureCredentialCertificate;
    }
}
