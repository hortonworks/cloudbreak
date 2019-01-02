package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.util.JsonUtil.writeValueAsStringSilent;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ClusterTemplateLoaderServiceTest {

    @InjectMocks
    private ClusterTemplateLoaderService underTest;

    @Mock
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenNoDefaultClusterTemplateAndNoDefaultInDB() {
        Mockito.when(defaultClusterTemplateCache.defaultClusterTemplateRequests()).thenReturn(emptyMap());

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(emptyList());

        assertThat(actual, is(false));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenNoDefaultClusterTemplateAndHasDefaultInDB() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        clusterTemplate.setName("cluster-template");

        Mockito.when(defaultClusterTemplateCache.defaultClusterTemplateRequests()).thenReturn(emptyMap());

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplate));

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenHasDefaultClusterTemplateAndNoDefaultInDB() {
        DefaultClusterTemplateV4Request clusterTemplate = new DefaultClusterTemplateV4Request();
        clusterTemplate.setName("cluster-template");

        Mockito.when(defaultClusterTemplateCache.defaultClusterTemplateRequests()).thenReturn(singletonMap(clusterTemplate.getName(), clusterTemplate));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(emptyList());

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenTemplateContentsAreSame() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault = sameClusterTemplateRequest();
        ClusterTemplate clusterTemplate = sameClusterTemplate();
        clusterTemplate.setTemplateContent(Base64.getEncoder().encodeToString(writeValueAsStringSilent(clusterTemplateFromDefault).getBytes()));

        Mockito.when(defaultClusterTemplateCache.defaultClusterTemplateRequests())
                .thenReturn(singletonMap(clusterTemplateFromDefault.getName(), clusterTemplateFromDefault));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplate));

        assertThat(actual, is(false));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenTemplateContentsAreNotSame() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault = clusterTemplateRequest("cluster-template");
        ClusterTemplate clusterTemplateFromDB = clusterTemplate("cluster-template", "gcp", "hostgroup2", "worker");

        Mockito.when(defaultClusterTemplateCache.defaultClusterTemplateRequests())
                .thenReturn(singletonMap(clusterTemplateFromDefault.getName(), clusterTemplateFromDefault));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplateFromDB));

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenOneTemplateContentsAreSameAndOtherOneTemplateContentsAreNotSame() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault1 = sameClusterTemplateRequest();
        ClusterTemplate clusterTemplateFromDB1 = sameClusterTemplate();
        DefaultClusterTemplateV4Request clusterTemplateFromDefault2 = clusterTemplateRequest("cluster-template2");
        ClusterTemplate clusterTemplateFromDB2 = clusterTemplate("cluster-template2", "gcp", "hostgroup2", "worker");

        Mockito.when(defaultClusterTemplateCache.defaultClusterTemplateRequests()).thenReturn(
                Map.of(clusterTemplateFromDefault1.getName(), clusterTemplateFromDefault1, clusterTemplateFromDefault2.getName(), clusterTemplateFromDefault2));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(Set.of(clusterTemplateFromDB1, clusterTemplateFromDB2));

        assertThat(actual, is(true));
    }

    private ClusterTemplate sameClusterTemplate() {
        return clusterTemplate("cluster-template", "aws", "hostgroup", "master");
    }

    private ClusterTemplate clusterTemplate(String templateName, String cloudPlatform, String hostgroupName, String instanceGroupName) {
        Workspace workspace = new Workspace();
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        clusterTemplate.setName(templateName);
        Stack stack = new Stack();
        stack.setWorkspace(workspace);
        stack.setCloudPlatform(cloudPlatform);
        Cluster cluster = new Cluster();
        cluster.setWorkspace(workspace);
        HostGroup hostGroup = new HostGroup();
        hostGroup.setName(hostgroupName);
        cluster.setHostGroups(singleton(hostGroup));
        stack.setCluster(cluster);
        stack.setPlatformVariant("VARIANT");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(instanceGroupName);
        stack.setInstanceGroups(singleton(instanceGroup));
        clusterTemplate.setStackTemplate(stack);
        clusterTemplate.setWorkspace(workspace);
        return clusterTemplate;
    }

    private DefaultClusterTemplateV4Request sameClusterTemplateRequest() {
        return clusterTemplateRequest("cluster-template");
    }

    private DefaultClusterTemplateV4Request clusterTemplateRequest(String templateName) {
        DefaultClusterTemplateV4Request clusterTemplate = new DefaultClusterTemplateV4Request();
        clusterTemplate.setName(templateName);
        StackV2Request stack = new StackV2Request();
        ClusterV2Request cluster = new ClusterV2Request();
        stack.setCluster(cluster);
        stack.setPlatformVariant("VARIANT");
        InstanceGroupV2Request instanceGroup = new InstanceGroupV2Request();
        instanceGroup.setGroup("master");
        stack.setInstanceGroups(singletonList(instanceGroup));
        clusterTemplate.setStackTemplate(stack);
        return clusterTemplate;
    }
}
