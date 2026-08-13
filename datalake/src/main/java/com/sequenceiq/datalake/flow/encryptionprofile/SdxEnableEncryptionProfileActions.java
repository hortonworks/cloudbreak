package com.sequenceiq.datalake.flow.encryptionprofile;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_ENCRYPTION_PROFILE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileHandlerEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileTriggerEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class SdxEnableEncryptionProfileActions {

    static final String ENCRYPTION_PROFILE_CRN_VARIABLE = "ENCRYPTION_PROFILE_CRN";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEnableEncryptionProfileActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Bean(name = "SDX_ENABLE_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> enableEncryptionProfileAction() {
        return new AbstractSdxEnableEncryptionProfileAction<>(SdxEnableEncryptionProfileTriggerEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEnableEncryptionProfileTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting Enable Encryption Profile flow for SDX: {}", payload.getResourceId());
                if (StringUtils.isNotEmpty(payload.getEncryptionProfileCrn())) {
                    variables.put(ENCRYPTION_PROFILE_CRN_VARIABLE, payload.getEncryptionProfileCrn());
                }
                sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_ENABLE_ENCRYPTION_PROFILE_IN_PROGRESS,
                        "Enable Encryption Profile is in progress", payload.getResourceId());
                sendEvent(context, SdxEnableEncryptionProfileHandlerEvent.from(context, payload.getEncryptionProfileCrn()));
            }

            @Override
            protected Object getFailurePayload(SdxEnableEncryptionProfileTriggerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return SdxEnableEncryptionProfileFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "SDX_ENABLE_ENCRYPTION_PROFILE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxEnableEncryptionProfileAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Enable Encryption Profile finished for SDX: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(RUNNING, ResourceEvent.DATALAKE_ENABLE_ENCRYPTION_PROFILE_FINISHED,
                        "Enable Encryption Profile completed successfully", sdxCluster);
                sendEvent(context, SDX_ENABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "SDX_ENABLE_ENCRYPTION_PROFILE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxEnableEncryptionProfileAction<>(SdxEnableEncryptionProfileFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEnableEncryptionProfileFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Enable Encryption Profile failed for SDX: {}", payload.getResourceId(), exception);
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_ENABLE_ENCRYPTION_PROFILE_FAILED,
                        List.of(sdxCluster.getName()), "Enable Encryption Profile failed", sdxCluster);
                sdxStatusService.setStatusForDatalake(RUNNING, "Enable Encryption Profile failed, datalake returned to RUNNING", sdxCluster);
                sendEvent(context, SDX_ENABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
