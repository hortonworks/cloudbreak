package com.sequenceiq.cloudbreak.init.clustertemplate;

import static com.sequenceiq.cloudbreak.common.json.JsonUtil.writeValueAsStringSilent;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;

@RunWith(MockitoJUnitRunner.class)
public class ClusterTemplateLoaderServiceTest {

    @InjectMocks
    private ClusterTemplateLoaderService underTest;

    @Mock
    private DefaultClusterTemplateCache defaultClusterTemplateCache;

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenNoDefaultClusterTemplateAndNoDefaultInDB() {
        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser()).thenReturn(emptyMap());

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(emptyList());

        assertThat(actual, is(false));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenNoDefaultClusterTemplateAndHasDefaultInDB() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setStatus(ResourceStatus.DEFAULT);
        clusterTemplate.setName("cluster-template");

        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser()).thenReturn(emptyMap());

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplate));

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenHasDefaultClusterTemplateAndNoDefaultInDB() {
        DefaultClusterTemplateV4Request clusterTemplate = new DefaultClusterTemplateV4Request();
        clusterTemplate.setName("cluster-template");

        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser())
                .thenReturn(singletonMap(clusterTemplate.getName(), encode(clusterTemplate)));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(emptyList());

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenTemplateContentsAreSame() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault = sameClusterTemplateRequest();
        ClusterTemplate clusterTemplate = sameClusterTemplate();
        clusterTemplate.setTemplateContent(Base64Util.encode(writeValueAsStringSilent(clusterTemplateFromDefault)));

        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser())
                .thenReturn(singletonMap(clusterTemplateFromDefault.getName(), encode(clusterTemplateFromDefault)));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplate));

        assertThat(actual, is(false));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenTemplateContentsAreSameButCrnIsNull() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault = sameClusterTemplateRequest();
        ClusterTemplate clusterTemplate = sameClusterTemplate();
        clusterTemplate.setResourceCrn(null);
        clusterTemplate.setTemplateContent(Base64Util.encode(writeValueAsStringSilent(clusterTemplateFromDefault)));

        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser())
                .thenReturn(singletonMap(clusterTemplateFromDefault.getName(), encode(clusterTemplateFromDefault)));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplate));

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenTemplateContentsAreNotSame() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault = clusterTemplateRequest("cluster-template");
        ClusterTemplate clusterTemplateFromDB = clusterTemplate("cluster-template", "gcp", "hostgroup2", "worker");

        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser())
                .thenReturn(singletonMap(clusterTemplateFromDefault.getName(), encode(clusterTemplateFromDefault)));

        boolean actual = underTest.isDefaultClusterTemplateUpdateNecessaryForUser(singleton(clusterTemplateFromDB));

        assertThat(actual, is(true));
    }

    @Test
    public void testIsDefaultClusterTemplateUpdateNecessaryForUserWhenOneTemplateContentsAreSameAndOtherOneTemplateContentsAreNotSame() {
        DefaultClusterTemplateV4Request clusterTemplateFromDefault1 = sameClusterTemplateRequest();
        ClusterTemplate clusterTemplateFromDB1 = sameClusterTemplate();
        DefaultClusterTemplateV4Request clusterTemplateFromDefault2 = clusterTemplateRequest("cluster-template2");
        ClusterTemplate clusterTemplateFromDB2 = clusterTemplate("cluster-template2", "gcp", "hostgroup2", "worker");

        when(defaultClusterTemplateCache.defaultClusterTemplateRequestsForUser()).thenReturn(
                Map.of(clusterTemplateFromDefault1.getName(), encode(clusterTemplateFromDefault1),
                        clusterTemplateFromDefault2.getName(), encode(clusterTemplateFromDefault2)));

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
        clusterTemplate.setResourceCrn("crn");
        clusterTemplate.setFeatureState(FeatureState.PREVIEW);
        return clusterTemplate;
    }

    private DefaultClusterTemplateV4Request sameClusterTemplateRequest() {
        return clusterTemplateRequest("cluster-template");
    }

    private DefaultClusterTemplateV4Request clusterTemplateRequest(String templateName) {
        DefaultClusterTemplateV4Request clusterTemplate = new DefaultClusterTemplateV4Request();
        clusterTemplate.setName(templateName);
        DistroXV1Request distrox = new DistroXV1Request();
        DistroXClusterV1Request cluster = new DistroXClusterV1Request();
        distrox.setCluster(cluster);
        InstanceGroupV1Request instanceGroup = new InstanceGroupV1Request();
        instanceGroup.setName("master");
        distrox.setInstanceGroups(singleton(instanceGroup));
        clusterTemplate.setDistroXTemplate(distrox);
        clusterTemplate.setFeatureState(FeatureState.PREVIEW);
        return clusterTemplate;
    }

    private String encode(DefaultClusterTemplateV4Request s) {
        return new String(Base64Util.encode(writeValueAsStringSilent(s)));
    }
}
