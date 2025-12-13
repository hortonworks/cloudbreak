package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.ManualClusterRepairMode;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.model.RepairValidation;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackUpgradeService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class OSUpgradeByUpgradeSetsServiceTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private ClusterRepairService clusterRepairService;

    @Mock
    private StackUpgradeService stackUpgradeService;

    @InjectMocks
    private OSUpgradeByUpgradeSetsService osUpgradeByUpgradeSetsService;

    @Test
    public void testUpgradeIfOrderNumberDuplicated() {
        StackView stackView = mock(StackView.class);
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3", "i-5")));
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-4")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-6")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stackView, new ImageChangeDto(1L, "imageId"), upgradeSets));
        assertEquals("There are duplicated order number(s): 0 appears 2 times", badRequestException.getMessage());
    }

    @Test
    public void testUpgradeIfElementsDuplicated() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3", "i-5")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-1", "i-4")));
        upgradeSets.add(new OrderedOSUpgradeSet(2, Set.of("i-6")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stackView, new ImageChangeDto(1L, "imageId"), upgradeSets));
        assertEquals("There are duplicated element(s): i-1 appears 2 times", badRequestException.getMessage());
    }

    @Test
    public void testUpgradeIfMissingInstances() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        StackDto stackDto = mock(StackDto.class);

        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceId("i-1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceId("i-2");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceId("i-3");
        InstanceMetaData instanceMetaData4 = new InstanceMetaData();
        instanceMetaData4.setInstanceId("i-4");

        when(stackDto.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetaData1, instanceMetaData2, instanceMetaData3, instanceMetaData4));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-2")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stackView, new ImageChangeDto(1L, "imageId"), upgradeSets));
        assertEquals("All instance must be selected, following instances are not selected for upgrade: [i-4]", badRequestException.getMessage());
    }

    @Test
    public void testUpgradeIfAnInstanceDoesNotExists() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        StackDto stackDto = mock(StackDto.class);

        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceId("i-1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceId("i-2");

        when(stackDto.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetaData1, instanceMetaData2));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-2")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stackView, new ImageChangeDto(1L, "imageId"), upgradeSets));
        assertEquals("These instances does not exists: [i-3]", badRequestException.getMessage());
    }

    @Test
    public void testUpgradeIfRepairValidationFails() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        StackDto stackDto = mock(StackDto.class);

        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceId("i-1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceId("i-2");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceId("i-3");
        InstanceMetaData instanceMetaData4 = new InstanceMetaData();
        instanceMetaData4.setInstanceId("i-4");

        when(clusterRepairService.validateRepairConditions(eq(ManualClusterRepairMode.ALL), eq(stackDto), anySet()))
                .thenReturn(Optional.of(new RepairValidation(List.of("validation error"))));
        when(stackDto.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetaData1, instanceMetaData2, instanceMetaData3, instanceMetaData4));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-2", "i-4")));
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stackView, new ImageChangeDto(1L, "imageId"), upgradeSets));
        assertEquals("validation error", badRequestException.getMessage());
    }

    @Test
    public void testUpgradeEverythingIsFine() {
        StackView stackView = mock(StackView.class);
        when(stackView.getId()).thenReturn(1L);
        StackDto stackDto = mock(StackDto.class);

        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceId("i-1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceId("i-2");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceId("i-3");
        InstanceMetaData instanceMetaData4 = new InstanceMetaData();
        instanceMetaData4.setInstanceId("i-4");

        when(stackDto.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetaData1, instanceMetaData2, instanceMetaData3, instanceMetaData4));
        when(stackDtoService.getById(1L)).thenReturn(stackDto);
        ArrayList<OrderedOSUpgradeSet> upgradeSets = new ArrayList<>();
        upgradeSets.add(new OrderedOSUpgradeSet(0, Set.of("i-1", "i-3")));
        upgradeSets.add(new OrderedOSUpgradeSet(1, Set.of("i-2", "i-4")));
        ImageChangeDto imageChangeDto = new ImageChangeDto(1L, "imageId");
        when(stackUpgradeService.calculateUpgradeVariant(eq(stackView), anyString(), eq(false))).thenReturn("AWS_NATIVE");

        ThreadBasedUserCrnProvider.doAs("somebody", () -> osUpgradeByUpgradeSetsService.osUpgradeByUpgradeSets(stackView, imageChangeDto, upgradeSets));

        ArgumentCaptor<Set> selectedPartsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(clusterRepairService, times(1)).validateRepairConditions(eq(ManualClusterRepairMode.ALL), eq(stackDto), selectedPartsCaptor.capture());
        assertEquals(0, selectedPartsCaptor.getValue().size());
        verify(flowManager, times(1)).triggerOsUpgradeByUpgradeSetsFlow(1L, "AWS_NATIVE", imageChangeDto, upgradeSets);
    }
}