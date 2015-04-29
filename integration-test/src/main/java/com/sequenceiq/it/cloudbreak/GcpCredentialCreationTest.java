package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

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
    @Value("${integrationtest.gcpcredential.publicKeyFile}")
    private String defaultPublicKeyFile;

    @Test
    @Parameters({ "name", "projectId", "serviceAccountId", "serviceAccountPrivateKeyP12File", "publicKeyFile" })
    public void testGCPCredentialCreation(@Optional("")String name, @Optional("")String projectId, @Optional("")String serviceAccountId,
            @Optional("")String serviceAccountPrivateKeyP12File, @Optional("")String publicKeyFile) throws Exception {
        // GIVEN
        name = StringUtils.hasLength(name) ? name : defaultName;
        projectId = StringUtils.hasLength(projectId) ? projectId : defaultProjectId;
        serviceAccountId = StringUtils.hasLength(serviceAccountId) ? serviceAccountId : defaultServiceAccountId;
        serviceAccountPrivateKeyP12File = StringUtils.hasLength(serviceAccountPrivateKeyP12File) ? serviceAccountPrivateKeyP12File : defaultP12File;
        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, serviceAccountPrivateKeyP12File);
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        // WHEN
        // TODO publicInAccount
        String id = getClient().postGccCredential(name, "GCP credential for integartiontest", publicKey, false, projectId, serviceAccountId,
                serviceAccountPrivateKey);
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
