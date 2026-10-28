package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.PROJECT_ID;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.SERVICE_ACCOUNT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudkms.v1.CloudKMSScopes;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.storage.StorageScopes;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class GcpCredentialFactory {

    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL, CloudKMSScopes.CLOUD_PLATFORM);

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String NO_JSON_KEY_MESSAGE = "The GCP credential has no JSON service account key. "
            + "Legacy P12 keys are no longer supported, please provide a JSON key.";

    private static final String CREDENTIAL_JSON = "credentialJson";

    private static final String GCP = "gcp";

    private static final String JSON = "json";

    public GoogleCredential buildCredential(CloudCredential gcpCredential, HttpTransport httpTransport) throws IOException, GeneralSecurityException {
        String credentialJson = getServiceAccountCredentialJson(gcpCredential);
        if (isEmpty(credentialJson)) {
            throw new CredentialVerificationException(NO_JSON_KEY_MESSAGE);
        }
        return GoogleCredential.fromStream(new ByteArrayInputStream(Base64.decodeBase64(credentialJson)), httpTransport, JSON_FACTORY)
                .createScoped(SCOPES);
    }

    public void prepareCredential(CloudCredential credential) {
        String credentialJson = getServiceAccountCredentialJson(credential);
        if (isEmpty(credentialJson)) {
            throw new CredentialVerificationException(NO_JSON_KEY_MESSAGE);
        }
        try {
            JsonNode credNode = JsonUtil.readTree(new String(Base64.decodeBase64(credentialJson)));
            JsonNode projectId = credNode.get("project_id");
            if (projectId != null) {
                credential.putParameter(PROJECT_ID, projectId.asText());
            } else {
                throw new CredentialVerificationException("project_id is missing from json");
            }
            JsonNode clientEmail = credNode.get("client_email");
            if (clientEmail != null) {
                credential.putParameter(SERVICE_ACCOUNT, clientEmail.asText());
            } else {
                throw new CredentialVerificationException("client_email is missing from json");
            }
        } catch (IOException iox) {
            throw new CredentialVerificationException("Invalid credential json!");
        }
    }

    public String getServiceAccountCredentialJson(CloudCredential credential) {
        return Optional.ofNullable((Map<String, Object>) credential.getParameters().get(GCP))
                .map(gcp -> (Map<String, Object>) gcp.get(JSON))
                .map(json -> json.get(CREDENTIAL_JSON))
                .map(Object::toString)
                .orElse(null);
    }
}
