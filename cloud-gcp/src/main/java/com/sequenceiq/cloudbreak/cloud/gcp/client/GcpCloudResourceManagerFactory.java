package com.sequenceiq.cloudbreak.cloud.gcp.client;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class GcpCloudResourceManagerFactory extends GcpServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudResourceManagerFactory.class);

    @Inject
    private JsonFactory jsonFactory;

    @Inject
    private GcpCredentialFactory gcpCredentialFactory;

    @Inject
    private HttpTransport httpTransport;

    public CloudResourceManager buildCloudResourceManager(CloudCredential cloudCredential) {
        try {
            GoogleCredential credential = gcpCredentialFactory.buildCredential(cloudCredential, httpTransport);
            return new CloudResourceManager.Builder(httpTransport, jsonFactory, requestInitializer(credential))
                    .setApplicationName(cloudCredential.getName())
                    .build();
        } catch (Exception e) {
            LOGGER.warn("Error occurred while building Google CloudResourceManager access.", e);
            throw new CredentialVerificationException("Error occurred while building Google CloudResourceManager access.", e);
        }
    }
}
