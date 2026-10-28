package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }

    @Test
    public void testGetServiceAccountCredentialJsonWhenNoGcpEntryShouldReturnNull() {
        CloudCredential cloudCredential = new CloudCredential("1", "name", new HashMap<>(), "acc");
        assertNull(underTest.getServiceAccountCredentialJson(cloudCredential));
    }

    @Test
    public void testGetServiceAccountCredentialJsonWhenNoJsonEntryShouldReturnNull() {
        assertNull(underTest.getServiceAccountCredentialJson(legacyP12Credential()));
    }

    @Test
    public void testGetServiceAccountCredentialJsonWhenCredentialJsonEmptyShouldReturnEmpty() {
        CloudCredential cloudCredential = getCloudCredential("");
        assertEquals("", underTest.getServiceAccountCredentialJson(cloudCredential));
    }

    @Test
    public void testPrepareCredentialWhenNoGcpEntryShouldThrowWithP12Message() {
        CloudCredential cloudCredential = new CloudCredential("1", "name", new HashMap<>(), "acc");
        CredentialVerificationException e = assertThrows(CredentialVerificationException.class,
                () -> underTest.prepareCredential(cloudCredential));
        assertTrue(e.getMessage().contains("Legacy P12 keys are no longer supported"));
    }

    @Test
    public void testPrepareCredentialWhenNoJsonEntryShouldThrowWithP12Message() {
        CredentialVerificationException e = assertThrows(CredentialVerificationException.class,
                () -> underTest.prepareCredential(legacyP12Credential()));
        assertTrue(e.getMessage().contains("Legacy P12 keys are no longer supported"));
    }

    @Test
    public void testPrepareCredentialWhenCredentialJsonEmptyShouldThrowWithP12Message() {
        CloudCredential cloudCredential = getCloudCredential("");
        CredentialVerificationException e = assertThrows(CredentialVerificationException.class,
                () -> underTest.prepareCredential(cloudCredential));
        assertTrue(e.getMessage().contains("Legacy P12 keys are no longer supported"));
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

    // A credential as stored before CB-35227: a gcp entry with only a p12 node, no json.
    private CloudCredential legacyP12Credential() {
        Map<String, Object> p12 = new HashMap<>();
        p12.put("serviceAccountId", "s");
        p12.put("serviceAccountPrivateKey", "k");
        p12.put("projectId", "p");
        Map<String, Object> gcp = new HashMap<>();
        gcp.put("p12", p12);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("gcp", gcp);
        return new CloudCredential("1", "name", parameters, "acc");
    }

}