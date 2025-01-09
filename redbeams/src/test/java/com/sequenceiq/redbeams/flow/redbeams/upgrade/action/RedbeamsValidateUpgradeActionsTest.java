package com.sequenceiq.redbeams.flow.redbeams.upgrade.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.AbstractRedbeamsUpgradeAction;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
class RedbeamsValidateUpgradeActionsTest {

    private static final Long RESOURCE_ID = 1234L;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private RedbeamsValidateUpgradeActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Captor
    private ArgumentCaptor<Object> payloadArgumentCaptor;

    private RedbeamsContext context;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private DBStack dbStack;

    @BeforeEach
    void setUp() {
        context = new RedbeamsContext(flowParameters, null, null, null, dbStack);
    }

    private AbstractRedbeamsUpgradeAction<RedbeamsStartValidateUpgradeRequest> getValidateUpgradeDatabaseServerAction() {
        return (AbstractRedbeamsUpgradeAction<RedbeamsStartValidateUpgradeRequest>) underTest.validateUpgradeDatabaseServer();
    }

    @Test
    void validateUpgradeDatabaseServerTestCreateRequest() throws Exception {
        TargetMajorVersion targetMajorVersion = mock(TargetMajorVersion.class);
        RedbeamsStartValidateUpgradeRequest payload = new RedbeamsStartValidateUpgradeRequest(RESOURCE_ID, targetMajorVersion, null);

        AbstractRedbeamsUpgradeAction<RedbeamsStartValidateUpgradeRequest> validateUpgradeDatabaseServerAction = getValidateUpgradeDatabaseServerAction();
        initActionPrivateFields(validateUpgradeDatabaseServerAction);

        AbstractActionTestSupport<RedbeamsUpgradeState, RedbeamsUpgradeEvent, RedbeamsContext, RedbeamsStartValidateUpgradeRequest> underTest
                = new AbstractActionTestSupport<>(validateUpgradeDatabaseServerAction);
        underTest.doExecute(context, payload, new HashMap<>());

        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        ValidateUpgradeDatabaseServerRequest request = (ValidateUpgradeDatabaseServerRequest) payloadArgumentCaptor.getValue();
        assertEquals(payload.getTargetMajorVersion(), request.getTargetMajorVersion());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);

    }

    @Test
    void validateUpgradeFinishedTestDoExecute() {
        ValidateUpgradeDatabaseServerSuccess payload = new ValidateUpgradeDatabaseServerSuccess(RESOURCE_ID, List.of(), "Warning message");
        AbstractRedbeamsUpgradeAction<ValidateUpgradeDatabaseServerSuccess> action =
                (AbstractRedbeamsUpgradeAction<ValidateUpgradeDatabaseServerSuccess>) underTest.validateUpgradeFinished();

        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.AVAILABLE, "Warning message");
        verify(metricService).incrementMetricCounter(eq(MetricType.DB_VALIDATE_UPGRADE_FINISHED), any(Optional.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validateUpgradeFailedTestDoExecute(boolean cancellationException) {
        RedbeamsFailureEvent payload = new RedbeamsFailureEvent(RESOURCE_ID, cancellationException ?
                new CancellationException("cancelled") :
                new Exception("Failure message"));
        AbstractRedbeamsFailureAction<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent> action =
                (AbstractRedbeamsFailureAction<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent>) underTest.validateUpgradeFailed();

        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        if (cancellationException) {
            verifyNoInteractions(dbStackStatusUpdater);
            verifyNoInteractions(metricService);
        } else {
            verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.VALIDATE_UPGRADE_FAILED, "Failure message");
            verify(metricService).incrementMetricCounter(eq(MetricType.DB_VALIDATE_UPGRADE_FAILED), any(Optional.class));
        }
    }
}