package com.sequenceiq.cloudbreak.service.credential.azure;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;

@Component
public class AzureCredentialInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialInitializer.class);

    public AzureCredential init(AzureCredential azureCredential) {
        validateCertificateFile(azureCredential);
        return azureCredential;
    }

    private void validateCertificateFile(AzureCredential azureCredential) {
        CbLoggerFactory.buildMdcContext(azureCredential);
        try {
            InputStream is = new ByteArrayInputStream(azureCredential.getPublicKey().getBytes(StandardCharsets.UTF_8));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate x509Certificate = (X509Certificate) cf.generateCertificate(is);
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate publickey certificate [credential: '%s', certificate: '%s'], detailed message: %s",
                    azureCredential.getId(), azureCredential.getPublicKey(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }
}
