package com.sequenceiq.cloudbreak.service.stack.connector;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import freemarker.template.TemplateException;

public class UserDataBuilderTest {

    private static UserDataBuilder userDataBuilder = new UserDataBuilder();

    @Test
    public void testBuildUserDataEc2() throws IOException, TemplateException {
        userDataBuilder.setUserDataScripts(FileReaderUtils.readFileFromClasspath(String.format("ec2-init-test.sh")));
        String expectedScript = FileReaderUtils.readFileFromClasspath("ec2-init-test-expected.sh");
        Assert.assertEquals(expectedScript, userDataBuilder.buildUserData(CloudPlatform.AWS));
    }

    @Test
    public void testBuildUserDataAzure() throws IOException, TemplateException {
        userDataBuilder.setUserDataScripts(FileReaderUtils.readFileFromClasspath(String.format("azure-init-test.sh")));
        String expectedScript = FileReaderUtils.readFileFromClasspath("azure-init-test-expected.sh");
        Assert.assertEquals(expectedScript, userDataBuilder.buildUserData(CloudPlatform.AZURE));
    }

    @Test
    public void testBuildUserDataGcc() throws IOException, TemplateException {
        userDataBuilder.setUserDataScripts(FileReaderUtils.readFileFromClasspath(String.format("gcc-init-test.sh")));
        String expectedScript = FileReaderUtils.readFileFromClasspath("gcc-init-test-expected.sh");
        Assert.assertEquals(expectedScript, userDataBuilder.buildUserData(CloudPlatform.GCC));
    }

}
