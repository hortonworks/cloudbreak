package com.sequenceiq.freeipa.service.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class InstanceMetaDataServiceTest {

    private static final String ENVIRONMENT_ID = "crn:cdp:environments:us-west-1:f39af961-e0ce-4f79-826c-45502efb9ca3:environment:12345-6789";

    private static final String INSTANCE_ID_1 = "instance_1";

    private static final String INSTANCE_ID_2 = "instance_2";

    private static final String INSTANCE_ID_3 = "instance_3";

    private static final String GROUP_NAME = "group_1";

    private static final long STACK_ID = 1L;

    private static final long INSTANCE_PRIVATE_ID_1 = 1L;

    private static final long INSTANCE_PRIVATE_ID_2 = 2L;

    private static final long INSTANCE_PRIVATE_ID_3 = 3L;

    @InjectMocks
    private InstanceMetaDataService underTest;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    private Set<InstanceMetaData> getInstancesFromStack() {
        Stack stack = initializeStackWithInstanceGroup();
        return stack.getInstanceGroups().stream()
                .flatMap(instanceGroup -> instanceGroup.getInstanceMetaData().stream()).collect(Collectors.toSet());
    }

    @Test
    public void testUpdateStatusSuccess() {
        Stack stack = initializeStackWithInstanceGroup();
        when(instanceMetaDataRepository.findAllInStack(STACK_ID)).thenReturn(getInstancesFromStack());

        underTest.updateStatus(stack, List.of(INSTANCE_ID_1), InstanceStatus.CREATED);

        verify(instanceMetaDataRepository, times(1)).save(any());
    }

    @Test
    public void testUpdateMultipleStatusSuccess() {
        Stack stack = initializeStackWithInstanceGroup();
        when(instanceMetaDataRepository.findAllInStack(STACK_ID)).thenReturn(getInstancesFromStack());

        underTest.updateStatus(stack, List.of(INSTANCE_ID_1, INSTANCE_ID_2), InstanceStatus.CREATED);

        verify(instanceMetaDataRepository, times(2)).save(any());
    }

    @Test
    public void testUpdateStatusInvalidId() {
        Stack stack = initializeStackWithInstanceGroup();
        when(instanceMetaDataRepository.findAllInStack(STACK_ID)).thenReturn(getInstancesFromStack());

        underTest.updateStatus(stack, List.of(INSTANCE_ID_3), InstanceStatus.CREATED);

        verify(instanceMetaDataRepository, times(0)).save(any());
    }

    @Test
    public void testSaveInstanceAndGetUpdatedStackWhenAvailabilityZoneDataIsAvailable() {
        Stack stack = initializeStackWithInstanceGroup();
        InstanceGroup instanceGroup = stack.getInstanceGroups().stream().findFirst().get();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setHostname("ipa");
        freeIpa.setDomain("dom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        InstanceTemplate template = mock(InstanceTemplate.class);
        when(template.getGroupName()).thenReturn(GROUP_NAME);
        when(template.getPrivateId()).thenReturn(INSTANCE_PRIVATE_ID_3);
        List<CloudInstance> cloudInstances = List.of(new CloudInstance(INSTANCE_ID_3, template, null, "subnet-1", "az1"));
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_ID)).thenReturn(environmentResponse);
        Map<String, String> subnetAzMap = Map.of("aSubnetId", "anAvailabilityZoneId");
        when(multiAzCalculatorService.prepareSubnetAzMap(environmentResponse)).thenReturn(subnetAzMap);
        Map<String, Integer> subnetUsage = Map.of();
        when(multiAzCalculatorService.calculateCurrentSubnetUsage(subnetAzMap, instanceGroup)).thenReturn(subnetUsage);
        when(multiAzCalculatorService.filterSubnetByLeastUsedAz(instanceGroup, subnetAzMap)).thenReturn(subnetAzMap);

        Stack actualStack = underTest.saveInstanceAndGetUpdatedStack(stack, cloudInstances);

        verify(instanceMetaDataRepository).save(any());
        assertEquals(3, actualStack.getAllInstanceMetaDataList().size());
        InstanceGroup actualInstanceGroup = actualStack.getInstanceGroups().stream().filter(ig -> GROUP_NAME.equals(ig.getGroupName())).findFirst().get();
        InstanceMetaData instanceMetaData = actualInstanceGroup.getInstanceMetaData().stream()
                .filter(im -> INSTANCE_PRIVATE_ID_3 == im.getPrivateId())
                .findFirst().get();
        assertEquals("ipa3.dom", instanceMetaData.getDiscoveryFQDN());
        verify(multiAzCalculatorService).filterSubnetByLeastUsedAz(actualInstanceGroup, subnetAzMap);
        verify(multiAzCalculatorService).updateSubnetIdForSingleInstanceIfEligible(subnetAzMap, subnetUsage, instanceMetaData, actualInstanceGroup);
    }

    @Test
    public void testSaveInstanceAndGetUpdatedStackWhenNoAvailabilityZoneDataAvailable() {
        Stack stack = initializeStackWithInstanceGroup();
        InstanceGroup instanceGroup = stack.getInstanceGroups().stream().findFirst().get();
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setHostname("ipa");
        freeIpa.setDomain("dom");
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        InstanceTemplate template = mock(InstanceTemplate.class);
        when(template.getGroupName()).thenReturn(GROUP_NAME);
        when(template.getPrivateId()).thenReturn(INSTANCE_PRIVATE_ID_3);
        List<CloudInstance> cloudInstances = List.of(new CloudInstance(INSTANCE_ID_3, template, null, "subnet-1", "az1"));
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_ID)).thenReturn(environmentResponse);
        Map<String, String> subnetAzMap = Map.of();
        when(multiAzCalculatorService.prepareSubnetAzMap(environmentResponse)).thenReturn(subnetAzMap);
        Map<String, Integer> subnetUsage = Map.of();
        when(multiAzCalculatorService.calculateCurrentSubnetUsage(subnetAzMap, instanceGroup)).thenReturn(subnetUsage);

        Stack actualStack = underTest.saveInstanceAndGetUpdatedStack(stack, cloudInstances);

        verify(instanceMetaDataRepository).save(any());
        assertEquals(3, actualStack.getAllInstanceMetaDataList().size());
        InstanceGroup actualInstanceGroup = actualStack.getInstanceGroups().stream().filter(ig -> GROUP_NAME.equals(ig.getGroupName())).findFirst().get();
        InstanceMetaData instanceMetaData = actualInstanceGroup.getInstanceMetaData().stream()
                .filter(im -> INSTANCE_PRIVATE_ID_3 == im.getPrivateId())
                .findFirst().get();
        assertEquals("ipa3.dom", instanceMetaData.getDiscoveryFQDN());
        verify(multiAzCalculatorService, times(0)).filterSubnetByLeastUsedAz(actualInstanceGroup, subnetAzMap);
        verify(multiAzCalculatorService, times(0)).updateSubnetIdForSingleInstanceIfEligible(subnetAzMap, subnetUsage, instanceMetaData, actualInstanceGroup);
    }

    @Test
    public void testGetNonPrimaryGwInstances() {

        Set<InstanceMetaData> nonPrimaryGwInstances = underTest.getNonPrimaryGwInstances(createValidImSet());
        assertEquals(2, nonPrimaryGwInstances.size());
        assertTrue(nonPrimaryGwInstances.stream().anyMatch(im -> im.getInstanceId().equals("im2")));
        assertTrue(nonPrimaryGwInstances.stream().anyMatch(im -> im.getInstanceId().equals("im3")));
        assertTrue(nonPrimaryGwInstances.stream().noneMatch(im -> im.getInstanceId().equals("pgw")));
    }

    @Test
    public void testGetPrimaryGwInstance() {

        InstanceMetaData primaryGwInstance = underTest.getPrimaryGwInstance(createValidImSet());
        assertEquals("pgw", primaryGwInstance.getInstanceId());
        assertEquals(InstanceMetadataType.GATEWAY_PRIMARY, primaryGwInstance.getInstanceMetadataType());
    }

    private Stack initializeStackWithInstanceGroup() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_ID);
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
        return stack;
    }

    private Set<InstanceMetaData> createValidImSet() {
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        im1.setInstanceId("pgw");
        InstanceMetaData im2 = new InstanceMetaData();
        im2.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        im2.setInstanceId("im2");
        InstanceMetaData im3 = new InstanceMetaData();
        im3.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
        im3.setInstanceId("im3");
        return Set.of(im1, im2, im3);
    }
}
