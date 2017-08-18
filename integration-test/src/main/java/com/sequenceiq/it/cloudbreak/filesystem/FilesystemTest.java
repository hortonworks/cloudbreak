package com.sequenceiq.it.cloudbreak.filesystem;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
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

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class),
                "Cloudprovider parameters are mandatory.");
    }

    @Test
    @Parameters({"filesystemType", "filesystemName", "sshCommand", "folderPrefix", "wasbContainerName"})
    public void testFileSystem(String filesystemType, String filesystemName, String sshCommand, @ Optional("it-terasort") String folderPrefix,
            @Optional("it-container") String wasbContainerName) throws Exception {
        //GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        StackEndpoint stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackEndpoint();

        String masterIp = CloudbreakUtil.getAmbariIp(stackEndpoint, stackId, itContext);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);

        sshCommand = ResourceUtil.readStringFromResource(applicationContext, sshCommand.replaceAll("\n", ""));

        if ("WASB".equals(filesystemType)) {
            FilesystemUtil.createWasbContainer(cloudProviderParams, filesystemName, wasbContainerName);
        }
//        //WHEN
        SshUtil mySshUtil = new SshUtil();
        Boolean sshResult =  mySshUtil.ssh(masterIp, defaultPrivateKeyFile, sshCommand, "notContains", "Container killed on request");
        //THEN
        Assert.assertTrue(sshResult,  "Ssh command executing was not successful");
        FilesystemUtil.cleanUpFiles(applicationContext, cloudProviderParams, filesystemType, filesystemName, folderPrefix, wasbContainerName);
    }
}




