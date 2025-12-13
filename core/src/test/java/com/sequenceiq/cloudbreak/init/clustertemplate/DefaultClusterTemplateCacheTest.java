package com.sequenceiq.cloudbreak.init.clustertemplate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.DefaultClusterTemplateV4RequestToClusterTemplateConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;

@ExtendWith(MockitoExtension.class)
class DefaultClusterTemplateCacheTest {

    private String defaultTemplateDir = "defaults/clustertemplates";

    @InjectMocks
    private DefaultClusterTemplateCache underTest;

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private CommonGovService commonGovService;

    @Mock
    private DefaultClusterTemplateV4RequestToClusterTemplateConverter defaultClusterTemplateV4RequestToClusterTemplateConverter;

    @BeforeEach
    void setUp() throws Exception {
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/");
    }

    @Test
    void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesProvided() {
        ClusterTemplate clusterTemplate = new ClusterTemplate();
        clusterTemplate.setName("template");
        when(defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(any())).thenReturn(clusterTemplate);

        underTest.setClusterTemplates(Collections.singletonList("default-template.json"));
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(1));
    }

    @Test
    void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesProvidedButDirNotExists() {
        underTest.setClusterTemplates(Collections.singletonList("default-template"));
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/notexists");
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(0));
    }

    @Test
    void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesProvidedButFileNotExists() {
        underTest.setClusterTemplates(Collections.singletonList("default-template-not-exists"));
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(0));
    }

    @Test
    void testLoadClusterTemplatesFromFileWhenClusterTemplateNamesNotProvided() {
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

        underTest.setClusterTemplates(Collections.emptyList());
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(7));
        MatcherAssert.assertThat(actual.get("cluster-template"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template2"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws-ranger"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-azure"), is(notNullValue()));
    }

    @Test
    void testAwsGovWhenWeAreRunningLocally() {
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
        ClusterTemplate clusterTemplateAwsGov = new ClusterTemplate();
        clusterTemplateAwsGov.setName("cluster-template-aws-gov");
        ClusterTemplate clusterTemplateAwsGovRanger = new ClusterTemplate();
        clusterTemplateAwsGovRanger.setName("cluster-template-aws-gov-ranger");

        when(preferencesService.enabledGovPlatforms())
                .thenReturn(Set.of(CloudPlatform.AWS.name()));
        when(preferencesService.enabledPlatforms())
                .thenReturn(Set.of(CloudPlatform.AWS.name()));

        when(defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(any(DefaultClusterTemplateV4Request.class)))
                .thenReturn(clusterTemplate)
                .thenReturn(clusterTemplate2)
                .thenReturn(clusterTemplateAws)
                .thenReturn(clusterTemplateAwsRanger)
                .thenReturn(clusterTemplateAzure)
                .thenReturn(clusterTemplateAwsGov)
                .thenReturn(clusterTemplateAwsGovRanger);

        underTest.setClusterTemplates(Collections.emptyList());
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(5));
        MatcherAssert.assertThat(actual.get("cluster-template"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template2"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws-ranger"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-azure"), is(notNullValue()));
    }

    @Test
    void testAwsGovWhenWeAreRunningGovCloud() {
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
        ClusterTemplate clusterTemplateAwsGov = new ClusterTemplate();
        clusterTemplateAwsGov.setName("cluster-template-aws-gov");
        ClusterTemplate clusterTemplateAwsGovRanger = new ClusterTemplate();
        clusterTemplateAwsGovRanger.setName("cluster-template-aws-gov-ranger");

        when(preferencesService.enabledGovPlatforms())
                .thenReturn(Set.of(CloudPlatform.AWS.name()));
        when(preferencesService.enabledPlatforms())
                .thenReturn(Set.of());

        when(defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(any(DefaultClusterTemplateV4Request.class)))
                .thenReturn(clusterTemplateAwsGov)
                .thenReturn(clusterTemplateAwsGovRanger);

        when(commonGovService.govCloudDeployment(any(), any())).thenReturn(true);

        underTest.setClusterTemplates(Collections.emptyList());
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(2));
        MatcherAssert.assertThat(actual.get("cluster-template-aws-gov"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws-gov-ranger"), is(notNullValue()));
    }

    @Test
    void testAwsWhenWeAreRunningPublicCloud() {
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
        ClusterTemplate clusterTemplateAwsGov = new ClusterTemplate();
        clusterTemplateAwsGov.setName("cluster-template-aws-gov");
        ClusterTemplate clusterTemplateAwsGovRanger = new ClusterTemplate();
        clusterTemplateAwsGovRanger.setName("cluster-template-aws-gov-ranger");

        when(preferencesService.enabledPlatforms())
                .thenReturn(Set.of(CloudPlatform.AWS.name()));
        when(preferencesService.enabledGovPlatforms())
                .thenReturn(Set.of());

        when(defaultClusterTemplateV4RequestToClusterTemplateConverter.convert(any(DefaultClusterTemplateV4Request.class)))
                .thenReturn(clusterTemplate)
                .thenReturn(clusterTemplate2)
                .thenReturn(clusterTemplateAws)
                .thenReturn(clusterTemplateAwsRanger)
                .thenReturn(clusterTemplateAzure)
                .thenReturn(clusterTemplateAwsGov)
                .thenReturn(clusterTemplateAwsGovRanger);

        underTest.setClusterTemplates(Collections.emptyList());
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(5));
        MatcherAssert.assertThat(actual.get("cluster-template"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template2"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-aws-ranger"), is(notNullValue()));
        MatcherAssert.assertThat(actual.get("cluster-template-azure"), is(notNullValue()));
    }

    @Test
    void testLoadClusterTemplatesFromFileWhenResourceDirNotExists() {
        underTest.setClusterTemplates(Collections.emptyList());
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/notexists");
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(0));
    }

    @Test
    void testClusterTemplatesForGovOnlyPresentedFor7218AndNothingElse() throws IOException {
        Set<String> actual = getFiles().stream()
                .filter(e -> e.contains("aws_gov"))
                .filter(e -> !e.contains("7.2.18"))
                .collect(Collectors.toSet());

        MatcherAssert.assertThat(actual.size(), is(0));
    }

    @Test
    void testLoadClusterTemplatesFromFileWhenResourceDirEmpty() {
        underTest.setClusterTemplates(Collections.emptyList());
        underTest.setDefaultTemplateDir("test/defaults/clustertemplates/empty");
        underTest.loadClusterTemplatesFromFile();

        Map<String, ClusterTemplate> actual = underTest.defaultClusterTemplates();
        MatcherAssert.assertThat(actual.size(), is(0));
    }

    private List<String> getFiles() throws IOException {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        return Arrays.stream(patternResolver.getResources("classpath:" + defaultTemplateDir + "/**/*.json"))
                .map(resource -> {
                    try {
                        String[] path = resource.getURL().getPath().split(defaultTemplateDir);
                        return String.format("%s%s", defaultTemplateDir, path[1]);
                    } catch (IOException e) {
                        // wrap to runtime exception because of lambda and log the error in the caller method.
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }
}
