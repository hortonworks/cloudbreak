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

class OpenSearchRoleConfigProviderTest {

    private OpenSearchRoleConfigProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new OpenSearchRoleConfigProvider();
    }

    @Test
    void testGetRoleConfigsProducesConfigForAllRoleTypes() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        assertThat(roleConfigs).containsKey("opensearch-OPENSEARCH_MASTER-BASE");
        assertThat(roleConfigs).containsKey("opensearch-OPENSEARCH_DATA-BASE");
        assertThat(roleConfigs).containsKey("opensearch-OPENSEARCH_COORDINATOR-BASE");
        assertThat(roleConfigs).containsKey("opensearch-OPENSEARCH_INGEST-BASE");
        assertThat(roleConfigs).containsKey("opensearch-OPENSEARCH_ML-BASE");
    }

    @Test
    void testGetRoleConfigsLogDirUsesLastPartOfRoleType() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> masterConfigs = roleConfigs.get("opensearch-OPENSEARCH_MASTER-BASE");
        assertEquals(1, masterConfigs.size());
        assertEquals("semanticsearch_local_log_dir", masterConfigs.get(0).getName());
        // master hostgroup has volumeCount=2
        assertEquals("/hadoopfs/fs1/opensearch/logs/master,/hadoopfs/fs2/opensearch/logs/master", masterConfigs.get(0).getValue());

        List<ApiClusterTemplateConfig> dataConfigs = roleConfigs.get("opensearch-OPENSEARCH_DATA-BASE");
        assertEquals("semanticsearch_local_log_dir", dataConfigs.get(0).getName());
        // worker hostgroup has volumeCount=3
        assertEquals("/hadoopfs/fs1/opensearch/logs/data,/hadoopfs/fs2/opensearch/logs/data,/hadoopfs/fs3/opensearch/logs/data",
                dataConfigs.get(0).getValue());

        List<ApiClusterTemplateConfig> coordinatorConfigs = roleConfigs.get("opensearch-OPENSEARCH_COORDINATOR-BASE");
        assertEquals("semanticsearch_local_log_dir", coordinatorConfigs.get(0).getName());
        assertEquals("/hadoopfs/fs1/opensearch/logs/coordinator,/hadoopfs/fs2/opensearch/logs/coordinator,/hadoopfs/fs3/opensearch/logs/coordinator",
                coordinatorConfigs.get(0).getValue());

        List<ApiClusterTemplateConfig> ingestConfigs = roleConfigs.get("opensearch-OPENSEARCH_INGEST-BASE");
        assertEquals("semanticsearch_local_log_dir", ingestConfigs.get(0).getName());
        assertEquals("/hadoopfs/fs1/opensearch/logs/ingest,/hadoopfs/fs2/opensearch/logs/ingest,/hadoopfs/fs3/opensearch/logs/ingest",
                ingestConfigs.get(0).getValue());

        List<ApiClusterTemplateConfig> mlConfigs = roleConfigs.get("opensearch-OPENSEARCH_ML-BASE");
        assertEquals("semanticsearch_local_log_dir", mlConfigs.get(0).getName());
        assertEquals("/hadoopfs/fs1/opensearch/logs/ml,/hadoopfs/fs2/opensearch/logs/ml,/hadoopfs/fs3/opensearch/logs/ml",
                mlConfigs.get(0).getValue());
    }

    @Test
    void testIsConfigurationNeededWhenOpenSearchPresent() {
        String inputJson = FileReaderUtils.readFileFromClasspathQuietly("input/opensearch.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);
        TemplatePreparationObject preparationObject = getTemplatePreparationObject(cmTemplateProcessor);

        assertTrue(underTest.isConfigurationNeeded(cmTemplateProcessor, preparationObject));
    }

    @Test
    void testIsConfigurationNeededWhenOpenSearchNotPresent() {
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
        assertThat(roleTypes).containsExactly(
                "OPENSEARCH_MASTER",
                "OPENSEARCH_DATA",
                "OPENSEARCH_ML",
                "OPENSEARCH_COORDINATOR",
                "OPENSEARCH_INGEST"
        );
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
