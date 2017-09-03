package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.it.util.ResourceUtil;

public class GcpCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.gcpcredential.name}")
    private String defaultName;

    @Value("${integrationtest.gcpcredential.projectId}")
    private String defaultProjectId;

    @Value("${integrationtest.gcpcredential.serviceAccountId}")
    private String defaultServiceAccountId;

    @Value("${integrationtest.gcpcredential.p12File}")
    private String defaultP12File;

    @Test
    @Parameters({ "credentialName", "projectId", "serviceAccountId", "serviceAccountPrivateKeyP12File" })
    public void testGCPCredentialCreation(@Optional("")String credentialName, @Optional("")String projectId, @Optional("")String serviceAccountId,
            @Optional("")String serviceAccountPrivateKeyP12File) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        projectId = StringUtils.hasLength(projectId) ? projectId : defaultProjectId;
        serviceAccountId = StringUtils.hasLength(serviceAccountId) ? serviceAccountId : defaultServiceAccountId;
        serviceAccountPrivateKeyP12File = StringUtils.hasLength(serviceAccountPrivateKeyP12File) ? serviceAccountPrivateKeyP12File : defaultP12File;
        String serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, serviceAccountPrivateKeyP12File);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setCloudPlatform("GCP");
        credentialRequest.setDescription("GCP credential for integartiontest");
        credentialRequest.setName(credentialName);
        Map<String, Object> map = new HashMap<>();
        map.put("projectId", projectId);
        map.put("serviceAccountId", serviceAccountId);
        map.put("serviceAccountPrivateKey", serviceAccountPrivateKey);
        credentialRequest.setParameters(map);
        // WHEN
        String id = getCloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
