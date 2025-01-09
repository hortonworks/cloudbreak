package com.sequenceiq.redbeams.flow.redbeams.upgrade.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
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

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
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
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
class RedbeamsValidateUpgradeCleanupActionsTest {

    private static final Long RESOURCE_ID = 1234L;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private RedbeamsValidateUpgradeCleanupActions underTest;

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

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @BeforeEach
    void setUp() {
        context = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    private AbstractRedbeamsUpgradeAction<RedbeamsStartValidateUpgradeCleanupRequest> getValidateUpgradeDatabaseServerCleanupAction() {
        return (AbstractRedbeamsUpgradeAction<RedbeamsStartValidateUpgradeCleanupRequest>) underTest.validateUpgradeDatabaseServerCleanup();
    }

    @Test
    void validateUpgradeDatabaseServerCleanupTestCreateRequest() throws Exception {
        RedbeamsStartValidateUpgradeCleanupRequest payload = new RedbeamsStartValidateUpgradeCleanupRequest(RESOURCE_ID);

        AbstractRedbeamsUpgradeAction<RedbeamsStartValidateUpgradeCleanupRequest> validateUpgradeDatabaseServerCleanupAction =
                getValidateUpgradeDatabaseServerCleanupAction();
        initActionPrivateFields(validateUpgradeDatabaseServerCleanupAction);

        AbstractActionTestSupport<RedbeamsUpgradeState, RedbeamsUpgradeEvent, RedbeamsContext, RedbeamsStartValidateUpgradeCleanupRequest> underTest
                = new AbstractActionTestSupport<>(validateUpgradeDatabaseServerCleanupAction);
        underTest.doExecute(context, payload, new HashMap<>());

        verify(reactorEventFactory).createEvent(anyMap(), payloadArgumentCaptor.capture());
        ValidateUpgradeDatabaseServerCleanupRequest request = (ValidateUpgradeDatabaseServerCleanupRequest) payloadArgumentCaptor.getValue();
        assertEquals(cloudContext, request.getCloudContext());
        assertEquals(cloudCredential, request.getCloudCredential());
        assertEquals(databaseStack, request.getDatabaseStack());
        assertNull(request.getTargetMajorVersion());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
    }

    @Test
    void validateUpgradeFinishedTestDoExecute() {
        ValidateUpgradeDatabaseServerCleanupSuccess payload = new ValidateUpgradeDatabaseServerCleanupSuccess(RESOURCE_ID);
        AbstractRedbeamsUpgradeAction<ValidateUpgradeDatabaseServerCleanupSuccess> action =
                (AbstractRedbeamsUpgradeAction<ValidateUpgradeDatabaseServerCleanupSuccess>) underTest.validateUpgradeFinished();

        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.AVAILABLE);
        verify(metricService).incrementMetricCounter(eq(MetricType.DB_VALIDATE_UPGRADE_CLEANUP_FINISHED), any(Optional.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validateUpgradeFailedTestDoExecute(boolean cancellationException) {
        RedbeamsFailureEvent payload = new RedbeamsFailureEvent(RESOURCE_ID, cancellationException ?
                new CancellationException("cancelled") :
                new Exception("Failure message"));
        AbstractRedbeamsFailureAction<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent> action =
                (AbstractRedbeamsFailureAction<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent>) underTest.validateUpgradeFailed();

        new AbstractActionTestSupport<>(action).prepareExecution(payload, Map.of());

        if (cancellationException) {
            verifyNoInteractions(dbStackStatusUpdater);
            verifyNoInteractions(metricService);
        } else {
            verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.VALIDATE_UPGRADE_CLEANUP_FAILED, "Failure message");
            verify(metricService).incrementMetricCounter(eq(MetricType.DB_VALIDATE_UPGRADE_CLEANUP_FAILED), any(Optional.class));
        }
    }
}