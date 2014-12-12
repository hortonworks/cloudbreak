package com.sequenceiq.cloudbreak.service.credential.azure;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Component
public class AzureCredentialHandler implements CredentialHandler<AzureCredential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCredentialHandler.class);
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";
    private static final int CERT_LINE_LENGTH = 76;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

    @Override
    public AzureCredential init(AzureCredential azureCredential) {
        validateCertificateFile(azureCredential);
        return azureCredential;
    }

    @Override
    public boolean delete(AzureCredential credential) {
        return true;
    }

    private void validateCertificateFile(AzureCredential azureCredential) {
        MDCBuilder.buildMdcContext(azureCredential);
        try {
            addNewlinesForCredential(azureCredential);
            InputStream is = new ByteArrayInputStream(azureCredential.getPublicKey().getBytes(StandardCharsets.UTF_8));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            cf.generateCertificate(is);
            azureCredential = azureStackUtil.generateAzureSshCerFile(azureCredential);
            azureCredential = azureStackUtil.generateAzureServiceFiles(azureCredential);
        } catch (Exception e) {
            String errorMessage = String.format("Could not validate publickey certificate [credential: '%s', certificate: '%s'], detailed message: %s",
                    azureCredential.getId(), azureCredential.getPublicKey(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new BadRequestException(errorMessage, e);
        }
    }

    private void addNewlinesForCredential(AzureCredential azureCredential) {
        String publicKey = azureCredential.getPublicKey();
        if (publicKey.startsWith(BEGIN_CERT) && publicKey.length() > BEGIN_CERT.length() + 1) {
            if (!"\n".equals(Character.toString(publicKey.charAt(BEGIN_CERT.length())))) {
                final String strPattern = (BEGIN_CERT + "(.*?)" + END_CERT).replaceAll("\n", "");
                final Pattern pattern = Pattern.compile(strPattern);
                final Matcher matcher = pattern.matcher(publicKey);
                if (matcher.find()) {
                    final String content = matcher.group(1);
                    final String hash = StringUtils.join(Splitter.fixedLength(CERT_LINE_LENGTH).split(content), "\n");
                    publicKey = BEGIN_CERT + "\n" + hash + "\n" + END_CERT;
                    azureCredential.setPublicKey(publicKey);
                }

            }
        }
    }
}
