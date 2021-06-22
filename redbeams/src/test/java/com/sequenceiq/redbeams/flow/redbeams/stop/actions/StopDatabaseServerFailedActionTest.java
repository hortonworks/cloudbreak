package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopContext;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopState;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
public class StopDatabaseServerFailedActionTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String FLOW_ID = "flowId";

    private static final String USER_NAME = "userName";

    private static final String ACCOUNT_ID = "accountId";

    private static final Long DB_STACK_ID = 12345L;

    private static final String DB_STACK_NAME = "dbStackName";

    private static final String DB_STACK_CLOUD_PLATFORM = "dbStackCloudPlatform";

    private static final String DB_STACK_PLATFORM_VARIANT = "dbStackPlatformVariant";

    private static final String DB_STACK_REGION = "dbStackRegion";

    private static final String DB_STACK_AZ = "dbStackAz";

    private static final String DB_STACK_ENV_ID = "dbStackEnvId";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private DBStack dbStack;

    @Mock
    private Exception exception;

    @Mock
    private Flow flow;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<RedbeamsStopState, RedbeamsStopEvent> stateContext;

    @Mock
    private RedbeamsFailureEvent payload;

    @Mock
    private Crn ownerCrn;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @InjectMocks
    private StopDatabaseServerFailedAction victim;

    @Test
    public void shouldUpdateStatusAndIncrementMetricOnPrepare() {
        RedbeamsFailureEvent event = new RedbeamsFailureEvent(RESOURCE_ID, exception);
        Optional<DBStack> dbStackOptional = Optional.of(dbStack);
        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.STOP_FAILED, null)).thenReturn(dbStackOptional);

        victim.prepareExecution(event, null);

        verify(metricService).incrementMetricCounter(MetricType.DB_STOP_FAILED, dbStackOptional);
    }

    @Test
    public void shouldUpdateStatusWithUknownErrorAndIncrementMetricOnPrepare() {
        RedbeamsFailureEvent event = new RedbeamsFailureEvent(RESOURCE_ID, null);
        Optional<DBStack> dbStackOptional = Optional.of(dbStack);
        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DetailedDBStackStatus.STOP_FAILED, "Unknown error")).thenReturn(Optional.of(dbStack));

        victim.prepareExecution(event, null);

        verify(metricService).incrementMetricCounter(MetricType.DB_STOP_FAILED, dbStackOptional);
    }

    @Test
    public void shouldCreateFlowContext() {
        when(flowParameters.getFlowId()).thenReturn(FLOW_ID);
        when(runningFlows.get(FLOW_ID)).thenReturn(flow);
        when(payload.getException()).thenReturn(exception);
        when(payload.getResourceId()).thenReturn(RESOURCE_ID);
        when(dbStackService.getById(RESOURCE_ID)).thenReturn(dbStack);
        when(dbStack.getOwnerCrn()).thenReturn(ownerCrn);
        when(dbStack.getId()).thenReturn(DB_STACK_ID);
        when(dbStack.getName()).thenReturn(DB_STACK_NAME);
        when(dbStack.getCloudPlatform()).thenReturn(DB_STACK_CLOUD_PLATFORM);
        when(dbStack.getPlatformVariant()).thenReturn(DB_STACK_PLATFORM_VARIANT);
        when(dbStack.getRegion()).thenReturn(DB_STACK_REGION);
        when(dbStack.getAvailabilityZone()).thenReturn(DB_STACK_AZ);
        when(dbStack.getEnvironmentId()).thenReturn(DB_STACK_ENV_ID);
        when(dbStack.getResourceCrn()).thenReturn(CrnTestUtil.getDatabaseServerCrnBuilder()
                        .setAccountId("acc")
                        .setResource("resource")
                        .build());
        when(ownerCrn.getUserId()).thenReturn(USER_NAME);
        when(ownerCrn.getAccountId()).thenReturn(ACCOUNT_ID);
        when(credentialService.getCredentialByEnvCrn(DB_STACK_ENV_ID)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);

        RedbeamsStopContext redbeamsStopContext = victim.createFlowContext(flowParameters, stateContext, payload);

        verify(flow).setFlowFailed(exception);

        assertEquals(flowParameters, redbeamsStopContext.getFlowParameters());
        assertEquals(DB_STACK_ID, redbeamsStopContext.getCloudContext().getId());
        assertEquals(DB_STACK_NAME, redbeamsStopContext.getCloudContext().getName());
        assertEquals(DB_STACK_CLOUD_PLATFORM, redbeamsStopContext.getCloudContext().getPlatform().value());
        assertEquals(DB_STACK_PLATFORM_VARIANT, redbeamsStopContext.getCloudContext().getVariant().value());
        assertEquals(DB_STACK_REGION, redbeamsStopContext.getCloudContext().getLocation().getRegion().value());
        assertEquals(DB_STACK_AZ, redbeamsStopContext.getCloudContext().getLocation().getAvailabilityZone().value());
        assertEquals(USER_NAME, redbeamsStopContext.getCloudContext().getUserId());
        assertEquals(ACCOUNT_ID, redbeamsStopContext.getCloudContext().getAccountId());
        assertEquals(flowParameters, redbeamsStopContext.getFlowParameters());
        assertEquals(cloudCredential, redbeamsStopContext.getCloudCredential());
        assertEquals(databaseStack, redbeamsStopContext.getDatabaseStack());
    }

    @Test
    public void shouldCreateRequest() {
        RedbeamsEvent request = (RedbeamsEvent) victim.createRequest(null);
        assertEquals(RedbeamsStopEvent.REDBEAMS_STOP_FAILURE_HANDLED_EVENT.event(), request.selector());
    }
}