package com.sequenceiq.environment.environment.dto.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.GcpCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.JsonAttributes;
import com.sequenceiq.environment.credential.attributes.gcp.P12Attributes;

@ExtendWith(MockitoExtension.class)
public class GcpCredentialDetailsConverterTest {
    @InjectMocks
    private GcpCredentialDetailsConverter underTest;

    @Test
    void convertCredentialDetailsJson() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        GcpCredentialAttributes gcpCredentialAttributes = new GcpCredentialAttributes();
        gcpCredentialAttributes.setJson(new JsonAttributes());
        credentialAttributes.setGcp(gcpCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.GCP_JSON, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsP12() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        GcpCredentialAttributes gcpCredentialAttributes = new GcpCredentialAttributes();
        gcpCredentialAttributes.setP12(new P12Attributes());
        credentialAttributes.setGcp(gcpCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.GCP_P12, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsAllTypeNull() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        GcpCredentialAttributes gcpCredentialAttributes = new GcpCredentialAttributes();
        credentialAttributes.setGcp(gcpCredentialAttributes);
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.UNKNOWN, builder.build().getCredentialType());
    }

    @Test
    void convertCredentialDetailsWhenAttributesNull() {
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        CredentialDetails.Builder builder = underTest.convertCredentialDetails(credentialAttributes, CredentialDetails.builder());
        assertEquals(CredentialType.UNKNOWN, builder.build().getCredentialType());
    }
}
