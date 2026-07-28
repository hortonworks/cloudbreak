package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.credential.Credential;

@ExtendWith(MockitoExtension.class)
class CredentialToCloudCredentialConverterTest {

    @InjectMocks
    private CredentialToCloudCredentialConverter underTest;

    @Test
    void testConvertShouldPropagateGovCloudTrueAndPreserveAttributes() {
        Credential credential = Credential.builder()
                .crn("crn")
                .name("name")
                .account("account")
                .govCloud(true)
                .attributes(new Json("{\"key\":\"value\"}"))
                .build();

        CloudCredential result = underTest.convert(credential);

        assertTrue(result.isGovCloud());
        assertEquals("value", result.getParameter("key", String.class));
    }

    @Test
    void testConvertShouldPropagateGovCloudFalse() {
        Credential credential = Credential.builder()
                .crn("crn")
                .name("name")
                .account("account")
                .govCloud(false)
                .attributes(new Json("{}"))
                .build();

        CloudCredential result = underTest.convert(credential);

        assertFalse(result.isGovCloud());
    }
}
