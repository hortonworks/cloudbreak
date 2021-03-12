package com.sequenceiq.cloudbreak.cloud.gcp.client;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.cloudkms.v1.CloudKMS;
import com.google.api.services.cloudkms.v1.CloudKMSScopes;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Service
public class GcpCloudKMSFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpCloudKMSFactory.class);

    @Inject
    private JsonFactory jsonFactory;

    @Inject
    private GcpCredentialFactory gcpCredentialFactory;

    @Inject
    private ApacheHttpTransport gcpApacheHttpTransport;

    public CloudKMS buildCloudKMS(CloudCredential cloudCredential) throws GeneralSecurityException, IOException {
        GoogleCredential credential = gcpCredentialFactory.buildCredential(cloudCredential, gcpApacheHttpTransport);
        if (credential.createScopedRequired()) {
            credential = credential.createScoped(CloudKMSScopes.all());
        }

        return new CloudKMS.Builder(gcpApacheHttpTransport, jsonFactory, credential)
                .setApplicationName(cloudCredential.getName())
                .build();
    }
}
