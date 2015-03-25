package com.sequenceiq.cloudbreak.service.stack.connector;

import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.GATEWAY;
import static com.sequenceiq.cloudbreak.domain.InstanceGroupType.HOSTGROUP;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class UserDataBuilderTest {

    private static UserDataBuilder userDataBuilder;

    @BeforeClass
    public static void before() throws IOException {
        Map<CloudPlatform, Map<InstanceGroupType, String>> userDataScripts = new HashMap<>();
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            Map<InstanceGroupType, String> tmpMap = new HashMap<>();
            tmpMap.put(HOSTGROUP, FileReaderUtils.readFileFromClasspath(String.format("%s-init-test.sh", cloudPlatform.getInitScriptPrefix())));
            tmpMap.put(GATEWAY, FileReaderUtils.readFileFromClasspath(String.format("%s-init-test.sh", cloudPlatform.getInitScriptPrefix())));
            userDataScripts.put(cloudPlatform, tmpMap);
        }
        userDataBuilder = new UserDataBuilder();
        userDataBuilder.setUserDataScripts(userDataScripts);
        userDataBuilder.setHostAddress("http://cloudbreak.sequenceiq.com");
        userDataBuilder.setAmbariDockerTag("1.7.0-consul");
    }

    @Test
    public void testBuildUserDataEc2() throws IOException {
        String expectedScript = FileReaderUtils.readFileFromClasspath("ec2-init-test-expected.sh");
        Map<String, String> map = new HashMap<>();
        map.put("NODE_PREFIX", "testamb");
        map.put("MYDOMAIN", "test.kom");
        Assert.assertEquals(expectedScript, userDataBuilder.buildUserData(CloudPlatform.AWS, "hash123", 3, map, InstanceGroupType.HOSTGROUP));
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedScript = FileReaderUtils.readFileFromClasspath("azure-init-test-expected.sh");
        Map<String, String> map = new HashMap<>();
        map.put("NODE_PREFIX", "testamb");
        map.put("MYDOMAIN", "test.kom");
        Assert.assertEquals(expectedScript, userDataBuilder.buildUserData(CloudPlatform.AZURE, "hash123", 3, map, InstanceGroupType.HOSTGROUP));
    }

    @Test
    public void testBuildUserDataGcc() throws IOException {
        String expectedScript = FileReaderUtils.readFileFromClasspath("gcc-init-test-expected.sh");
        Map<String, String> map = new HashMap<>();
        map.put("NODE_PREFIX", "testamb");
        map.put("MYDOMAIN", "test.kom");
        Assert.assertEquals(expectedScript, userDataBuilder.buildUserData(CloudPlatform.GCC, "hash123", 3, map, InstanceGroupType.HOSTGROUP));
    }

}
