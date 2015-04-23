package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private HadoopConfigurationService underTest;

    private AwsCredential credential = (AwsCredential) ServiceTestUtils.createCredential(CloudPlatform.AWS);
    private Set<Resource> resources = new HashSet<>();

    @Test
    @Ignore
    public void testGetYarnSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        Stack stack = ServiceTestUtils.createStack(template, credential, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack, "master"));
        // WHEN
        Map<String, Map<String, String>> result = underTest.getConfiguration(stack).get(HadoopConfigurationService.YARN_SITE);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationService.YARN_NODEMANAGER_LOCAL_DIRS));
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationService.YARN_NODEMANAGER_LOG_DIRS));
    }

    @Test
    @Ignore
    public void testGetYarnSiteConfigsShouldReturnNotReturnYarnLocalDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        Stack stack = ServiceTestUtils.createStack(template, credential, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack, "master"));
        // WHEN
        Map<String, Map<String, Map<String, String>>> configuration = underTest.getConfiguration(stack);
        // THEN
        assertEquals(0, configuration.size());
    }

    @Test
    @Ignore
    public void testGetHdfsSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        Stack stack = ServiceTestUtils.createStack(template, credential, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack, "master"));
        // WHEN
        Map<String, Map<String, String>> result = underTest.getConfiguration(stack).get(HadoopConfigurationService.HDFS_SITE);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationService.HDFS_DATANODE_DATA_DIRS));
    }

    @Test
    @Ignore
    public void testGetHdfsSiteConfigsShouldNotReturnHdfsDatanodeDataDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        Template template = ServiceTestUtils.createTemplate(CloudPlatform.AWS);
        Stack stack = ServiceTestUtils.createStack(template, credential, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack, "master"));
        // WHEN
        Map<String, Map<String, Map<String, String>>> configuration = underTest.getConfiguration(stack);
        // THEN
        assertEquals(0, configuration.size());
    }

}
