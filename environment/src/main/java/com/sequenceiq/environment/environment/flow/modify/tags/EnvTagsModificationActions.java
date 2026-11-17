package com.sequenceiq.environment.environment.flow.modify.tags;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_REDBEAMS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class EnvTagsModificationActions {

    private static final Logger LOGGER = getLogger(EnvTagsModificationActions.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    public EnvTagsModificationActions(EnvironmentStatusUpdateService environmentStatusUpdateService) {
        this.environmentStatusUpdateService = environmentStatusUpdateService;
    }

    @Bean(name = "ENVIRONMENT_TAGS_MODIFICATION_START_STATE")
    public Action<?, ?> initUserDefinedTagsModificationOnEnvironment() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags on environment state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_STARTED,
                        EnvTagsModificationState.ENVIRONMENT_TAGS_MODIFICATION_START_STATE);
                String nextEvent = START_MODIFY_USER_DEFINED_TAGS_FREEIPA_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }

            @Override
            protected Object getFailurePayload(EnvTagsModificationEvent payload, Optional<CommonContext> context,
                    Exception ex) {
                return new EnvTagsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE")
    public Action<?, ?> modifyUserDefinedTagsOnFreeIpa() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags on FreeIPA state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_STARTED,
                        EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FREEIPA_STATE);
                String nextEvent = MODIFY_USER_DEFINED_TAGS_ON_FREEIPA_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }

            @Override
            protected Object getFailurePayload(EnvTagsModificationEvent payload, Optional<CommonContext> context,
                    Exception ex) {
                return new EnvTagsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }

            @Override
            protected EnvironmentStatus getFailureEnvironmentStatus() {
                return EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_FREEIPA_FAILED;
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE")
    public Action<?, ?> modifyUserDefinedTagsOnDatalake() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {
            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags on Data Lake state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_STARTED,
                        EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATALAKE_STATE);
                String nextEvent = MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }

            @Override
            protected Object getFailurePayload(EnvTagsModificationEvent payload, Optional<CommonContext> context,
                    Exception ex) {
                return new EnvTagsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }

            @Override
            protected EnvironmentStatus getFailureEnvironmentStatus() {
                return EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_FAILED;
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE")
    public Action<?, ?> modifyUserDefinedTagsOnDatahubs() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags on Data Hubs state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_STARTED,
                        EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_DATAHUBS_STATE);
                String nextEvent = MODIFY_USER_DEFINED_TAGS_ON_DATAHUBS_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }

            @Override
            protected Object getFailurePayload(EnvTagsModificationEvent payload, Optional<CommonContext> context,
                    Exception ex) {
                return new EnvTagsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }

            @Override
            protected EnvironmentStatus getFailureEnvironmentStatus() {
                return EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATAHUBS_FAILED;
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_REDBEAMS_STATE")
    public Action<?, ?> modifyUserDefinedTagsOnRedbeams() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags on Redbeams state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_REDBEAMS_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_REDBEAMS_STARTED,
                        EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_REDBEAMS_STATE);
                String nextEvent = MODIFY_USER_DEFINED_TAGS_ON_REDBEAMS_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }

            @Override
            protected Object getFailurePayload(EnvTagsModificationEvent payload, Optional<CommonContext> context,
                    Exception ex) {
                return new EnvTagsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }

            @Override
            protected EnvironmentStatus getFailureEnvironmentStatus() {
                return EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_REDBEAMS_FAILED;
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_EXPERIENCES_STATE")
    public Action<?, ?> modifyUserDefinedTagsOnExperiences() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags on Experiences state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload,
                        EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATA_SERVICES_IN_PROGRESS,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_ON_EXPERIENCES_STARTED,
                        EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_EXPERIENCES_STATE);
                String nextEvent = MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }

            @Override
            protected Object getFailurePayload(EnvTagsModificationEvent payload, Optional<CommonContext> context,
                    Exception ex) {
                return new EnvTagsModificationFailureEvent(payload.getResourceId(), payload.getResourceName(), payload.getResourceCrn(),
                        getFailureEnvironmentStatus(), ex);
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE")
    public Action<?, ?> modifyUserDefinedTagsFinished() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationEvent payload, Map<Object, Object> variables) {
                LOGGER.debug("Modify user defined tags finished state started {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_FINISHED,
                        EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FINISHED_STATE);
                String nextEvent = FINALIZE_MODIFY_USER_DEFINED_TAGS_EVENT.event();
                sendEvent(context, nextEvent, EnvTagsModificationSupport.nextEvent(payload, nextEvent));
            }
        };
    }

    @Bean(name = "USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE")
    public Action<?, ?> modifyUserDefinedTagsFailed() {
        return new AbstractEnvTagsModificationAction<>(EnvTagsModificationFailureEvent.class) {

            @Override
            protected void doExecute(CommonContext context, EnvTagsModificationFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Modifying user defined tags failed: {}", payload);
                environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_MODIFICATION_FAILED, List.of(payload.getEnvironmentStatus(),
                                failureDetails(payload.getEnvironmentStatus())), EnvTagsModificationState.USER_DEFINED_TAGS_MODIFICATION_FAILED_STATE);
                sendEvent(context, HANDLED_FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.event(), payload);
            }
        };
    }

    /**
     * Customer-facing failure details. The exception text is internal and only logged; the customer is pointed at the failed step instead.
     */
    private static String failureDetails(EnvironmentStatus failedStatus) {
        return failedStatus.getResponseStatus().getDescription()
                + ". Check the events of the affected resources for details, then retry the operation.";
    }
}
