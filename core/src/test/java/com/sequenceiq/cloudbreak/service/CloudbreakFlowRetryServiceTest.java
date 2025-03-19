package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsEvent.UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsState.UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade.UpgradeRdsFlowConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.domain.RetryResponse;
import com.sequenceiq.flow.domain.StateStatus;
import com.sequenceiq.flow.service.FlowRetryService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@ExtendWith(MockitoExtension.class)
class CloudbreakFlowRetryServiceTest {
    private static final Long STACK_ID = 49L;

    private static final NameOrCrn NAME_OR_CRN = NameOrCrn.ofName("Name");

    @Mock
    private StackService stackService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FlowRetryService flowRetryService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @InjectMocks
    private CloudbreakFlowRetryService underTest;

    @Test
    void testRetryRedbeamsRetryNecessary() {
        when(stackService.getIdByNameOrCrnInWorkspace(NAME_OR_CRN, 1L)).thenReturn(STACK_ID);
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(UpgradeRdsFlowConfig.class));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        when(flowRetryService.retry(eq(STACK_ID), any(Consumer.class))).thenAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return new RetryResponse("name", flowIdentifier);
        });
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn("dbcrn");
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);

        FlowIdentifier actualResult = underTest.retryInWorkspace(NAME_OR_CRN, 1L);

        assertEquals(flowIdentifier, actualResult);
        verify(databaseServerV4Endpoint).retry("dbcrn");
    }

    @Test
    void testRetryRedbeamsRetryNecessaryBadRequest() {
        when(stackService.getIdByNameOrCrnInWorkspace(NAME_OR_CRN, 1L)).thenReturn(STACK_ID);
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(UpgradeRdsFlowConfig.class));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        when(flowRetryService.retry(eq(STACK_ID), any(Consumer.class))).thenAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return new RetryResponse("name", flowIdentifier);
        });
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn("dbcrn");
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);
        when(databaseServerV4Endpoint.retry("dbcrn")).thenThrow(new BadRequestException("bad request"));

        FlowIdentifier actualResult = underTest.retryInWorkspace(NAME_OR_CRN, 1L);

        assertEquals(flowIdentifier, actualResult);
        verify(databaseServerV4Endpoint).retry("dbcrn");
    }

    @Test
    void testRetryNoRedbeamsRetryNecessary() {
        when(stackService.getIdByNameOrCrnInWorkspace(NAME_OR_CRN, 1L)).thenReturn(STACK_ID);
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(UPGRADE_RDS_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(UpgradeRdsFlowConfig.class));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        when(flowRetryService.retry(eq(STACK_ID), any(Consumer.class))).thenAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return new RetryResponse("name", flowIdentifier);
        });

        FlowIdentifier actualResult = underTest.retryInWorkspace(NAME_OR_CRN, 1L);

        assertEquals(flowIdentifier, actualResult);
        verify(databaseServerV4Endpoint, never()).retry(any());
        verify(stackDtoService, never()).getClusterViewByStackId(STACK_ID);
    }

    @Test
    void testRetryNoRedbeamsRetryNecessaryEmptyEvent() {
        when(stackService.getIdByNameOrCrnInWorkspace(NAME_OR_CRN, 1L)).thenReturn(STACK_ID);
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(UpgradeRdsFlowConfig.class));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        when(flowRetryService.retry(eq(STACK_ID), any(Consumer.class))).thenAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return new RetryResponse("name", flowIdentifier);
        });

        FlowIdentifier actualResult = underTest.retryInWorkspace(NAME_OR_CRN, 1L);

        assertEquals(flowIdentifier, actualResult);
        verify(databaseServerV4Endpoint, never()).retry(any());
        verify(stackDtoService, never()).getClusterViewByStackId(STACK_ID);
    }

    @Test
    void testRetryRedbeamsRetryNecessaryButNoDBCrn() {
        when(stackService.getIdByNameOrCrnInWorkspace(NAME_OR_CRN, 1L)).thenReturn(STACK_ID);
        FlowLog successfulFlowLog = new FlowLog();
        successfulFlowLog.setFlowId("FLOW_ID_1");
        successfulFlowLog.setStateStatus(StateStatus.SUCCESSFUL);
        successfulFlowLog.setNextEvent(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_FINISHED_EVENT.name());
        successfulFlowLog.setCreated(1L);
        successfulFlowLog.setCurrentState(UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE.name());
        successfulFlowLog.setFlowType(ClassValue.of(UpgradeRdsFlowConfig.class));
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flowid");
        when(flowRetryService.retry(eq(STACK_ID), any(Consumer.class))).thenAnswer(invocation -> {
            ((Consumer<FlowLog>) invocation.getArgument(1)).accept(successfulFlowLog);
            return new RetryResponse("name", flowIdentifier);
        });
        Cluster cluster = new Cluster();
        when(stackDtoService.getClusterViewByStackId(STACK_ID)).thenReturn(cluster);

        FlowIdentifier actualResult = underTest.retryInWorkspace(NAME_OR_CRN, 1L);

        assertEquals(flowIdentifier, actualResult);
        verify(databaseServerV4Endpoint, never()).retry(any());
    }
}
