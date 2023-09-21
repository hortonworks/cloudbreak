package com.sequenceiq.it.cloudbreak.util.gcp.client;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Builder;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.storage.StorageScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.gcp.GcpProperties.Credential;
import com.sequenceiq.it.cloudbreak.log.Log;

@Component
public class GcpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpClient.class);

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final List<String> SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL);

    private static final int ONE_MINUTE_IN_MILLISECOND = 60000;

    private static final int MINUTES = 3;

    private static final String APPLICATION_NAME = "Gcp E2E test credential";

    @Inject
    private GcpProperties gcpProperties;

    public Compute buildCompute() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credentials = buildCredential();
            HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);
            return new Builder(httpTransport, JSON_FACTORY, requestInitializer)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException e) {
            LOGGER.error("Cannot establish NetHttpTransport with trusted certificates!", e);
            throw new RuntimeException("Cannot establish NetHttpTransport with trusted certificates!", e);
        } catch (IOException e) {
            LOGGER.error("Canot return the key store for trusted certificates!", e);
            throw new RuntimeException("Canot return the key store for trusted certificates!", e);
        }
    }

    public Storage buildStorage() {
        return StorageOptions
                .newBuilder()
                .setCredentials(buildCredential())
                .setProjectId(getProjectId())
                .build()
                .getService();
    }

    public GoogleCredentials buildCredential() {
        Credential credential = gcpProperties.getCredential();
        String credentialJson = credential.getJson().getBase64();
        String credentialP12 = credential.getP12().getBase64();
        GoogleCredentials googleCredentials = null;
        try {
            if (isNotEmpty(credentialJson)) {
                googleCredentials = GoogleCredentials
                        .fromStream(new ByteArrayInputStream(Base64.decodeBase64(credentialJson)))
                        .createScoped(SCOPES);
            } else if (isNotEmpty(credentialP12)) {
                PrivateKey privateKey = SecurityUtils
                        .loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                                new ByteArrayInputStream(Base64.decodeBase64(credentialP12)), "notasecret", "privatekey",
                                "notasecret");

                googleCredentials = ServiceAccountCredentials.newBuilder()
                        .setClientEmail(credential.getP12().getServiceAccountId())
                        .setPrivateKey(privateKey)
                        .setScopes(SCOPES)
                        .build();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create Google Cloud Client!", e);
        }
        if (googleCredentials == null) {
            Log.error(LOGGER, "Cannot create GCP Client, because of both of the JSON and P12 credentials are missing!");
            throw new RuntimeException("Cannot create GCP Client, because of both of the JSON and P12 credentials are missing!");
        } else {
            return googleCredentials;
        }
    }

    protected String getProjectIdFromCredentialJson(String credentialJson) {
        try {
            JsonNode credNode = JsonUtil.readTree(new String(Base64.decodeBase64(credentialJson)));
            JsonNode projectId = credNode.get("project_id");
            if (projectId != null) {
                return projectId.asText().toLowerCase(Locale.ROOT).replaceAll("[^A-Za-z0-9 ]", "-");
            }
        } catch (IOException ioException) {
            LOGGER.warn("Unable to read credential JSON into a tree.", ioException);
        }
        throw new IllegalArgumentException("Could not extract project id from credential JSON");
    }

    protected String getProjectId() {
        if (gcpProperties.getCredential().getP12().getProjectId() == null || gcpProperties.getCredential().getP12().getProjectId().isEmpty()) {
            Credential credential = gcpProperties.getCredential();
            return getProjectIdFromCredentialJson(credential.getJson().getBase64());
        } else {
            return gcpProperties.getCredential().getP12().getProjectId();
        }
    }

    protected String getAvailabilityZone() {
        return gcpProperties.getAvailabilityZone();
    }

    protected String getBaseLocation() {
        return gcpProperties.getCloudStorage().getBaseLocation();
    }
}
