package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeCleanupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation.ValidateRdsUpgradeFailedEvent;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.UpgradeDatabaseServerV4Response;

@ExtendWith(MockitoExtension.class)
class ValidateRdsUpgradeCleanupHandlerTest {

    private static final String TEST_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private static final long RESOURCE_ID = 1L;

    private static final String DATABASE_CRN = "database-crn";

    @InjectMocks
    private ValidateRdsUpgradeCleanupHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RedbeamsClientService redbeamsClientService;

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Test
    void testSelector() {
        assertEquals("VALIDATERDSUPGRADECLEANUPREQUEST", underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable actual = underTest.defaultFailureEvent(RESOURCE_ID, new Exception(), mock(Event.class));
        assertEquals("VALIDATERDSUPGRADEFAILEDEVENT", actual.selector());
    }

    @Test
    void testDoAcceptShouldReturnSuccessEvent() {
        setUpMocks();
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flow-id");
        response.setFlowIdentifier(flowIdentifier);
        when(redbeamsClientService.validateUpgradeCleanup(DATABASE_CRN)).thenReturn(response);

        Selectable actual = ThreadBasedUserCrnProvider.doAs(TEST_CRN, () -> underTest.doAccept(createEvent()));

        assertInstanceOf(ValidateRdsUpgradeCleanupResult.class, actual);
        ValidateRdsUpgradeCleanupResult result = (ValidateRdsUpgradeCleanupResult) actual;
        assertEquals(flowIdentifier, result.getFlowIdentifier());
    }

    @Test
    void testDoAcceptShouldReturnSuccessEventWithWarning() {
        setUpMocks();
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setWarning(true);
        response.setReason("warning");
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "flow-id");
        response.setFlowIdentifier(flowIdentifier);
        when(redbeamsClientService.validateUpgradeCleanup(DATABASE_CRN)).thenReturn(response);

        Selectable actual = ThreadBasedUserCrnProvider.doAs(TEST_CRN, () -> underTest.doAccept(createEvent()));

        assertInstanceOf(ValidateRdsUpgradeCleanupResult.class, actual);
        ValidateRdsUpgradeCleanupResult result = (ValidateRdsUpgradeCleanupResult) actual;
        assertEquals(flowIdentifier, result.getFlowIdentifier());
        verifyNoInteractions(externalDatabaseService);
    }

    @Test
    void testDoAcceptShouldReturnFailureEventWithError() {
        setUpMocks();
        UpgradeDatabaseServerV4Response response = new UpgradeDatabaseServerV4Response();
        response.setWarning(false);
        response.setReason("error");
        when(redbeamsClientService.validateUpgradeCleanup(DATABASE_CRN)).thenThrow(new CloudbreakServiceException("validation error"));

        Selectable actual = ThreadBasedUserCrnProvider.doAs(TEST_CRN, () -> underTest.doAccept(createEvent()));

        assertInstanceOf(ValidateRdsUpgradeFailedEvent.class, actual);
        verifyNoInteractions(externalDatabaseService);
    }

    @Test
    void testDoAcceptShouldReturnFailureEventWhenRedBeamsEndpointThrowsAnException() {
        setUpMocks();
        doThrow(RuntimeException.class).when(redbeamsClientService).validateUpgradeCleanup(DATABASE_CRN);

        Selectable actual = ThreadBasedUserCrnProvider.doAs(TEST_CRN, () -> underTest.doAccept(createEvent()));

        assertInstanceOf(ValidateRdsUpgradeFailedEvent.class, actual);
        verifyNoInteractions(externalDatabaseService);
    }

    private HandlerEvent<ValidateRdsUpgradeCleanupRequest> createEvent() {
        return new HandlerEvent<>(new Event<>(new ValidateRdsUpgradeCleanupRequest(RESOURCE_ID)));
    }

    private void setUpMocks() {
        when(stackDtoService.getById(RESOURCE_ID)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getDatabaseServerCrn()).thenReturn(DATABASE_CRN);
    }

}