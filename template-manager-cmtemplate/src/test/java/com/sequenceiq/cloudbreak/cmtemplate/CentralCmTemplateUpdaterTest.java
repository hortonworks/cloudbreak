package com.sequenceiq.cloudbreak.cmtemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.HiveMetastoreConfigProvider;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplateProcessor;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
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
    private BlueprintView blueprintView;

    @Mock
    private GeneralClusterConfigs generalClusterConfigs;

    @Mock
    private RDSConfig rdsConfig;

    private ClouderaManagerRepo clouderaManagerRepo;

    @Before
    public void setUp() {
        List<CmTemplateComponentConfigProvider> cmTemplateComponentConfigProviders = List.of(new HiveMetastoreConfigProvider());
        when(cmTemplateProcessorFactory.get(anyString())).thenAnswer(i -> new CmTemplateProcessor(i.getArgument(0)));
        when(templatePreparationObject.getBlueprintView()).thenReturn(blueprintView);
        when(templatePreparationObject.getGeneralClusterConfigs()).thenReturn(generalClusterConfigs);
        when(templatePreparationObject.getRdsConfigs()).thenReturn(getRdsConfigs());
        when(generalClusterConfigs.getClusterName()).thenReturn("testcluster");
        clouderaManagerRepo = new ClouderaManagerRepo();
        clouderaManagerRepo.setVersion("6.1.0");
        ReflectionTestUtils.setField(cmTemplateComponentConfigProcessor, "cmTemplateComponentConfigProviderList", cmTemplateComponentConfigProviders);
    }

    @Test
    public void getCmTemplate() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null);
        Assert.assertEquals(new CmTemplateProcessor(getBlueprintText("output/clouderamanager.bp")).getTemplate(), generated);
    }

    @Test
    public void getCmTemplateNoMetastore() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-nometastore.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null);
        Assert.assertEquals(new CmTemplateProcessor(getBlueprintText("output/clouderamanager-nometastore.bp")).getTemplate(), generated);
    }

    @Test
    public void getCmTemplateNoMetastoreWithTemplateParams() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-fixparam.bp"));
        ApiClusterTemplate generated = generator.getCmTemplate(templatePreparationObject, getHostgroupMappings(), clouderaManagerRepo, null);
        Assert.assertEquals(new CmTemplateProcessor(getBlueprintText("output/clouderamanager-fixparam.bp")).getTemplate(), generated);
    }

    @Test
    public void getCmTemplateWithoutHosts() {
        when(blueprintView.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-without-hosts.bp"));
        String generated = generator.getBlueprintText(templatePreparationObject);
        Assert.assertEquals(new CmTemplateProcessor(getBlueprintText("output/clouderamanager-without-hosts.bp")).getTemplate(),
                new CmTemplateProcessor(generated).getTemplate());
    }

    private String getBlueprintText(String path) {
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