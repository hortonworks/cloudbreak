package com.sequenceiq.cloudbreak.cmtemplate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class RangerConfigProviderTest {

    private CmTemplateProcessor underTest;

    @Test
    public void testRangerServiceDbConfigs() {
        underTest = new CmTemplateProcessor(getBlueprintText("input/clouderamanager-db-config.bp"));
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();
        configs.add(new ApiClusterTemplateConfig().name("ranger_database_host").variable("ranger-ranger_database_host"));
        configs.add(new ApiClusterTemplateConfig().name("ranger_database_name").variable("ranger-ranger_database_name"));
        configs.add(new ApiClusterTemplateConfig().name("ranger_database_type").variable("ranger-ranger_database_type"));
        configs.add(new ApiClusterTemplateConfig().name("ranger_database_user").variable("ranger-ranger_database_user"));
        configs.add(new ApiClusterTemplateConfig().name("ranger_database_user_password").variable("ranger-ranger_database_user_password"));

        underTest.addServiceConfigs("RANGER", List.of("RANGER_ADMIN"), configs);

        ApiClusterTemplateService service = underTest.getTemplate().getServices().stream().filter(srv -> "RANGER".equals(srv.getServiceType())).findAny().get();
        List<ApiClusterTemplateConfig> serviceConfigs = service.getServiceConfigs();
        assertEquals(5, serviceConfigs.size());
        assertEquals("ranger_database_host", serviceConfigs.get(0).getName());
        assertEquals("ranger-ranger_database_host", serviceConfigs.get(0).getVariable());

        assertEquals("ranger_database_name", serviceConfigs.get(1).getName());
        assertEquals("ranger-ranger_database_name", serviceConfigs.get(1).getVariable());

        assertEquals("ranger_database_type", serviceConfigs.get(2).getName());
        assertEquals("ranger-ranger_database_type", serviceConfigs.get(2).getVariable());

        assertEquals("ranger_database_user", serviceConfigs.get(3).getName());
        assertEquals("ranger-ranger_database_user", serviceConfigs.get(3).getVariable());

        assertEquals("ranger_database_user_password", serviceConfigs.get(4).getName());
        assertEquals("ranger-ranger_database_user_password", serviceConfigs.get(4).getVariable());
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}