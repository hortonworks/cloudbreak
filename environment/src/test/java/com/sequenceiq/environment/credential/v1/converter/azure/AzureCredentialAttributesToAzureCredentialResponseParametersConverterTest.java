package com.sequenceiq.environment.credential.v1.converter.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.credential.attributes.azure.AppBasedAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.service.AzureCredentialCertificateService;

public class AzureCredentialAttributesToAzureCredentialResponseParametersConverterTest {

    private AzureCredentialAttributesToAzureCredentialResponseParametersConverter underTest;

    private AzureCredentialCertificateService azureCredentialCertificateService;

    @BeforeEach
    void setUp() {
        underTest = new AzureCredentialAttributesToAzureCredentialResponseParametersConverter();
        azureCredentialCertificateService = new AzureCredentialCertificateService();
    }

    @Test
    void testConvert() {
        AzureCredentialAttributes input = new AzureCredentialAttributes();
        AppBasedAttributes appBasedAttributes = new AppBasedAttributes();
        appBasedAttributes.setCertificate(azureCredentialCertificateService.generate());
        appBasedAttributes.setAccessKey("accessKey");
        appBasedAttributes.setAuthenticationType(AppAuthenticationType.CERTIFICATE);
        input.setAppBased(appBasedAttributes);

        AzureCredentialResponseParameters response = underTest.convert(input);

        assertEquals(input.getAppBased().getCertificate().getStatus(), response.getCertificate().getStatus());
        assertEquals(input.getAppBased().getCertificate().getSha512(), response.getCertificate().getSha512());
        assertEquals(Base64Util.encode(input.getAppBased().getCertificate().getCertificate()),
                response.getCertificate().getBase64());
        assertEquals(input.getAppBased().getCertificate().getExpiration(), response.getCertificate().getExpiration());
        assertEquals("accessKey", response.getAccessKey());
        assertEquals(AppAuthenticationType.CERTIFICATE, response.getAuthenticationType());
    }
}
