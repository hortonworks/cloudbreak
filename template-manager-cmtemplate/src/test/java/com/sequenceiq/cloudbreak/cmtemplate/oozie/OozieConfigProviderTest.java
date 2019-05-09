package com.sequenceiq.cloudbreak.cmtemplate.oozie;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class OozieConfigProviderTest {

    private CmTemplateProcessor underTest;

    @Test
    public void testOozieServiceDbConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-db-config.bp"));
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig().name("oozie_database_host").variable("oozie-oozie_database_host"));
        configs.add(new ApiClusterTemplateConfig().name("oozie_database_name").variable("oozie-oozie_database_name"));
        configs.add(new ApiClusterTemplateConfig().name("oozie_database_type").variable("oozie-oozie_database_type"));
        configs.add(new ApiClusterTemplateConfig().name("oozie_database_user").variable("oozie-oozie_database_user"));
        configs.add(new ApiClusterTemplateConfig().name("oozie_database_password").variable("oozie-oozie_database_password"));

        underTest.addServiceConfigs("OOZIE", List.of("OOZIE_SERVER"), configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "OOZIE".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();
        assertEquals(5, serviceConfigs.size());
        assertEquals("oozie_database_host", serviceConfigs.get(0).getName());
        assertEquals("oozie-oozie_database_host", serviceConfigs.get(0).getVariable());

        assertEquals("oozie_database_name", serviceConfigs.get(1).getName());
        assertEquals("oozie-oozie_database_name", serviceConfigs.get(1).getVariable());

        assertEquals("oozie_database_type", serviceConfigs.get(2).getName());
        assertEquals("oozie-oozie_database_type", serviceConfigs.get(2).getVariable());

        assertEquals("oozie_database_user", serviceConfigs.get(3).getName());
        assertEquals("oozie-oozie_database_user", serviceConfigs.get(3).getVariable());

        assertEquals("oozie_database_password", serviceConfigs.get(4).getName());
        assertEquals("oozie-oozie_database_password", serviceConfigs.get(4).getVariable());
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}