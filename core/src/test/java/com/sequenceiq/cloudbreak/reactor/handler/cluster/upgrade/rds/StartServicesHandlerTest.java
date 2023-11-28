package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMServicesRequest;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class StartServicesHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private HandlerEvent<UpgradeRdsStartCMServicesRequest> event;

    @Mock
    private UpgradeRdsService upgradeRdsService;

    @InjectMocks
    private StartServicesHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSSTARTCMSERVICESREQUEST");
    }

    @Test
    void testDoAcceptWhenNoSkipStartStopEntitlement() throws CloudbreakException {
        // GIVEN
        UpgradeRdsStartCMServicesRequest request = new UpgradeRdsStartCMServicesRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(mock(ClusterApi.class));
        // WHEN
        Selectable actualSelectable = underTest.doAccept(event);
        // THEN
        verify(clusterApiConnectors.getConnector(stackDto), never()).startCluster();
        assertThat(actualSelectable.selector()).isEqualTo("UPGRADERDSSTARTCMSERVICESRESULT");
    }

    @Test
    void testDoAcceptWhenSkipStartStopEntitlementIsTrue() throws CloudbreakException {
        // GIVEN
        UpgradeRdsStartCMServicesRequest request = new UpgradeRdsStartCMServicesRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(mock(ClusterApi.class));
        when(upgradeRdsService.shouldStopStartServices(stackView)).thenReturn(true);
        // WHEN
        Selectable actualSelectable = underTest.doAccept(event);
        // THEN
        verify(clusterApiConnectors.getConnector(stackDto), times(1)).startCluster();
        assertThat(actualSelectable.selector()).isEqualTo("UPGRADERDSSTARTCMSERVICESRESULT");
    }

    @Test
    void testDoAcceptThrowsException() throws Exception {
        // GIVEN
        UpgradeRdsStartCMServicesRequest request = new UpgradeRdsStartCMServicesRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(upgradeRdsService.shouldStopStartServices(stackView)).thenReturn(true);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        doThrow(new CloudbreakException("exception")).when(clusterApi).startCluster();
        // WHEN
        Selectable actualSelectable = underTest.doAccept(event);
        // THEN
        assertThat(actualSelectable.selector()).isEqualTo("UPGRADERDSFAILEDEVENT");
    }
}
