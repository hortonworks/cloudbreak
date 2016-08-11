package com.sequenceiq.it.cloudbreak;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.Collections;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.it.util.ResourceUtil;

public class GcpDeleteVpcTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.gcpcredential.name}")
    private String defaultName;
    @Value("${integrationtest.gcpcredential.projectId}")
    private String defaultProjectId;
    @Value("${integrationtest.gcpcredential.serviceAccountId}")
    private String defaultServiceAccountId;
    @Value("${integrationtest.gcpcredential.p12File}")
    private String defaultP12File;
    private JacksonFactory jsonFactory;

    @AfterSuite
    @Parameters({ "vpcName" })
    public void deleteNetwork(@Optional("it-vpc") String vpcName) throws Exception {
        springTestContextPrepareTestInstance();
        String serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, defaultP12File);
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                new ByteArrayInputStream(Base64.decodeBase64(serviceAccountPrivateKey)), "notasecret", "privatekey", "notasecret");
        jsonFactory = JacksonFactory.getDefaultInstance();
        GoogleCredential googleCredential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(defaultServiceAccountId)
                .setServiceAccountScopes(Collections.singletonList(ComputeScopes.COMPUTE))
                .setServiceAccountPrivateKey(privateKey)
                .build();

        Compute compute = new Compute.Builder(httpTransport, jsonFactory, null)
                .setApplicationName(defaultName)
                .setHttpRequestInitializer(googleCredential)
                .build();

        Compute.Networks.Delete delete = compute.networks().delete(defaultProjectId, vpcName);

        Operation operation = delete.execute();
        if (operation.getHttpErrorStatusCode() != null) {
            throw new IllegalStateException("gcp operation failed: " + operation.getHttpErrorMessage());
        }
    }
}