package com.sequenceiq.datalake.flow.java;

import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.java.SetDatalakeDefaultJavaVersionFlowEvent.SET_DATALAKE_DEFAULT_JAVA_VERSION_FINALIZED_EVENT;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.java.handler.WaitSetDatalakeDefaultJavaVersionRequest;
import com.sequenceiq.datalake.flow.java.handler.WaitSetDatalakeDefaultJavaVersionResult;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.datalake.service.sdx.CloudbreakStackService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class SetDatalakeDefaultJavaVersionActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDatalakeDefaultJavaVersionActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Inject
    private CloudbreakStackService cloudbreakStackService;

    @Bean(name = "SET_DATALAKE_DEFAULT_JAVA_VERSION_STATE")
    public Action<?, ?> setDefaultJavaVersion() {

        return new AbstractSdxAction<>(SetDatalakeDefaultJavaVersionTriggerEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SetDatalakeDefaultJavaVersionTriggerEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Initiating the process to set the default Java version to {} for the Data Lake. Restart services: {}",
                        payload.getDefaultJavaVersion(), payload.isRestartServices());
                WaitSetDatalakeDefaultJavaVersionRequest setDefaultJavaVersionRequest = new WaitSetDatalakeDefaultJavaVersionRequest(payload.getResourceId(),
                        context.getUserId(), payload.getDefaultJavaVersion(), payload.isRestartServices());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_DEFAULT_JAVA_VERSION_CHANGE_IN_PROGRESS,
                        List.of(payload.getDefaultJavaVersion()), "Initiating the process to set the default Java version for the Data Lake",
                        context.getSdxId());
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                cloudbreakStackService.setDefaultJavaVersion(sdxCluster, payload.getDefaultJavaVersion(), payload.isRestartServices(), payload.isRestartCM());
                LOGGER.info("Successfully initiated the process to set the default Java version for the Data Lake with payload: {}", payload);
                sendEvent(context, setDefaultJavaVersionRequest);
            }

            @Override
            protected Object getFailurePayload(SetDatalakeDefaultJavaVersionTriggerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SetDatalakeDefaultJavaVersionFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SET_DATALAKE_DEFAULT_JAVA_VERSION_FINISED_STATE")
    public Action<?, ?> setDefaultJavaVersionFinished() {

        return new AbstractSdxAction<>(WaitSetDatalakeDefaultJavaVersionResult.class) {
            @Override
            protected void doExecute(SdxContext context, WaitSetDatalakeDefaultJavaVersionResult payload, Map<Object, Object> variables) throws Exception {
                LOGGER.info("Successfully set the default Java version for the Data Lake");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.RUNNING,
                        "Successfully set the default Java version for the Data Lake", context.getSdxId());
                sendEvent(context, new SdxEvent(SET_DATALAKE_DEFAULT_JAVA_VERSION_FINALIZED_EVENT.event(), context.getSdxId(), context.getUserId()));
            }

            @Override
            protected Object getFailurePayload(WaitSetDatalakeDefaultJavaVersionResult payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SetDatalakeDefaultJavaVersionFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SET_DATALAKE_DEFAULT_JAVA_VERSION_FAILED_STATE")
    public Action<?, ?> setDefaultJavaVersionFailed() {
        return new AbstractSdxAction<>(SetDatalakeDefaultJavaVersionFailedEvent.class) {

            @Override
            protected void doExecute(SdxContext context, SetDatalakeDefaultJavaVersionFailedEvent payload, Map<Object, Object> variables) throws Exception {
                LOGGER.warn("Failed to set the default Java version for datalake");
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_DEFAULT_JAVA_VERSION_CHANGE_FAILED,
                        Collections.singleton(payload.getException().getMessage()), "Failed to set the default Java version for datalake",
                        context.getSdxId());
                sendEvent(context);
            }

            @Override
            protected Object getFailurePayload(SetDatalakeDefaultJavaVersionFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected Selectable createRequest(SdxContext context) {
                return new SdxEvent(SET_DATALAKE_DEFAULT_JAVA_VERSION_FAIL_HANDLED_EVENT.event(), context.getSdxId(), context.getUserId());
            }
        };
    }

}
