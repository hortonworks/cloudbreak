package com.sequenceiq.cloudbreak.cloud.gcp.client;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.sqladmin.SQLAdmin;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class GcpSQLAdminFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpSQLAdminFactory.class);

    @Inject
    private JsonFactory jsonFactory;

    @Inject
    private GcpCredentialFactory gcpCredentialFactory;

    public SQLAdmin buildSQLAdmin(CloudCredential gcpCredential, String name) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = gcpCredentialFactory.buildCredential(gcpCredential, httpTransport);
            return new SQLAdmin.Builder(
                    httpTransport, jsonFactory, null)
                    .setApplicationName(name)
                    .setHttpRequestInitializer(credential)
                    .build();
        } catch (Exception e) {
            LOGGER.warn("Error occurred while building Google Storage access.", e);
        }
        return null;
    }
}
