package com.sequenceiq.datalake.flow.create;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FAILED_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_CREATE_FINALIZED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_STACK_CREATION_IN_PROGRESS_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.quartz.statuschecker.service.StatusCheckerJobService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.RdsWaitRequest;
import com.sequenceiq.datalake.flow.create.event.RdsWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.StackCreationWaitRequest;
import com.sequenceiq.datalake.flow.create.event.StorageValidationRequest;
import com.sequenceiq.datalake.flow.create.event.StorageValidationSuccessEvent;
import com.sequenceiq.datalake.job.SdxClusterJobAdapter;
import com.sequenceiq.datalake.metric.MetricType;
import com.sequenceiq.datalake.metric.SdxMetricService;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.ProvisionerService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.DatalakeStatusUpdateException;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowState;

@Configuration
public class SdxCreateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxCreateActions.class);

    @Inject
    private ProvisionerService provisionerService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private StatusCheckerJobService jobService;

    @Inject
    private SdxMetricService metricService;

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Inject
    private SdxService sdxService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private EventSenderService eventSenderService;

    @Bean(name = "SDX_CREATION_STORAGE_VALIDATION_STATE")
    public Action<?, ?> storageValidation() {
        return new AbstractSdxAction<>(SdxEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                // When SDX is created as part of re-size flow chain, SDX in payload will not have the correct ID.
                if (!Strings.isNullOrEmpty(payload.getSdxName())) {
                    SdxCluster sdxCluster = sdxService.getByNameInAccount(context.getUserId(), payload.getSdxName());
                    context.setSdxId(sdxCluster.getId());
                }
                eventSenderService.notifyEvent(context, ResourceEvent.SDX_CLUSTER_PROVISION_STARTED);
                StorageValidationRequest req = new StorageValidationRequest(context);
                sendEvent(context, req.selector(), req);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_WAIT_RDS_STATE")
    public Action<?, ?> rdsCreation() {
        return new AbstractSdxAction<>(StorageValidationSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StorageValidationSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StorageValidationSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                RdsWaitRequest req = new RdsWaitRequest(context);
                sendEvent(context, req.selector(), req);
            }

            @Override
            protected Object getFailurePayload(StorageValidationSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_WAIT_ENV_STATE")
    public Action<?, ?> envWaitInProgress() {
        return new AbstractSdxAction<>(RdsWaitSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, RdsWaitSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, RdsWaitSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                EnvWaitRequest req = EnvWaitRequest.from(context);
                sendEvent(context, req.selector(), req);
            }

            @Override
            protected Object getFailurePayload(RdsWaitSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_START_STATE")
    public Action<?, ?> sdxCreation() {
        return new AbstractSdxAction<>(EnvWaitSuccessEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    EnvWaitSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, EnvWaitSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                provisionerService.startStackProvisioning(payload.getResourceId(), payload.getDetailedEnvironmentResponse());
                sendEvent(context, SDX_STACK_CREATION_IN_PROGRESS_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(EnvWaitSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_STACK_CREATION_IN_PROGRESS_STATE")
    public Action<?, ?> sdxStackCreationInProgress() {
        return new AbstractSdxAction<>(SdxEvent.class) {

            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext, SdxEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) throws Exception {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return StackCreationWaitRequest.from(context);
            }

            @Override
            protected Object getFailurePayload(SdxEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_CREATION_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxAction<>(StackCreationSuccessEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    StackCreationSuccessEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, StackCreationSuccessEvent payload, Map<Object, Object> variables) throws Exception {
                SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Datalake is running", payload.getResourceId());
                metricService.incrementMetricCounter(MetricType.SDX_CREATION_FINISHED, sdxCluster);
                jobService.schedule(context.getSdxId(), SdxClusterJobAdapter.class);
                eventSenderService.notifyEvent(context, ResourceEvent.SDX_CLUSTER_PROVISION_FINISHED);
                sendEvent(context, SDX_CREATE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(StackCreationSuccessEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }

    @Bean(name = "SDX_CREATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxAction<>(SdxCreateFailedEvent.class) {
            @Override
            protected SdxContext createFlowContext(FlowParameters flowParameters, StateContext<FlowState, FlowEvent> stateContext,
                    SdxCreateFailedEvent payload) {
                return SdxContext.from(flowParameters, payload);
            }

            @Override
            protected void doExecute(SdxContext context, SdxCreateFailedEvent payload, Map<Object, Object> variables) throws Exception {
                Exception exception = payload.getException();
                String statusReason = "Datalake creation failed";
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(exception);
                if (StringUtils.hasText(errorMessage)) {
                    statusReason = statusReason + ". " + errorMessage;
                } else if (exception.getMessage() != null) {
                    statusReason = statusReason + ". " + exception.getMessage();
                }
                LOGGER.error(statusReason, exception);
                try {
                    SdxCluster sdxCluster = sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.PROVISIONING_FAILED,
                            statusReason, payload.getResourceId());
                    metricService.incrementMetricCounter(MetricType.SDX_CREATION_FAILED, sdxCluster);
                    eventSenderService.notifyEvent(context, ResourceEvent.SDX_CLUSTER_CREATION_FAILED);
                } catch (NotFoundException notFoundException) {
                    LOGGER.info("Can not set status to SDX_CREATION_FAILED because data lake was not found");
                } catch (DatalakeStatusUpdateException datalakeStatusUpdateException) {
                    LOGGER.info("Status update for data lake failed (possible reason: ongoing parallel deletion flow): ", exception.getMessage());
                }
                Flow flow = getFlow(context.getFlowParameters().getFlowId());
                flow.setFlowFailed(payload.getException());
                sendEvent(context, SDX_CREATE_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SdxCreateFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
