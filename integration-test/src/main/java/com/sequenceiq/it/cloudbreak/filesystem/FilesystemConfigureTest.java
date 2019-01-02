package com.sequenceiq.it.cloudbreak.filesystem;

import static com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants.FSREQUEST;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls.AdlsCloudStorageParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.gcs.GcsCloudStorageParameters;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class FilesystemConfigureTest extends AbstractCloudbreakIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesystemConfigureTest.class);

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class),
                "Cloudprovider parameters are mandatory.");
    }

    @Test
    @Parameters({"filesystemType", "fsName"})
    public void testFilesystemConfigure(String filesystemType, String fsName) {
        //GIVEN
        IntegrationTestContext itContext = getItContext();
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);
        // WHEN
        FileSystemRequest fsRequest = new FileSystemRequest();
        fsRequest = createRequestProperties(cloudProviderParams, fsName, fsRequest, filesystemType);
        fsRequest.setName("it-fs");
        fsRequest.setDefaultFs(false);
        // THEN
        getItContext().putContextParam(FSREQUEST, fsRequest);
    }

    protected static FileSystemRequest createRequestProperties(Map<String, String> cloudProviderParams, String fsName,
            FileSystemRequest fsRequest, String filesystemType) {
        switch (cloudProviderParams.get("cloudProvider")) {
            case "AZURE":
                AdlsCloudStorageParameters adlsCloudStorageParameters = new AdlsCloudStorageParameters();
                adlsCloudStorageParameters.setAccountName(fsName);
                adlsCloudStorageParameters.setClientId(cloudProviderParams.get("accountKeyWasb"));
                adlsCloudStorageParameters.setTenantId(cloudProviderParams.get("tenantId"));
                fsRequest.setAdls(adlsCloudStorageParameters);
                fsRequest.setType(filesystemType);
                break;
            case "GCP":
                GcsCloudStorageParameters gcsCloudStorageParameters = new GcsCloudStorageParameters();
                gcsCloudStorageParameters.setServiceAccountEmail(cloudProviderParams.get("serviceAccountId"));
                fsRequest.setGcs(gcsCloudStorageParameters);
                fsRequest.setType(filesystemType);
                break;
            default:
                LOGGER.info("CloudProvider {} is not supported!", cloudProviderParams.get("cloudProvider"));
                break;
            }
        return fsRequest;
    }
}