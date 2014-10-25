package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.LocalDirBuilderService;
import com.sequenceiq.cloudbreak.service.stack.connector.aws.AwsConnectorTestUtil;

@RunWith(MockitoJUnitRunner.class)
public class HadoopConfigurationServiceTest {

    @InjectMocks
    private HadoopConfigurationService underTest;

    @Mock
    private LocalDirBuilderService localDirBuilderService;

    private AwsCredential credential = AwsConnectorTestUtil.createAwsCredential();
    private Set<Resource> resources = new HashSet<>();

    @Test
    public void testGetYarnSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        given(localDirBuilderService.buildLocalDirs(anyInt())).willReturn("/mnt/fs1,/mnt/fs2");
        Map<String, String> result = underTest.getConfiguration(stack).get(HadoopConfigurationService.YARN_SITE);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationService.YARN_NODEMANAGER_LOCAL_DIRS));
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationService.YARN_NODEMANAGER_LOG_DIRS));
    }

    @Test
    public void testGetYarnSiteConfigsShouldReturnNotReturnYarnLocalDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplateWithZeroVolumes();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, Map<String, String>> configuration = underTest.getConfiguration(stack);
        // THEN
        assertEquals(0, configuration.size());
    }

    @Test
    public void testGetHdfsSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        given(localDirBuilderService.buildLocalDirs(anyInt())).willReturn("/mnt/fs1,/mnt/fs2");
        Map<String, String> result = underTest.getConfiguration(stack).get(HadoopConfigurationService.HDFS_SITE);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationService.HDFS_DATANODE_DATA_DIRS));
    }

    @Test
    public void testGetHdfsSiteConfigsShouldNotReturnHdfsDatanodeDataDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplateWithZeroVolumes();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, Map<String, String>> configuration = underTest.getConfiguration(stack);
        // THEN
        assertEquals(0, configuration.size());
    }

}
