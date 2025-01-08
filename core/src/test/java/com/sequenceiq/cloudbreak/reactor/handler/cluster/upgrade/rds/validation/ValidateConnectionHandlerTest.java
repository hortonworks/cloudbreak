package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseConnectionProperties;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeConnectionResult;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeOrchestratorService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ValidateConnectionHandlerTest {

    private static final long RESOURCE_ID = 1L;

    private static final String CONNECTION_URL = "connection-url";

    private static final String USERNAME = "username";

    @InjectMocks
    private ValidateConnectionHandler underTest;

    @Mock
    private RdsUpgradeOrchestratorService rdsUpgradeOrchestratorService;

    @Test
    void testSelector() {
        assertEquals("VALIDATERDSUPGRADECONNECTIONREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable actual = underTest.defaultFailureEvent(RESOURCE_ID, new Exception(), mock(Event.class));
        assertEquals("VALIDATERDSUPGRADEFAILEDEVENT", actual.selector());
    }

    @Test
    void testDoAcceptShouldReturnSuccessEvent() throws CloudbreakOrchestratorException {
        Selectable actual = underTest.doAccept(createEvent());

        assertEquals("VALIDATERDSUPGRADECONNECTIONRESULT", actual.selector());
        verify(rdsUpgradeOrchestratorService).validateDbConnection(RESOURCE_ID, CONNECTION_URL, USERNAME);
    }

    @Test
    void testDoAcceptShouldReturnFailureEventWhenTheValidationThrowsAnException() throws CloudbreakOrchestratorException {
        CloudbreakOrchestratorFailedException error = new CloudbreakOrchestratorFailedException("error");
        doThrow(error).when(rdsUpgradeOrchestratorService).validateDbConnection(RESOURCE_ID, CONNECTION_URL, USERNAME);
        Selectable actual = underTest.doAccept(createEvent());
        assertEquals("VALIDATERDSUPGRADECONNECTIONRESULT", actual.selector());
        assertEquals("error", ((ValidateRdsUpgradeConnectionResult) actual).getReason());
        verify(rdsUpgradeOrchestratorService).validateDbConnection(RESOURCE_ID, CONNECTION_URL, USERNAME);
    }

    private HandlerEvent<ValidateRdsUpgradeConnectionRequest> createEvent() {
        DatabaseConnectionProperties connectionProperties = new DatabaseConnectionProperties();
        connectionProperties.setConnectionUrl(CONNECTION_URL);
        connectionProperties.setUsername(USERNAME);
        ValidateRdsUpgradeConnectionRequest request = new ValidateRdsUpgradeConnectionRequest(RESOURCE_ID, connectionProperties);
        return new HandlerEvent<>(new Event<>(request));
    }
}