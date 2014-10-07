package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;
import com.sequenceiq.cloudbreak.service.stack.connector.LocalDirBuilderService;

public class AwsHadoopConfigurationProviderTest {

    @InjectMocks
    private AwsHadoopConfigurationProvider underTest;

    @Mock
    private LocalDirBuilderService localDirBuilderService;

    private String owner;
    private AwsCredential credential;
    private Set<Resource> resources = new HashSet<>();

    @Before
    public void setUp() {
        underTest = new AwsHadoopConfigurationProvider();
        MockitoAnnotations.initMocks(this);
        credential = AwsConnectorTestUtil.createAwsCredential();
    }

    @Test
    public void testGetYarnSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        given(localDirBuilderService.buildLocalDirs(anyInt())).willReturn("/mnt/fs1,/mnt/fs2");
        Map<String, String> result = underTest.getYarnSiteConfigs(stack);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationProvider.YARN_NODEMANAGER_LOCAL_DIRS));
    }

    @Test
    public void testGetYarnSiteConfigsShouldReturnNotReturnYarnLocalDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplateWithZeroVolumes();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, String> result = underTest.getYarnSiteConfigs(stack);
        // THEN
        assertTrue(!result.containsKey(HadoopConfigurationProvider.YARN_NODEMANAGER_LOCAL_DIRS));
    }

    @Test
    public void testGetHdfsSiteConfigsShouldReturnDirectoriesProperlyWithTwoVolumes() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplate();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        given(localDirBuilderService.buildLocalDirs(anyInt())).willReturn("/mnt/fs1,/mnt/fs2");
        Map<String, String> result = underTest.getHdfsSiteConfigs(stack);
        // THEN
        assertEquals("/mnt/fs1,/mnt/fs2", result.get(HadoopConfigurationProvider.HDFS_DATANODE_DATA_DIRS));
    }

    @Test
    public void testGetHdfsSiteConfigsShouldNotReturnHdfsDatanodeDataDirsPropertyWhenThereAreNoVolumesAttached() {
        // GIVEN
        AwsTemplate template = AwsConnectorTestUtil.createAwsTemplateWithZeroVolumes();
        Stack stack = AwsConnectorTestUtil.createStack(AwsConnectorTestUtil.DUMMY_OWNER, AwsConnectorTestUtil.DUMMY_ACCOUNT, credential, template, resources);
        resources.add(new Resource(ResourceType.CLOUDFORMATION_STACK, "", stack));
        // WHEN
        Map<String, String> result = underTest.getHdfsSiteConfigs(stack);
        // THEN
        assertTrue(!result.containsKey(HadoopConfigurationProvider.HDFS_DATANODE_DATA_DIRS));
    }

}
