package com.sequenceiq.cloudbreak.service.stack.aws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class Ec2UserDataBuilderTest {

    private Ec2UserDataBuilder underTest = new Ec2UserDataBuilder();

    @Before
    public void setTemplate() throws IOException {
        underTest.setEc2userDataScript(FileReaderUtils.readFileFromClasspath("ec2-init-test-raw.sh"));
    }

    @Test
    public void testBuildUserData() throws IOException {
        String expectedScript = FileReaderUtils.readFileFromClasspath("ec2-init-test-expected.sh");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("NODE_PREFIX", "testamb");
        parameters.put("MYDOMAIN", "test.kom");

        String result = underTest.buildUserData(parameters);

        Assert.assertEquals(expectedScript, result);
    }

}
