package com.sequenceiq.cloudbreak.cmtemplate.configproviders.opensearch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

class OpenSearchDashboardRoleConfigProviderTest {

    private OpenSearchDashboardRoleConfigProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new OpenSearchDashboardRoleConfigProvider();
    }

    @Test
    void testGetRoleConfigsWithVolumes() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 2, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 3, InstanceGroupType.CORE, 1);
        HostgroupView worker = new HostgroupView("worker", 3, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withBlueprintView(blueprintView)
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        assertThat(roleConfigs).containsKey("opensearch-OPENSEARCH_DASHBOARD-BASE");
        List<ApiClusterTemplateConfig> dashboardConfigs = roleConfigs.get("opensearch-OPENSEARCH_DASHBOARD-BASE");
        assertEquals(1, dashboardConfigs.size());
        assertEquals("dashboard_local_log_dir", dashboardConfigs.get(0).getName());
        assertEquals("/hadoopfs/fs1/opensearch/logs/dashboard,/hadoopfs/fs2/opensearch/logs/dashboard", dashboardConfigs.get(0).getValue());
    }

    @Test
    void testGetRoleConfigsWithZeroVolumes() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 2, InstanceGroupType.CORE, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withBlueprintView(blueprintView)
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> dashboardConfigs = roleConfigs.get("opensearch-OPENSEARCH_DASHBOARD-BASE");
        assertEquals("/hadoopfs/root1/opensearch/logs/dashboard", dashboardConfigs.get(0).getValue());
    }

    @Test
    void testGetRoleConfigsWithSingleVolume() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 3, InstanceGroupType.CORE, 1);
        HostgroupView worker = new HostgroupView("worker", 3, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        TemplatePreparationObject preparationObject = Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withBlueprintView(blueprintView)
                .build();

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> dashboardConfigs = roleConfigs.get("opensearch-OPENSEARCH_DASHBOARD-BASE");
        assertEquals("/hadoopfs/fs1/opensearch/logs/dashboard", dashboardConfigs.get(0).getValue());
    }

    @Test
    void testIsConfigurationNeededWhenOpenSearchDashboardPresent() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    void testIsConfigurationNeededWhenOpenSearchDashboardNotPresent() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/clouderamanager-ds.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        assertFalse(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    void testGetServiceType() {
        assertEquals("OPENSEARCH", underTest.getServiceType());
    }

    @Test
    void testGetRoleTypes() {
        List<String> roleTypes = underTest.getRoleTypes();
        assertThat(roleTypes).containsExactly("OPENSEARCH_DASHBOARD");
    }

    private TemplatePreparationObject getTemplatePreparationObject(CmTemplateProcessor cmTemplateProcessor) {
        HostgroupView gateway = new HostgroupView("gateway", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView master = new HostgroupView("master", 2, InstanceGroupType.CORE, 1);
        HostgroupView worker = new HostgroupView("worker", 3, InstanceGroupType.CORE, 3);
        BlueprintView blueprintView = new BlueprintView(null, null, null, null, cmTemplateProcessor);

        return Builder.builder()
                .withHostgroupViews(Set.of(gateway, master, worker))
                .withBlueprintView(blueprintView)
                .build();
    }
}
