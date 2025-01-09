package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.WaitForValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class WaitForValidateRdsUpgradeCleanupHandlerTest {

    private static final Long STACK_ID = 42L;

    private static final String FLOW_ID = "flow_id";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private HandlerEvent<WaitForValidateRdsUpgradeCleanupRequest> event;

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @InjectMocks
    private WaitForValidateRdsUpgradeCleanupHandler underTest;

    private FlowIdentifier flowIdentifier;

    @BeforeEach
    void setup() {
        flowIdentifier = new FlowIdentifier(FlowType.FLOW, FLOW_ID);
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("WAITFORVALIDATERDSUPGRADECLEANUPREQUEST");
    }

    @Test
    void testDoAccept() {
        WaitForValidateRdsUpgradeCleanupRequest request = new WaitForValidateRdsUpgradeCleanupRequest(STACK_ID, "", flowIdentifier);
        when(event.getData()).thenReturn(request);

        ClusterView clusterView = mock(ClusterView.class);
        initStackDto(clusterView);

        Selectable result = underTest.doAccept(event);

        assertThat(result.selector()).isEqualTo("WAITFORVALIDATERDSUPGRADECLEANUPRESULT");
        assertThat(result).isInstanceOf(WaitForValidateRdsUpgradeCleanupResult.class);
    }

    @Test
    void testDoAcceptWhenWarning() {
        WaitForValidateRdsUpgradeCleanupRequest request = new WaitForValidateRdsUpgradeCleanupRequest(STACK_ID, "connection_error_message", flowIdentifier);
        when(event.getData()).thenReturn(request);

        ClusterView clusterView = mock(ClusterView.class);
        initStackDto(clusterView);

        Selectable result = underTest.doAccept(event);

        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
    }

    private void initStackDto(ClusterView clusterView) {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
    }

    @Test
    void testDoAcceptWhenException() {
        WaitForValidateRdsUpgradeCleanupRequest request = new WaitForValidateRdsUpgradeCleanupRequest(STACK_ID, "connection_error_message", flowIdentifier);
        when(event.getData()).thenReturn(request);
        ClusterView clusterView = mock(ClusterView.class);
        UserBreakException userBreakException = new UserBreakException("exception");
        initStackDto(clusterView);
        doThrow(userBreakException).when(externalDatabaseService).waitForDatabaseFlowToBeFinished(eq(clusterView), eq(flowIdentifier));

        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
        assertThat(result.getException()).isEqualTo(userBreakException);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
    }

    @Test
    void testDoAcceptWhenPollerStoppedException() {
        WaitForValidateRdsUpgradeCleanupRequest request = new WaitForValidateRdsUpgradeCleanupRequest(STACK_ID, "connection_error_message", flowIdentifier);
        when(event.getData()).thenReturn(request);
        ClusterView clusterView = mock(ClusterView.class);
        PollerStoppedException pollerStoppedException = new PollerStoppedException("exception");
        initStackDto(clusterView);
        doThrow(pollerStoppedException).when(externalDatabaseService).waitForDatabaseFlowToBeFinished(eq(clusterView), eq(flowIdentifier));

        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
        assertThat(result.getException()).isEqualTo(pollerStoppedException);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
    }

    @Test
    void testDoAcceptWhenPollerException() {
        WaitForValidateRdsUpgradeCleanupRequest request = new WaitForValidateRdsUpgradeCleanupRequest(STACK_ID, "connection_error_message", flowIdentifier);
        when(event.getData()).thenReturn(request);
        ClusterView clusterView = mock(ClusterView.class);
        PollerException pollerException = new PollerException("exception");
        initStackDto(clusterView);
        doThrow(pollerException).when(externalDatabaseService).waitForDatabaseFlowToBeFinished(eq(clusterView), eq(flowIdentifier));

        Selectable result = underTest.doAccept(event);

        assertThat(result).isInstanceOf(ValidateRdsUpgradeFailedEvent.class);
        assertThat(result.getException()).isEqualTo(pollerException);
        assertThat(result.selector()).isEqualTo("VALIDATERDSUPGRADEFAILEDEVENT");
    }
}