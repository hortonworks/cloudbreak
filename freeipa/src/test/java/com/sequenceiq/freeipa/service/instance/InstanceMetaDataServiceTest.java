package com.sequenceiq.freeipa.service.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class InstanceMetaDataServiceTest {

    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String ACCOUNT_ID = "accountId";

    private static final String INSTANCE_ID_1 = "instance_1";

    private static final String INSTANCE_ID_2 = "instance_2";

    private static final String INSTANCE_ID_3 = "instance_3";

    private static final String GROUP_NAME = "group_1";

    private static final long STACK_ID = 1L;

    private static final long INSTANCE_PRIVATE_ID_1 = 1L;

    private static final long INSTANCE_PRIVATE_ID_2 = 2L;

    private static final long INSTANCE_PRIVATE_ID_3 = 3L;

    private static Stack stack;

    @InjectMocks
    private InstanceMetaDataService underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private FreeIpaService freeIpaService;

    @BeforeAll
    public static void init() {
        stack = new Stack();
        stack.setResourceCrn(ENVIRONMENT_ID);
        stack.setId(STACK_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        stack.getInstanceGroups().add(instanceGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.MASTER);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        instanceGroup.setGroupName(GROUP_NAME);
        instanceMetaData.setDiscoveryFQDN("host1.domain");
        instanceMetaData.setInstanceId(INSTANCE_ID_1);
        instanceMetaData.setPrivateId(INSTANCE_PRIVATE_ID_1);
        instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("host2.domain");
        instanceMetaData.setInstanceId(INSTANCE_ID_2);
        instanceMetaData.setPrivateId(INSTANCE_PRIVATE_ID_2);
        instanceGroup.getInstanceMetaData().add(instanceMetaData);
    }

    private Set<InstanceMetaData> getInstancesFromStack() {
        return stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaData().stream()).collect(Collectors.toSet());
    }

    @Test
    public void testUpdateStatusSuccess() {
        when(instanceMetaDataRepository.findAllInStack(STACK_ID)).thenReturn(getInstancesFromStack());

        underTest.updateStatus(stack, List.of(INSTANCE_ID_1), InstanceStatus.CREATED);

        verify(instanceMetaDataRepository, times(1)).save(any());
    }

    @Test
    public void testUpdateMultipleStatusSuccess() {
        when(instanceMetaDataRepository.findAllInStack(STACK_ID)).thenReturn(getInstancesFromStack());

        underTest.updateStatus(stack, List.of(INSTANCE_ID_1, INSTANCE_ID_2), InstanceStatus.CREATED);

        verify(instanceMetaDataRepository, times(2)).save(any());
    }

    @Test
    public void testUpdateStatusInvalidId() {
        when(instanceMetaDataRepository.findAllInStack(STACK_ID)).thenReturn(getInstancesFromStack());

        underTest.updateStatus(stack, List.of(INSTANCE_ID_3), InstanceStatus.CREATED);

        verify(instanceMetaDataRepository, times(0)).save(any());
    }

    @Test
    public void testSaveInstanceAndGetUpdatedStack() {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setHostname("ipa");
        freeIpa.setDomain("dom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        InstanceTemplate template = mock(InstanceTemplate.class);
        when(template.getGroupName()).thenReturn(GROUP_NAME);
        when(template.getPrivateId()).thenReturn(INSTANCE_PRIVATE_ID_3);
        List<CloudInstance> cloudInstances = List.of(new CloudInstance(INSTANCE_ID_3, template, null, "subnet-1", "az1"));

        Stack stack1 = underTest.saveInstanceAndGetUpdatedStack(stack, cloudInstances);

        verify(instanceMetaDataRepository).save(any());
        assertEquals(3, stack1.getAllInstanceMetaDataList().size());
        InstanceGroup instanceGroup = stack1.getInstanceGroups().stream().filter(ig -> GROUP_NAME.equals(ig.getGroupName())).findFirst().get();
        InstanceMetaData instanceMetaData = instanceGroup.getInstanceMetaData().stream()
                .filter(im -> INSTANCE_PRIVATE_ID_3 == im.getPrivateId())
                .findFirst().get();
        assertEquals("ipa3.dom", instanceMetaData.getDiscoveryFQDN());
    }
}
