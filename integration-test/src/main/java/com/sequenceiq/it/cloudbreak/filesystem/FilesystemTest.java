package com.sequenceiq.it.cloudbreak.filesystem;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.SshUtil;
import com.sequenceiq.it.util.ResourceUtil;


public class FilesystemTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    private final Map<String, String> fsParams = new HashMap<>();

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class),
                "Cloudprovider parameters are mandatory.");
    }

    @Test
    @Parameters({"filesystemType", "filesystemName", "folderPrefix", "wasbContainerName", "sshCommand", "sshUser", "sshChecker"})
    public void testFileSystem(String filesystemType, String filesystemName, String folderPrefix, @Optional("it-container") String wasbContainerName,
            String sshCommand, @Optional("cloudbreak") String sshUser, String sshChecker) throws IOException, GeneralSecurityException {
        //GIVEN
        Assert.assertEquals(new File(defaultPrivateKeyFile).exists(), true, "Private cert file not found: " + defaultPrivateKeyFile);
        fsParams.put("filesystemType", filesystemType);
        fsParams.put("filesystemName", filesystemName);
        fsParams.put("folderPrefix", folderPrefix);
        fsParams.put("wasbContainerName", wasbContainerName);

        IntegrationTestContext itContext = getItContext();
        String stackId  = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);

        StackV1Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackEndpoint();

        String masterIp = CloudbreakUtil.getAmbariIp(stackV1Endpoint, stackId, itContext);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);

        sshCommand = ResourceUtil.readStringFromResource(applicationContext, sshCommand.replaceAll("\n", ""));

        if ("WASB".equals(filesystemType)) {
            FilesystemUtil.createWasbContainer(cloudProviderParams, filesystemName, wasbContainerName);
        }

        //WHEN
        boolean sshResult = SshUtil.executeCommand(masterIp, defaultPrivateKeyFile, sshCommand, sshUser, SshUtil.getSshCheckMap(sshChecker));

        //THEN
        Assert.assertTrue(sshResult, "Ssh command executing was not successful");
    }

    @AfterTest
    public void cleanUpFilesystem() throws Exception {
        IntegrationTestContext itContext = getItContext();
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);
        FilesystemUtil.cleanUpFiles(applicationContext, cloudProviderParams, fsParams.get("filesystemType"), fsParams.get("filesystemName"),
                fsParams.get("folderPrefix"), fsParams.get("wasbContainerName"));
    }
}