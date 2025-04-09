package com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.view.RdsConfigWithoutCluster;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.TemplateCoreTestUtil;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class OozieRoleConfigProviderTest {

    private final OozieRoleConfigProvider underTest = new OozieRoleConfigProvider();

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor,
                1, false, "7.2.2");

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

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroupWhenSSLIsTrue() {
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor,
                1, true, "7.2.2");

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> oozieServer = roleConfigs.get("oozie-OOZIE_SERVER-BASE");

        assertEquals(4, oozieServer.size());

        assertEquals("oozie_database_type", oozieServer.get(0).getName());
        assertEquals("postgresql", oozieServer.get(0).getValue());

        assertEquals("oozie_database_user", oozieServer.get(1).getName());
        assertEquals("testuser", oozieServer.get(1).getValue());

        assertEquals("oozie_database_password", oozieServer.get(2).getName());
        assertEquals("testpassword", oozieServer.get(2).getValue());

        assertEquals("oozie_config_safety_valve", oozieServer.get(3).getName());
        assertEquals("<property><name>oozie.service.JPAService.jdbc.url</name><value>" +
                        "jdbc:postgresql://testhost:5432/ooziedb?sslmode=verify-full&amp;sslrootcert=</value></property>",
                oozieServer.get(3).getValue());
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroupWhenSSLIsTrueAndVersionHigherThan7112() {
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor,
                1, true, "7.11.2");

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> oozieServer = roleConfigs.get("oozie-OOZIE_SERVER-BASE");

        assertEquals(6, oozieServer.size());

        assertEquals("oozie_database_host", oozieServer.get(0).getName());
        assertEquals("testhost", oozieServer.get(0).getValue());

        assertEquals("oozie_database_is_secure", oozieServer.get(1).getName());
        assertEquals("true", oozieServer.get(1).getValue());

        assertEquals("oozie_database_connection_properties", oozieServer.get(2).getName());
        assertEquals("<property><name>sslmode</name><value>verify-full</value></property>" +
                        "<property><name>sslrootcert</name><value>/hadoopfs/fs/cert</value></property>",
                oozieServer.get(2).getValue());

        assertEquals("oozie_database_type", oozieServer.get(3).getName());
        assertEquals("postgresql", oozieServer.get(3).getValue());

        assertEquals("oozie_database_user", oozieServer.get(4).getName());
        assertEquals("testuser", oozieServer.get(4).getValue());

        assertEquals("oozie_database_password", oozieServer.get(5).getName());
        assertEquals("testpassword", oozieServer.get(5).getValue());
    }

    @Test
    public void testGetRoleConfigsWithOozieHA() {
        String inputJson = getBlueprintText("input/de-ha.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor,
                2, false, "7.2.2");

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

    @Test
    public void testGetRoleConfigsWithNoOozie() {
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(inputJson, cmTemplateProcessor, 1, false, "7.2.2");

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig> oozieServer = roleConfigs.get("oozie-OOZIE_SERVER-BASE");

        assertNull(oozieServer);
    }

    static TemplatePreparationObject getTemplatePreparationObject(String inputJson,
            CmTemplateProcessor cmTemplateProcessor, int numMasters, boolean ssl, String version) {
        List<String> hosts = new ArrayList<>();
        for (int i = 0; i < numMasters; i++) {
            hosts.add("master" + i + ".blah.timbuk2.dev.cldr.");
        }
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, hosts);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        RdsConfigWithoutCluster rdsConfig = mock(RdsConfigWithoutCluster.class);
        lenient().when(rdsConfig.getType()).thenReturn(DatabaseType.OOZIE.toString());
        lenient().when(rdsConfig.getConnectionPassword()).thenReturn("testpassword");
        lenient().when(rdsConfig.getConnectionUserName()).thenReturn("testuser");
        lenient().when(rdsConfig.getConnectionURL()).thenReturn("jdbc:postgresql://testhost:5432/ooziedb");
        if (ssl) {
            lenient().when(rdsConfig.getSslMode()).thenReturn(RdsSslMode.ENABLED);
        }
        return Builder.builder()
                .withRdsSslCertificateFilePath("/hadoopfs/fs/cert")
                .withHostgroupViews(Set.of(master, worker))
                .withRdsViews(Set.of(rdsConfig)
                        .stream()
                        .map(e -> TemplateCoreTestUtil.rdsViewProvider().getRdsView(e, "AWS", true))
                        .collect(Collectors.toSet()))
                .withBlueprintView(new BlueprintView(inputJson, "CDP", "1.0", null, cmTemplateProcessor))
                .withProductDetails(new ClouderaManagerRepo()
                        .withVersion(version)
                        .withBaseUrl("url"), new ArrayList<>())
                .withCloudPlatform(CloudPlatform.GCP)
                .build();
    }

    static String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}