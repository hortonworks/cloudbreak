package com.sequenceiq.cloudbreak.cloud.gcp.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.iam.v1.Iam;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class GcpIamFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpIamFactory.class);

    @Inject
    private JsonFactory jsonFactory;

    @Inject
    private GcpCredentialFactory gcpCredentialFactory;

    public Iam buildIam(CloudCredential gcpCredential) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = gcpCredentialFactory.buildCredential(gcpCredential, httpTransport);
            return new Iam.Builder(
                    httpTransport, jsonFactory, null).setApplicationName(gcpCredential.getName())
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.warn("Error occurred while building Google Compute access.", e);
            throw new CredentialVerificationException("Error occurred while building Google Compute access.", e);
        }
    }
}
