package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;

public class AwsHadoopConfigurationProviderTest {

    @InjectMocks
    private AwsHadoopConfigurationProvider underTest;

    private User user;
    private AwsCredential credential;
    private Set<Resource> resources = new HashSet<>();

    @Before
    public void setUp() {
        underTest = new AwsHadoopConfigurationProvider();
        MockitoAnnotations.initMocks(this);
        user = AwsConnectorTestUtil.createUser();
        credential = AwsConnectorTestUtil.createAwsCredential();
    }

    @Test
    public void testGetYarnSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate(user);
        Stack stack = AwsConnectorTestUtil.createStack(user, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, String> result = underTest.getYarnSiteConfigs(stack);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationProvider.YARN_NODEMANAGER_LOCAL_DIRS));
    }

    @Test
    public void testGetYarnSiteConfigsShouldReturnNotReturnYarnLocalDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplateWithZeroVolumes(user);
        Stack stack = AwsConnectorTestUtil.createStack(user, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, String> result = underTest.getYarnSiteConfigs(stack);
        // THEN
        assertTrue(!result.containsKey(HadoopConfigurationProvider.YARN_NODEMANAGER_LOCAL_DIRS));
    }

    @Test
    public void testGetHdfsSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate(user);
        Stack stack = AwsConnectorTestUtil.createStack(user, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, String> result = underTest.getHdfsSiteConfigs(stack);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationProvider.HDFS_DATANODE_DATA_DIRS));
    }

    @Test
    public void testGetHdfsSiteConfigsShouldNotReturnHdfsDatanodeDataDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplateWithZeroVolumes(user);
        Stack stack = AwsConnectorTestUtil.createStack(user, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, String> result = underTest.getHdfsSiteConfigs(stack);
        // THEN
        assertTrue(!result.containsKey(HadoopConfigurationProvider.HDFS_DATANODE_DATA_DIRS));
    }

}
