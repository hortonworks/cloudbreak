package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.P12Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
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
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setCloudPlatform("GCP");
        credentialRequest.setDescription("GCP credential for integartiontest");
        credentialRequest.setName(credentialName);
        GcpCredentialV4Parameters credentialParameters = new GcpCredentialV4Parameters();
        P12Parameters p12Parameters = new P12Parameters();
        p12Parameters.setProjectId(projectId);
        p12Parameters.setServiceAccountId(serviceAccountId);
        p12Parameters.setServiceAccountPrivateKey(serviceAccountPrivateKey);
        credentialParameters.setP12(p12Parameters);
        credentialRequest.setGcp(credentialParameters);
        // WHEN
        Long id = getCloudbreakClient().credentialV4Endpoint().post(1L, credentialRequest).getId();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
