package com.sequenceiq.environment.environment.dto.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AppBasedAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.CodeGrantFlowAttributes;

@ExtendWith(MockitoExtension.class)
public class AzureCredentialDetailsConverterTest {
    @InjectMocks
    private AzureCredentialDetailsConverter underTest;

    @Test
    void convertCredentialDetailsCodeGrantFlow() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        azureCredentialAttributes.setCodeGrantFlowBased(new CodeGrantFlowAttributes());
        credentialAttributes.setAzure(azureCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.AZURE_CODEGRANTFLOW, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsAppBasedWithSecret() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        AppBasedAttributes appBasedAttributes = new AppBasedAttributes();
        appBasedAttributes.setAuthenticationType(AppAuthenticationType.SECRET);
        azureCredentialAttributes.setAppBased(appBasedAttributes);
        credentialAttributes.setAzure(azureCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.AZURE_APPBASED_SECRET, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsAppBasedWithCertificate() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        AppBasedAttributes appBasedAttributes = new AppBasedAttributes();
        appBasedAttributes.setAuthenticationType(AppAuthenticationType.CERTIFICATE);
        azureCredentialAttributes.setAppBased(appBasedAttributes);
        credentialAttributes.setAzure(azureCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.AZURE_APPBASED_CERTIFICATE, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsAppBasedWithNullAuthType() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        AppBasedAttributes appBasedAttributes = new AppBasedAttributes();
        azureCredentialAttributes.setAppBased(appBasedAttributes);
        credentialAttributes.setAzure(azureCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.AZURE_APPBASED_SECRET, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsWhenAttributesNull() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.UNKNOWN, builder.build().getCredentialType());
    }
}
