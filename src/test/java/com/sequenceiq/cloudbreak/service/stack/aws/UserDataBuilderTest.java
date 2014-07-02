package com.sequenceiq.cloudbreak.service.stack.aws;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.UserDataBuilder;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class UserDataBuilderTest {

    @Test
    public void testBuildUserDataEc2() throws IOException {
        String expectedScript = FileReaderUtils.readFileFromClasspath("ec2-init-test-expected.sh");
        String result = UserDataBuilder.builder()
                .withCloudPlatform(CloudPlatform.AWS)
                .withEnvironmentVariable("NODE_PREFIX", "testamb")
                .withEnvironmentVariable("MYDOMAIN", "test.kom")
                .build();
        Assert.assertEquals(expectedScript, result);
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedScript = FileReaderUtils.readFileFromClasspath("azure-init-test-expected.sh");
        String result = UserDataBuilder.builder()
                .withCloudPlatform(CloudPlatform.AZURE)
                .withEnvironmentVariable("NODE_PREFIX", "testamb")
                .withEnvironmentVariable("MYDOMAIN", "test.kom")
                .build();
        Assert.assertEquals(expectedScript, result);
    }

}
