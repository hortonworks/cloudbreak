package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class DependentRolesHealthCheckServiceTest {

    private static final String UNDEFINED_DEPENDENCY = "UNDEFINED_DEPENDENCY";

    @InjectMocks
    private DependentRolesHealthCheckService dependentRolesHealthCheckService;

    private InstanceMetadataView generateInstanceMetadata(String hostgroup, InstanceStatus instanceStatus) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(hostgroup);
        instanceMetaData.setInstanceGroup(instanceGroup);
        instanceMetaData.setInstanceStatus(instanceStatus);
        return instanceMetaData;
    }

    @Test
    public void testgetUnhealthyDependentHostGroupsMasterUnhealthy() {
        CmTemplateProcessor processor = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        StackDto stackdto = mock(StackDto.class);
        List<String> expected = new ArrayList<>();
        expected.add("master");

        Set<String> dependentComponent = new HashSet<String>();
        dependentComponent.add("RESOURCEMANAGER");

        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        instanceMetadataViews.add(generateInstanceMetadata("master", InstanceStatus.SERVICES_UNHEALTHY));
        instanceMetadataViews.add(generateInstanceMetadata("worker", InstanceStatus.SERVICES_HEALTHY));
        instanceMetadataViews.add(generateInstanceMetadata("compute", InstanceStatus.SERVICES_UNHEALTHY));
        when(stackdto.getAllAvailableInstances()).thenReturn(instanceMetadataViews);
        List<String> actual = dependentRolesHealthCheckService.getUnhealthyDependentHostGroups(stackdto, processor, dependentComponent);
        assertEquals(actual, expected);
    }

    @Test
    public void testgetUnhealthyDependentHostGroupsMasterhealthy() {
        CmTemplateProcessor processor = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        StackDto stackdto = mock(StackDto.class);
        List<String> expected = new ArrayList<>();

        Set<String> dependentComponent = new HashSet<String>();
        dependentComponent.add("RESOURCEMANAGER");

        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>();
        instanceMetadataViews.add(generateInstanceMetadata("master", InstanceStatus.SERVICES_HEALTHY));
        instanceMetadataViews.add(generateInstanceMetadata("worker", InstanceStatus.SERVICES_HEALTHY));
        instanceMetadataViews.add(generateInstanceMetadata("compute", InstanceStatus.SERVICES_UNHEALTHY));
        when(stackdto.getAllAvailableInstances()).thenReturn(instanceMetadataViews);
        List<String> actual = dependentRolesHealthCheckService.getUnhealthyDependentHostGroups(stackdto, processor, dependentComponent);
        assertEquals(actual, expected);
    }

    @Test
    public void testgetDependentComponentsWithDependencyDefined() {
        CmTemplateProcessor processor = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        Set<String> expected = new HashSet<>();
        expected.add("RESOURCEMANAGER");
        Set<String> actual = dependentRolesHealthCheckService.getDependentComponentsForHostGroup(processor, "compute");
        assertEquals(actual, expected);
    }

    @Test
    public void testgetDependentComponentsWithDependencyUnDefined() {
        CmTemplateProcessor processor = new CmTemplateProcessor(getBlueprintText("input/de-ha.bp"));
        Set<String> expected = new HashSet<>();
        expected.add(UNDEFINED_DEPENDENCY);
        Set<String> actual = dependentRolesHealthCheckService.getDependentComponentsForHostGroup(processor, "master");
        assertEquals(actual, expected);
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

}