package com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject.Builder;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class YarnVolumeConfigProviderTest {

    private final YarnRoleConfigConfigProvider underTest = new YarnRoleConfigConfigProvider();

    @Test
    public void testGetRoleConfigsWithSingleRolesPerHostGroup() {
        HostgroupView master = new HostgroupView("master", 1, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 2, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> workerNM = roleConfigs.get("yarn-NODEMANAGER-BASE");

        assertEquals(2, workerNM.size());
        assertEquals("yarn_nodemanager_local_dirs", workerNM.get(0).getName());
        assertEquals("/hadoopfs/fs1/nodemanager,/hadoopfs/fs2/nodemanager", workerNM.get(0).getValue());
        assertEquals("yarn_nodemanager_log_dirs", workerNM.get(1).getName());
        assertEquals("/hadoopfs/fs1/nodemanager/log,/hadoopfs/fs2/nodemanager/log", workerNM.get(1).getValue());
    }

    @Test
    public void testGetRoleConfigsWithZeroDisks() {
        HostgroupView master = new HostgroupView("master", 0, InstanceGroupType.GATEWAY, 1);
        HostgroupView worker = new HostgroupView("worker", 0, InstanceGroupType.CORE, 2);
        TemplatePreparationObject preparationObject = Builder.builder().withHostgroupViews(Set.of(master, worker)).build();
        String inputJson = getBlueprintText("input/clouderamanager.bp");
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(inputJson);

        Map<String, List<ApiClusterTemplateConfig>> roleConfigs = underTest.getRoleConfigs(cmTemplateProcessor, preparationObject);

        List<ApiClusterTemplateConfig> workerNM = roleConfigs.get("yarn-NODEMANAGER-BASE");

        assertEquals(2, workerNM.size());
        assertEquals("yarn_nodemanager_local_dirs", workerNM.get(0).getName());
        assertEquals("/hadoopfs/root1/nodemanager", workerNM.get(0).getValue());
        assertEquals("yarn_nodemanager_log_dirs", workerNM.get(1).getName());
        assertEquals("/hadoopfs/root1/nodemanager/log", workerNM.get(1).getValue());
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }
}
