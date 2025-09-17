package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CDH_BUILD_NUMBER;
import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.STACK;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.ClusterUpgradeEvent.CLUSTER_UPGRADE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_NOT_NEEDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.message.FlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.CdhPatchVersionProvider;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String REMOTE_DATA_CONTEXT = "remote-data-context";

    private static final String STACK_NAME = "stack-name";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ParcelService parcelService;

    @Mock
    private ClusterBuilderService clusterBuilderService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private FlowMessageService flowMessageService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterApi connector;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Spy
    private CdhPatchVersionProvider cdhPatchVersionProvider = new CdhPatchVersionProvider();

    private Set<ClusterComponentView> components = Collections.singleton(new ClusterComponentView());

    private Set<ClouderaManagerProduct> upgradeCandidateProducts = Collections.singleton(new ClouderaManagerProduct());

    @InjectMocks
    private ClusterUpgradeHandler underTest;

    @Test
    void testDoAcceptShouldReturnSuccessResponseWhenThereAreUpgradeCandidatesAndTheStackIsDataLake() throws Exception {
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(STACK_ID, upgradeCandidateProducts, true, true);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        when(stackDto.getStack()).thenReturn(createStack(StackType.DATALAKE));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(createParcelOperationStatus(true));
        when(stackDto.getName()).thenReturn(STACK_NAME);
        Image image = Image.builder().withPackageVersions(Map.of(CDH_BUILD_NUMBER.getKey(), "1234", STACK.getKey(), "7.2.18")).build();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(image);
        when(connector.getStackCdhVersion(STACK_NAME)).thenReturn("7.2.18-1.cdh7.2.18.p1101.68994679");

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FINISHED_EVENT.event(), result.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
        verify(connector).upgradeClusterRuntime(upgradeCandidateProducts, true, Optional.empty(), true);
        verify(parcelService).removeUnusedParcelComponents(stackDto);
    }

    @Test
    void testDoAcceptShouldReturnSuccessResponseWhenThereAreUpgradeCandidatesAndTheStackIsDataHub() throws Exception {
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(STACK_ID, upgradeCandidateProducts, true, true);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        when(stackDto.getStack()).thenReturn(createStack(StackType.WORKLOAD));
        when(stackDto.getName()).thenReturn(STACK_NAME);
        when(clusterBuilderService.getSdxContextOptional(stackDto.getStack().getDatalakeCrn())).thenReturn(Optional.of(REMOTE_DATA_CONTEXT));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(createParcelOperationStatus(true));
        Image image = Image.builder().withPackageVersions(Map.of(CDH_BUILD_NUMBER.getKey(), "1234", STACK.getKey(), "7.2.18")).build();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(image);
        when(connector.getStackCdhVersion(STACK_NAME)).thenReturn("7.2.18-1.cdh7.2.18.p1101.68994679");

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FINISHED_EVENT.event(), result.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
        verify(clusterBuilderService).getSdxContextOptional(stackDto.getStack().getDatalakeCrn());
        verify(connector).upgradeClusterRuntime(upgradeCandidateProducts, true, Optional.of(REMOTE_DATA_CONTEXT), true);
        verify(parcelService).removeUnusedParcelComponents(stackDto);
    }

    @Test
    void testDoAcceptShouldReturnSuccessResponseWhenThereAreUpgradeCandidatesAndNotAPatchUpgrade() throws Exception {
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(STACK_ID, upgradeCandidateProducts, true, true);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        when(stackDto.getStack()).thenReturn(createStack(StackType.WORKLOAD));
        when(stackDto.getName()).thenReturn(STACK_NAME);
        when(clusterBuilderService.getSdxContextOptional(stackDto.getStack().getDatalakeCrn())).thenReturn(Optional.of(REMOTE_DATA_CONTEXT));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(createParcelOperationStatus(true));
        Image image = Image.builder().withPackageVersions(Map.of(CDH_BUILD_NUMBER.getKey(), "1234", STACK.getKey(), "7.3.1")).build();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(image);
        when(connector.getStackCdhVersion(STACK_NAME)).thenReturn("7.2.18-1.cdh7.2.18.p1101.68994679");

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FINISHED_EVENT.event(), result.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
        verify(clusterBuilderService).getSdxContextOptional(stackDto.getStack().getDatalakeCrn());
        verify(connector).upgradeClusterRuntime(upgradeCandidateProducts, false, Optional.of(REMOTE_DATA_CONTEXT), true);
        verify(parcelService).removeUnusedParcelComponents(stackDto);
    }

    @Test
    void testDoAcceptShouldReturnSuccessResponseWhenThereAreNoUpgradeCandidates() {
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(STACK_ID, Collections.emptySet(), true, true);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FINISHED_EVENT.event(), result.selector());
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE_NOT_NEEDED);
        verifyNoInteractions(stackDtoService, clusterService, connector, clusterApiConnectors);
    }

    @Test
    void testDoAcceptShouldReturnFailedResponseWhenThereAreUpgradeCandidatesAndTheUpgradeClusterRuntimeThrowsException() throws Exception {
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(STACK_ID, upgradeCandidateProducts, true, true);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        when(stackDto.getStack()).thenReturn(createStack(StackType.DATALAKE));
        when(stackDto.getName()).thenReturn(STACK_NAME);
        Image image = Image.builder().withPackageVersions(Map.of(CDH_BUILD_NUMBER.getKey(), "1234", STACK.getKey(), "7.2.18")).build();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(image);
        when(connector.getStackCdhVersion(STACK_NAME)).thenReturn("7.2.18-1.cdh7.2.18.p1101.68994679");
        doThrow(new CloudbreakException("error")).when(connector).upgradeClusterRuntime(upgradeCandidateProducts, true, Optional.empty(), true);

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FAILED_EVENT.event(), result.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
        verify(connector).upgradeClusterRuntime(upgradeCandidateProducts, true, Optional.empty(), true);
    }

    @Test
    void testDoAcceptShouldReturnFailedResponseWhenParcelRemovalFailedAfterTheUpgrade() throws Exception {
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(STACK_ID, upgradeCandidateProducts, true, true);

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        when(stackDto.getStack()).thenReturn(createStack(StackType.DATALAKE));
        when(parcelService.removeUnusedParcelComponents(stackDto)).thenReturn(createParcelOperationStatus(false));
        when(stackDto.getName()).thenReturn(STACK_NAME);
        Image image = Image.builder().withPackageVersions(Map.of(CDH_BUILD_NUMBER.getKey(), "1234", STACK.getKey(), "7.2.18")).build();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(image);
        when(connector.getStackCdhVersion(STACK_NAME)).thenReturn("7.2.18-1.cdh7.2.18.p1101.68994679");

        Selectable result = underTest.doAccept(new HandlerEvent<>(Event.wrap(request)));

        assertEquals(CLUSTER_UPGRADE_FAILED_EVENT.event(), result.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(clusterService).updateClusterStatusByStackId(STACK_ID, DetailedStackStatus.CLUSTER_UPGRADE_IN_PROGRESS);
        verify(flowMessageService).fireEventAndLog(STACK_ID, Status.UPDATE_IN_PROGRESS.name(), CLUSTER_UPGRADE);
        verify(connector).upgradeClusterRuntime(upgradeCandidateProducts, true, Optional.empty(), true);
        verify(parcelService).removeUnusedParcelComponents(stackDto);
    }

    private StackView createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        stack.setDatalakeCrn("crn");
        return stack;
    }

    private ParcelOperationStatus createParcelOperationStatus(boolean success) {
        return new ParcelOperationStatus(Collections.emptyMap(), success ? Collections.emptyMap() : Map.of("cdh", "7.3.2"));
    }

}