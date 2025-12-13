package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class GcpCredentialFactoryTest {

    @InjectMocks
    private GcpCredentialFactory underTest;

    @Test
    public void testGetServiceAccountCredentialJsonWhenJsonHasValidTextShouldReturnWithTheText() {
        CloudCredential cloudCredential = getCloudCredential("super-json");
        String serviceAccountCredentialJson = underTest.getServiceAccountCredentialJson(cloudCredential);
        assertEquals("c3VwZXItanNvbg==", serviceAccountCredentialJson);
    }

    @Test
    public void testPrepareCredentialWhenJsonHasNoProjectIdShouldThrowCredentialVerificationException() {
        CloudCredential cloudCredential = getCloudCredential("{}");
        assertThrows(CredentialVerificationException.class, () -> underTest.prepareCredential(cloudCredential));
    }

    @Test
    public void testPrepareCredentialWhenJsonHasNoClientEmailShouldThrowCredentialVerificationException() {
        CloudCredential cloudCredential = getCloudCredential("{\"project_id\":\"joska-project\"}");
        assertThrows(CredentialVerificationException.class, () -> underTest.prepareCredential(cloudCredential));
    }

    @Test
    public void testPrepareCredentialWhenJsonHasClientEmailShouldThrowCredentialVerificationException() {
        CloudCredential cloudCredential = getCloudCredential("{\"project_id\":\"joska-project\", \"client_email\": \"client_email\"}");
        underTest.prepareCredential(cloudCredential);
        assertEquals(cloudCredential.getParameters().get("projectId"), "joska-project");
        assertEquals(cloudCredential.getParameters().get("serviceAccountId"), "client_email");
        assertEquals(cloudCredential.getParameters().get("serviceAccountPrivateKey"), "");
    }

    private String getCredentialJson(String credentialJson) {
        return Base64.encodeBase64String(credentialJson.getBytes());
    }

    private CloudCredential getCloudCredential(String jsonString) {
        Map<String, Object> gcp = new HashMap<>();
        Map<String, Object> json = new HashMap<>();
        Map<String, Object> parameters = new HashMap<>();
        json.put("credentialJson", getCredentialJson(jsonString));
        gcp.put("json", json);
        parameters.put("gcp", gcp);
        return new CloudCredential("1", "name", parameters, "acc");
    }

}