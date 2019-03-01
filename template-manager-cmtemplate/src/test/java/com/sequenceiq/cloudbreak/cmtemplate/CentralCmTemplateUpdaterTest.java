package com.sequenceiq.cloudbreak.cmtemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class CentralCmTemplateUpdaterTest {

    private static final String FQDN = "fqdn";

    @InjectMocks
    private CentralCmTemplateUpdater generator = new CentralCmTemplateUpdater();

    @Spy
    private TemplateProcessor templateProcessor;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Spy
    private CmTemplateComponentConfigProcessor cmTemplateComponentConfigProcessor;

    @Mock
    private TemplatePreparationObject templatePreparationObject;

    @Mock
    private ClusterDefinitionView clusterDefinitionView;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    @Mock
    private RDSConfig rdsConfig;

    @Before
    public void setUp() throws IOException {
        List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviders = List.of(new HiveMetastoreConfigProvider());
        when(cmTemplateProcessorFactory.get(anyString())).thenAnswer(i -> new CmTemplateProcessor(i.getArgument(0)));
        when(templatePreparationObject.getClusterDefinitionView()).thenReturn(clusterDefinitionView);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(templatePreparationObject.getRdsConfigs()).thenReturn(getRdsConfigs());
        when(generalClusterConfigs.getClusterName()).thenReturn("testcluster");
        when(cmTemplateComponentConfigProcessor.getCmTemplateComponentConfigProviderList()).thenReturn(cmTemplateComponentConfigProviders);
    }

    @Test
    public void getCmTemplate() {
        when(clusterDefinitionView.getClusterDefinitionText()).thenReturn(getClusterDefinitionText("input/clouderamanager.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings());
        Assert.assertEquals(new CmTemplateProcessor(getClusterDefinitionText("output/clouderamanager.bp")).getTemplate(), generated);
    }

    @Test
    public void getCmTemplateNoMetastore() throws IOException {
        when(clusterDefinitionView.getClusterDefinitionText()).thenReturn(getClusterDefinitionText("input/clouderamanager-nometastore.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings());
        Assert.assertEquals(new CmTemplateProcessor(getClusterDefinitionText("output/clouderamanager-nometastore.bp")).getTemplate(), generated);
    }

    @Test
    public void getCmTemplateNoMetastoreWithTemplateParams() throws IOException {
        when(clusterDefinitionView.getClusterDefinitionText()).thenReturn(getClusterDefinitionText("input/clouderamanager-fixparam.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings());
        Assert.assertEquals(new CmTemplateProcessor(getClusterDefinitionText("output/clouderamanager-fixparam.bp")).getTemplate(), generated);
    }

    @Test
    public void getCmTemplateWithoutHosts() {
        when(clusterDefinitionView.getClusterDefinitionText()).thenReturn(getClusterDefinitionText("input/clouderamanager-without-hosts.bp"));
        String generated = generator.getClusterDefinitionText(templatePreparationObject);
        Assert.assertEquals(new CmTemplateProcessor(getClusterDefinitionText("output/clouderamanager-without-hosts.bp")).getTemplate(),
                new CmTemplateProcessor(generated).getTemplate());
    }

    private String getClusterDefinitionText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private Map<String, List<Map<String, String>>> getHostgroupMappings() {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        List<Map<String, String>> hosts12 = new ArrayList<>();
        hosts12.add(Map.of(FQDN, "host1"));
        hosts12.add(Map.of(FQDN, "host2"));
        result.put("master", hosts12);
        List<Map<String, String>> hosts34 = new ArrayList<>();
        hosts34.add(Map.of(FQDN, "host3"));
        hosts34.add(Map.of(FQDN, "host4"));
        result.put("worker", hosts34);
        return result;
    }

    private Set<RDSConfig> getRdsConfigs() {
        RDSConfig config = new RDSConfig();
        config.setConnectionURL("jdbc:postgresql://cluster.test.com:5432/hive");
        config.setDatabaseEngine(DatabaseVendor.POSTGRES);
        config.setType(DatabaseType.HIVE.name());
        config.setConnectionUserName("user");
        config.setConnectionPassword("password");
        return Set.of(config);
    }
}