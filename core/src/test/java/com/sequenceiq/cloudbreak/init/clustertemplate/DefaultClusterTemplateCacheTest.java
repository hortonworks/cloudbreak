package com.sequenceiq.cloudbreak.init.clustertemplate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.DefaultClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintEntities;

@RunWith(MockitoJUnitRunner.class)
public class DefaultClusterTemplateCacheTest {

    @InjectMocks
    private DefaultClusterTemplateCache underTest;

    @Mock
    private DefaultClusterTemplateV4RequestToClusterTemplateConverter defaultClusterTemplateV4RequestToClusterTemplateConverter;

    @Mock
    private BlueprintEntities blueprintEntities;

    @Before
    public void setUp() throws Exception {
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/");
    }

    @Test
    public void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesProvided() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setName("template");
        when(defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(any())).thenReturn(clusterTemplate);

        underTest.setClusterTemplates(Collections.singletonList("default-template.json"));
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        assertThat(actual.size(), is(1));
    }

    @Test
    public void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesProvidedButDirNotExists() {
        underTest.setClusterTemplates(Collections.singletonList("default-template"));
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/notexists");
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesProvidedButFileNotExists() {
        underTest.setClusterTemplates(Collections.singletonList("default-template-not-exists"));
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesNotProvided() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setName("cluster-template");
        ClusterTemplate clusterTemplate2 = new ClusterTemplate();
        clusterTemplate2.setName("cluster-template2");
        ClusterTemplate clusterTemplateAws = new ClusterTemplate();
        clusterTemplateAws.setName("cluster-template-aws");
        ClusterTemplate clusterTemplateAzure = new ClusterTemplate();
        clusterTemplateAzure.setName("cluster-template-azure");
        ClusterTemplate clusterTemplateAwsRanger = new ClusterTemplate();
        clusterTemplateAwsRanger.setName("cluster-template-aws-ranger");
        when(defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(any(DefaultClusterTemplateV4Request.class)))
                .thenReturn(clusterTemplate)
                .thenReturn(clusterTemplate2)
                .thenReturn(clusterTemplateAws)
                .thenReturn(clusterTemplateAwsRanger)
                .thenReturn(clusterTemplateAzure);
        when(blueprintEntities.getDefaults()).thenReturn(Map.of("7.2.6", "7.2.6 - Data Engineering: Apache Spark, Apache Hive, Apache Oozie=cdp-data-engineering"));

        underTest.setClusterTemplates(Collections.emptyList());
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        assertThat(actual.size(), is(5));
        assertThat(actual.get("cluster-template"), is(notNullValue()));
        assertThat(actual.get("cluster-template2"), is(notNullValue()));
        assertThat(actual.get("cluster-template-aws"), is(notNullValue()));
        assertThat(actual.get("cluster-template-aws-ranger"), is(notNullValue()));
        assertThat(actual.get("cluster-template-azure"), is(notNullValue()));
    }

    @Test
    public void testLoadClusterTemplatesFromFileWhenResourceDirNotExists() {
        underTest.setClusterTemplates(Collections.emptyList());
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/notexists");
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        assertThat(actual.size(), is(0));
    }

    @Test
    public void testLoadClusterTemplatesFromFileWhenResourceDirEmpty() {
        underTest.setClusterTemplates(Collections.emptyList());
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/empty");
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        assertThat(actual.size(), is(0));
    }
}
