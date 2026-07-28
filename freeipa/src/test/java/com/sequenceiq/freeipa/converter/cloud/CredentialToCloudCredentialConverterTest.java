package com.sequenceiq.freeipa.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.freeipa.dto.Credential;

@ExtendWith(MockitoExtension.class)
class CredentialToCloudCredentialConverterTest {

    @InjectMocks
    private CredentialToCloudCredentialConverter underTest;

    @Test
    void testConvertShouldReturnNullWhenCredentialIsNull() {
        assertNull(underTest.convert(null));
    }

    @Test
    void testConvertShouldPropagateGovCloudTrueAndPreserveAttributes() {
        Credential credential = new Credential("AWS", "name", "{\"key\":\"value\"}", "crn", "account", true);

        CloudCredential result = underTest.convert(credential);

        assertTrue(result.isGovCloud());
        assertEquals("value", result.getParameter("key", String.class));
    }

    @Test
    void testConvertShouldPropagateGovCloudFalseWhenAttributesEmpty() {
        Credential credential = new Credential("AWS", "name", null, "crn", "account", false);

        CloudCredential result = underTest.convert(credential);

        assertFalse(result.isGovCloud());
    }
}
