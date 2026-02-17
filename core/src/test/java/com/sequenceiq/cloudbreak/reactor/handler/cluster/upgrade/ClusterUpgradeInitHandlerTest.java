package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeInitSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.parcel.UpgradeCandidateProvider;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePrerequisitesService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeInitHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ParcelService parcelService;

    @Mock
    private ClusterUpgradePrerequisitesService clusterUpgradePrerequisitesService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private UpgradeCandidateProvider upgradeCandidateProvider;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterApi connector;

    @InjectMocks
    private ClusterUpgradeInitHandler underTest;

    @Test
    void testDoAcceptSuccess() throws Exception {
        String targetRuntimeVersion = "7.3.2";
        Set<ClusterComponentView> components = new HashSet<>();
        Set<ClouderaManagerProduct> upgradeCandidateProducts = new HashSet<>();
        ClusterUpgradeInitRequest request = new ClusterUpgradeInitRequest(STACK_ID, targetRuntimeVersion, OsType.RHEL8);
        HandlerEvent<ClusterUpgradeInitRequest> event = new HandlerEvent<>(new Event<>(request));

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelService.getParcelComponentsByBlueprint(stackDto)).thenReturn(components);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        when(upgradeCandidateProvider.getRequiredProductsForUpgrade(connector, stackDto, components)).thenReturn(upgradeCandidateProducts);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ClusterUpgradeInitSuccess.class, result);
        assertEquals("CLUSTERUPGRADEINITSUCCESS", result.getSelector());
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(upgradeCandidateProducts, ((ClusterUpgradeInitSuccess) result).getUpgradeCandidateProducts());
        assertEquals(OsType.RHEL8, ((ClusterUpgradeInitSuccess) result).getOriginalOsType());

        verify(parcelService).removeUnusedParcelComponents(stackDto, components);
        verify(clusterUpgradePrerequisitesService).removeIncompatibleServices(stackDto, connector, targetRuntimeVersion);

    }

    @Test
    void testDoAcceptFailure() throws Exception {
        String targetRuntimeVersion = "7.3.2";
        Set<ClusterComponentView> components = new HashSet<>();
        ClusterUpgradeInitRequest request = new ClusterUpgradeInitRequest(STACK_ID, targetRuntimeVersion, OsType.RHEL8);
        HandlerEvent<ClusterUpgradeInitRequest> event = new HandlerEvent<>(new Event<>(request));

        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelService.getParcelComponentsByBlueprint(stackDto)).thenReturn(components);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(connector);
        doThrow(new RuntimeException("error")).when(clusterUpgradePrerequisitesService).removeIncompatibleServices(stackDto, connector, targetRuntimeVersion);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(ClusterUpgradeFailedEvent.class, result);
        verify(parcelService).removeUnusedParcelComponents(stackDto, components);
        verify(clusterUpgradePrerequisitesService).removeIncompatibleServices(stackDto, connector, targetRuntimeVersion);
    }

}