package com.sequenceiq.cloudbreak.cloud.gcp.client;

import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.PROJECT_ID;
import static com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.SERVICE_ACCOUNT;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.cloudkms.v1.CloudKMSScopes;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.storage.StorageScopes;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class GcpCredentialFactory {

    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL, CloudKMSScopes.CLOUD_PLATFORM);

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final String PRIVATE_KEY = "serviceAccountPrivateKey";

    private static final String CREDENTIAL_JSON = "credentialJson";

    private static final String GCP = "gcp";

    private static final String JSON = "json";

    @Inject
    private GcpStackUtil gcpStackUtil;

    public GoogleCredential buildCredential(CloudCredential gcpCredential, HttpTransport httpTransport) throws IOException, GeneralSecurityException {
        String credentialJson = getServiceAccountCredentialJson(gcpCredential);
        if (isNotEmpty(credentialJson)) {
            return GoogleCredential.fromStream(new ByteArrayInputStream(Base64.decodeBase64(credentialJson)), httpTransport, JSON_FACTORY)
                    .createScoped(SCOPES);
        } else {
            try {
                PrivateKey pk = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                        new ByteArrayInputStream(Base64.decodeBase64(getServiceAccountPrivateKey(gcpCredential))), "notasecret", "privatekey", "notasecret");
                return new GoogleCredential.Builder()
                        .setTransport(httpTransport)
                        .setJsonFactory(JSON_FACTORY)
                        .setServiceAccountId(gcpStackUtil.getServiceAccountId(gcpCredential))
                        .setServiceAccountScopes(SCOPES)
                        .setServiceAccountPrivateKey(pk)
                        .build();
            } catch (IOException e) {
                throw new CredentialVerificationException("Can not read private key", e);
            }
        }
    }

    public void prepareCredential(CloudCredential credential) {
        try {
            String credentialJson = getServiceAccountCredentialJson(credential);
            if (isNotEmpty(credentialJson)) {
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
                credential.putParameter(PRIVATE_KEY, "");
            }
        } catch (IOException iox) {
            throw new CredentialVerificationException("Invalid credential json!");
        }
    }

    public String getServiceAccountCredentialJson(CloudCredential credential) {
        Map<String, Object> gcp = (Map<String, Object>) credential.getParameters().get(GCP);
        Map<String, Object> json = (Map<String, Object>) gcp.get(JSON);
        return json.get(CREDENTIAL_JSON).toString();
    }

    private String getServiceAccountPrivateKey(CloudCredential credential) {
        return credential.getParameter(PRIVATE_KEY, String.class);
    }
}
