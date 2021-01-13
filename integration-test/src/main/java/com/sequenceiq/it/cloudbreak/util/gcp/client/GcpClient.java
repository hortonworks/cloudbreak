package com.sequenceiq.it.cloudbreak.util.gcp.client;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.storage.StorageScopes;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties;

@Component
public class GcpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpClient.class);

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    @Inject
    private GcpProperties gcpProperties;

    public Compute buildCompute() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = buildCredential(httpTransport);
            return new Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName("Gcp E2E test credential")
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.warn("Error occurred while building Google Compute access.", e);
            throw new IllegalArgumentException("Error occurred while building Google Compute access.", e);
        }
    }

    public GoogleCredential buildCredential(HttpTransport httpTransport) throws IOException, GeneralSecurityException {
        String credentialJson = gcpProperties.getCredential().getJson().getBase64();
        if (isNotEmpty(credentialJson)) {
            return GoogleCredential.fromStream(new ByteArrayInputStream(Base64.decodeBase64(credentialJson)), httpTransport, JSON_FACTORY)
                    .createScoped(SCOPES);
        } else {
            try {
                GcpProperties.Credential credential = gcpProperties.getCredential();
                PrivateKey pk = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                        new ByteArrayInputStream(Base64.decodeBase64(credential.getP12().getBase64())), "notasecret", "privatekey",
                        "notasecret");
                return new GoogleCredential.Builder().setTransport(httpTransport)
                        .setJsonFactory(JSON_FACTORY)
                        .setServiceAccountId(credential.getP12().getServiceAccountId())
                        .setServiceAccountScopes(SCOPES)
                        .setServiceAccountPrivateKey(pk)
                        .build();
            } catch (IOException e) {
                throw new IllegalArgumentException("Can not read private key from P12 file", e);
            }
        }
    }

    public String getProjectIdFromCredentialJson(String credentialJson) {
        try {
            JsonNode credNode = JsonUtil.readTree(new String(Base64.decodeBase64(credentialJson)));
            JsonNode projectId = credNode.get("project_id");
            if (projectId != null) {
                return projectId.asText().toLowerCase().replaceAll("[^A-Za-z0-9 ]", "-");
            }
        } catch (IOException ioException) {
            LOGGER.warn("Unable to read credential JSON into a tree.", ioException);
        }
        throw new IllegalArgumentException("Could not extract project id from credential JSON");
    }

    public String getProjectId() {
        if (gcpProperties.getCredential().getP12().getProjectId() == null || gcpProperties.getCredential().getP12().getProjectId().isEmpty()) {
            GcpProperties.Credential credential = gcpProperties.getCredential();
            return getProjectIdFromCredentialJson(credential.getJson().getBase64());
        } else {
            return gcpProperties.getCredential().getP12().getProjectId();
        }
    }

}
