package com.sequenceiq.cloudbreak.cmtemplate.configproviders.profilermanager;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ProfilerMetricsRoleConfigProviderTest {
    private ProfilerMetricsRoleConfigProvider underTest;

    @Before
    public void setUp() {
        underTest = new ProfilerMetricsRoleConfigProvider();
    }

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        TemplatePreparationObject preparationObject = getTemplatePreparationObject();
        String inputJson = getBlueprintText("input/clouderamanager-db-config.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>>
                roleConfigs =
                underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);
        List<ApiClusterTemplateConfig>
                profilerMetrics =
                roleConfigs.get("profiler_manager-PROFILER_METRICS_AGENT-BASE");

        assertEquals(5, profilerMetrics.size());
        assertEquals("profiler_metrics_database_host", profilerMetrics.get(0).getName());
        assertEquals("10.1.1.1", profilerMetrics.get(0).getValue());

        assertEquals("profiler_metrics_database_name", profilerMetrics.get(1).getName());
        assertEquals("profiler_metric", profilerMetrics.get(1).getValue());

        assertEquals("profiler_metrics_database_type", profilerMetrics.get(2).getName());
        assertEquals("POSTGRES", profilerMetrics.get(2).getValue());

        assertEquals("profiler_metrics_database_user", profilerMetrics.get(3).getName());
        assertEquals("heyitsme", profilerMetrics.get(3).getValue());

        assertEquals("profiler_metrics_database_password", profilerMetrics.get(4).getName());
        assertEquals("iamsoosecure", profilerMetrics.get(4).getValue());
    }

    private TemplatePreparationObject getTemplatePreparationObject() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);

        return Builder.builder().withHostgroupViews(Set.of(master, worker))
                .withRdsConfigs(Set.of(rdsConfig(DatabaseType.PROFILER_METRIC))).build();
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}