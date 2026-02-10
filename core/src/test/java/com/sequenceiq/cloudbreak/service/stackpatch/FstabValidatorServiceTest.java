package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class FstabValidatorServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID = "instance-id";

    private static final String FQDN = "fqdn";

    private static final String FSTAB_FROM_DB = "/dev/xvdb /hadoopfs/fs1 auto defaults,noatime 0 0";

    private static final String FSTAB_FROM_DB_2 = "/dev/xvdb /hadoopfs/fs1 auto defaults,noatime 0 0 \n" +
        "/dev/xvdc /hadoopfs/fs2 auto defaults,noatime 0 0";

    private static final String FSTAB_FROM_HOST = "/dev/xvdb /hadoopfs/fs1 auto defaults,noatime 0 2";

    @Mock
    private ResourceService resourceService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private SaltOrchestrator saltOrchestrator;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private SaltService saltService;

    @InjectMocks
    private FstabValidatorService underTest;

    private Stack stack;

    private StackDto stackDto;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("test-stack");
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, null, null));

        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(INSTANCE_ID);
        instanceMetaData.setDiscoveryFQDN(FQDN);
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));

        InstanceGroupDto instanceGroupDto = new InstanceGroupDto(instanceGroup, List.of(instanceMetaData));
        stackDto = new StackDto(stack, null, null, null, null, null,
                Map.of("test", instanceGroupDto), null, null, null, null,
                null, null, null, null, null,
                null, null, null);

        Node node = new Node("privateIp", "publicIp", "instanceId", "instanceType", FQDN, "hostGroup");
        lenient().when(stackUtil.collectNodes(stack)).thenReturn(Set.of(node));
        lenient().when(saltOrchestrator.getResponsiveNodes(any(), any(), anyBoolean())).thenReturn(new NodeReachabilityResult(Set.of(node), null));
        lenient().doCallRealMethod().when(resourceAttributeUtil).getTypedAttributes(any(), eq(VolumeSetAttributes.class));
    }

    @Test
    void getStackPatchType() {
        assertEquals(StackPatchType.FSTAB_VALIDATION, underTest.getStackPatchType());
    }

    @Test
    void testIsAffectedWhenNoVolumeSets() {
        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), anyList())).thenReturn(Collections.emptyList());
        assertFalse(underTest.isAffected(stack));
    }

    @Test
    void testIsAffectedWhenUnsupportedCloudProvider() {
        stack.setCloudPlatform("YARN");
        assertFalse(underTest.isAffected(stack));
    }

    @Test
    void testIsAffectedWhenFstabAndResourceVolumeSizeAreSame() {
        Resource resource = createVolumeSetResource(FSTAB_FROM_DB);
        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), anyList())).thenReturn(List.of(resource));
//        when(saltOrchestrator.getFstabInformation(any(), any(), anySet())).thenReturn(Map.of(FQDN, Map.of("uuid", "", "fstab", FSTAB_FROM_DB)));

        assertFalse(underTest.isAffected(stack));
    }

    @Test
    void testIsAffectedWhenFstabAndResourceVolumeSizeAreDifferent() {
        Resource resource = createVolumeSetResource(FSTAB_FROM_DB_2);
        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), anyList())).thenReturn(List.of(resource));
//        when(saltOrchestrator.getFstabInformation(any(), any(), anySet())).thenReturn(Map.of(FQDN, Map.of("uuid", "", "fstab", FSTAB_FROM_HOST)));

        assertTrue(underTest.isAffected(stack));
    }

    @Test
    void testDoApplyWhenFstabsAreDifferent() throws ExistingStackPatchApplyException {
        Resource resource = createVolumeSetResource(FSTAB_FROM_DB);
        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), anyList())).thenReturn(List.of(resource));
        when(saltOrchestrator.getFstabInformation(any(), any(), anySet())).thenReturn(Map.of(FQDN, Map.of("uuid", "", "fstab", FSTAB_FROM_HOST)));

        assertTrue(underTest.doApply(stack));
        verify(resourceService, times(1)).save(any(Resource.class));
    }

    @Test
    void testDoApplyWhenFstabsAreSame() throws ExistingStackPatchApplyException {
        Resource resource = createVolumeSetResource(FSTAB_FROM_DB);
        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), anyList())).thenReturn(List.of(resource));
        when(saltOrchestrator.getFstabInformation(any(), any(), anySet())).thenReturn(Map.of(FQDN, Map.of("uuid", "", "fstab", FSTAB_FROM_DB)));

        assertTrue(underTest.doApply(stack));
        verify(resourceService, never()).save(any(Resource.class));
    }

    @Test
    void testDoApplyWhenOrchestratorFails() {
        Resource resource = createVolumeSetResource(FSTAB_FROM_DB);
        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), anyList())).thenReturn(List.of(resource));
        when(saltOrchestrator.getFstabInformation(any(), any(), anySet())).thenThrow(new RuntimeException("error"));

        assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(stack));
    }

    private Resource createVolumeSetResource(String fstab) {
        Resource resource = new Resource();
        resource.setInstanceId(INSTANCE_ID);
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        VolumeSetAttributes.Volume vol = new VolumeSetAttributes.Volume("", "/dev/test", 100, "test", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes attributes = new VolumeSetAttributes("az", false, "", List.of(vol), 100, "standard");
        attributes.setFstab(fstab);
        resource.setAttributes(new Json(attributes));
        return resource;
    }

    @Test
    void testFstabNormalization() {
        String duplicatedFstab = FSTAB_FROM_HOST + "\n" + FSTAB_FROM_HOST;
        assertEquals(FSTAB_FROM_HOST, underTest.normalizeFstab(duplicatedFstab));
    }
}