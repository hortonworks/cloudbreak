package com.sequenceiq.redbeams.flow.redbeams.provision.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.AbstractRedbeamsProvisionAction;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionState;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.RedbeamsEventToTriggerRedbeamsProvisionEventPayloadConverter;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.TriggerRedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.UpdateDatabaseServerRegistrationSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import com.sequenceiq.redbeams.sync.DBStackJobService;

@Configuration
public class RedbeamsProvisionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsProvisionActions.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private DBResourceService dbResourceService;

    @Inject
    private RedbeamsMetricService metricService;

    @Inject
    private DBStackJobService dbStackJobService;

    @Bean(name = "ALLOCATE_DATABASE_SERVER_STATE")
    public Action<?, ?> allocateDatabaseServer() {
        return new AbstractRedbeamsProvisionAction<>(TriggerRedbeamsProvisionEvent.class) {

            @Override
            protected void prepareExecution(TriggerRedbeamsProvisionEvent payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.CREATING_INFRASTRUCTURE);
            }

            @Override
            protected void doExecute(RedbeamsContext context, TriggerRedbeamsProvisionEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new AllocateDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(), context.getDatabaseStack(),
                        payload.getNetworkParameters()));
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<TriggerRedbeamsProvisionEvent>> payloadConverters) {
                payloadConverters.add(new RedbeamsEventToTriggerRedbeamsProvisionEventPayloadConverter());
            }
        };
    }

    @Bean(name = "UPDATE_DATABASE_SERVER_REGISTRATION_STATE")
    public Action<?, ?> updateDatabaseServerRegistration() {
        return new AbstractRedbeamsProvisionAction<>(AllocateDatabaseServerSuccess.class) {

            @Override
            protected void doExecute(RedbeamsContext context, AllocateDatabaseServerSuccess payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.PROVISIONED);
                List<CloudResource> dbResourcesList = dbResourceService.getAllAsCloudResource(payload.getResourceId());
                sendEvent(context,
                        new UpdateDatabaseServerRegistrationRequest(
                                context.getCloudContext(),
                                context.getCloudCredential(),
                                context.getDatabaseStack(),
                                dbResourcesList
                        )
                );
            }
        };
    }

    @Bean(name = "REDBEAMS_PROVISION_FINISHED_STATE")
    public Action<?, ?> provisionFinished() {
        return new AbstractRedbeamsProvisionAction<>(UpdateDatabaseServerRegistrationSuccess.class) {

            @Override
            protected void prepareExecution(UpdateDatabaseServerRegistrationSuccess payload, Map<Object, Object> variables) {
                Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.AVAILABLE);
                metricService.incrementMetricCounter(MetricType.DB_PROVISION_FINISHED, dbStack);

                dbStack.ifPresentOrElse(
                        db -> dbStackJobService.schedule(db.getId()),
                        () -> LOGGER.info("DBStack was not present, could not start autosync service")
                );
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_FINISHED_EVENT.name(), context.getDBStack().getId());
            }
        };
    }

    @Bean(name = "REDBEAMS_PROVISION_FAILED_STATE")
    public Action<?, ?> provisionFailed() {
        return new AbstractRedbeamsFailureAction<RedbeamsProvisionState, RedbeamsProvisionEvent>() {

            @Override
            protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {

                Exception failureException = payload.getException();
                LOGGER.info("Error during database stack creation flow:", failureException);

                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    // StackCreationActions / StackCreationService only update status if stack isn't mid-deletion
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.PROVISION_FAILED, errorReason);
                    metricService.incrementMetricCounter(MetricType.DB_PROVISION_FAILED, dbStack);
                }

            }

            @Override
            protected void doExecute(CommonContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new RedbeamsEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
            }

        };
    }
}