package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class OozieRoleConfigProviderTest {

    private final OozieRoleConfigProvider underTest = new OozieRoleConfigProvider();

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> oozieServer = roleConfigs.get("oozie-OOZIE_SERVER-BASE");

        assertEquals(5, oozieServer.size());
        assertEquals("oozie_database_host", oozieServer.get(0).getName());
        assertEquals("testhost", oozieServer.get(0).getValue());

        assertEquals("oozie_database_name", oozieServer.get(1).getName());
        assertEquals("ooziedb", oozieServer.get(1).getValue());

        assertEquals("oozie_database_type", oozieServer.get(2).getName());
        assertEquals("postgresql", oozieServer.get(2).getValue());

        assertEquals("oozie_database_user", oozieServer.get(3).getName());
        assertEquals("testuser", oozieServer.get(3).getValue());

        assertEquals("oozie_database_password", oozieServer.get(4).getName());
        assertEquals("testpassword", oozieServer.get(4).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setType(DatabaseType.OOZIE.toString());
        rdsConfig.setConnectionPassword("testpassword");
        rdsConfig.setConnectionUserName("testuser");
        rdsConfig.setConnectionURL("jdbc:postgresql://testhost:5432/ooziedb");

        return TemplatePreparationObject.Builder.builder()
                .withHostgroupViews(Set.of(master, worker)).withRdsConfigs(Set.of(rdsConfig)).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}