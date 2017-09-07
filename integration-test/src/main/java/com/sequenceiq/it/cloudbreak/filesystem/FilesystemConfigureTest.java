package com.sequenceiq.it.cloudbreak.filesystem;

import static com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants.FSREQUEST;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
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
        fsRequest.setProperties(createRequestProperties(cloudProviderParams, fsName));
        fsRequest.setType(FileSystemType.valueOf(filesystemType));
        fsRequest.setName("it-fs");
        fsRequest.setDefaultFs(false);
        // THEN
        getItContext().putContextParam(FSREQUEST, fsRequest);
    }

    protected static Map<String, String> createRequestProperties(Map<String, String> cloudProviderParams, String fsName) {
        Map<String, String> requestProperties = new HashMap<>();
        switch (cloudProviderParams.get("cloudProvider")) {
            case "AZURE":
                requestProperties.put("accountName", fsName);
                requestProperties.put("accountKey", cloudProviderParams.get("accountKeyWasb"));
                requestProperties.put("tenantId", cloudProviderParams.get("tenantId"));
                break;
            case "GCP":
                requestProperties.put("projectId", cloudProviderParams.get("projectId"));
                requestProperties.put("serviceAccountEmail", cloudProviderParams.get("serviceAccountId"));
                requestProperties.put("privateKeyEncoded", cloudProviderParams.get("p12File"));
                requestProperties.put("defaultBucketName", fsName);
                break;
            default:
                LOGGER.info("CloudProvider {} is not supported!", cloudProviderParams.get("cloudProvider"));
                break;
            }
        return requestProperties;
    }
}