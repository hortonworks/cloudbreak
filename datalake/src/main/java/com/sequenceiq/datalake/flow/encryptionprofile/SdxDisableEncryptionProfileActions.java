package com.sequenceiq.datalake.flow.encryptionprofile;

import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISABLE_ENCRYPTION_PROFILE_FAILED;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.DATALAKE_DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS;
import static com.sequenceiq.datalake.entity.DatalakeStatusEnum.RUNNING;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileHandlerEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileTriggerEvent;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;

@Configuration
public class SdxDisableEncryptionProfileActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDisableEncryptionProfileActions.class);

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Bean(name = "SDX_DISABLE_ENCRYPTION_PROFILE_STATE")
    public Action<?, ?> disableEncryptionProfileAction() {
        return new AbstractSdxDisableEncryptionProfileAction<>(SdxDisableEncryptionProfileTriggerEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxDisableEncryptionProfileTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Starting Disable Encryption Profile flow for Data Lake: {}", payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_DISABLE_ENCRYPTION_PROFILE_IN_PROGRESS,
                        "Disable Encryption Profile is in progress", payload.getResourceId());
                sendEvent(context, SdxDisableEncryptionProfileHandlerEvent.from(context));
            }
        };
    }

    @Bean(name = "SDX_DISABLE_ENCRYPTION_PROFILE_FINISHED_STATE")
    public Action<?, ?> finishedAction() {
        return new AbstractSdxDisableEncryptionProfileAction<>(SdxEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Disable Encryption Profile finished for Data Lake: {}", payload.getResourceId());
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(RUNNING, ResourceEvent.DATALAKE_DISABLE_ENCRYPTION_PROFILE_FINISHED,
                        "Disable Encryption Profile completed successfully", sdxCluster);
                sendEvent(context, SDX_DISABLE_ENCRYPTION_PROFILE_FINALIZED_EVENT.event(), payload);
            }
        };
    }

    @Bean(name = "SDX_DISABLE_ENCRYPTION_PROFILE_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractSdxDisableEncryptionProfileAction<>(SdxDisableEncryptionProfileFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SdxDisableEncryptionProfileFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Disable Encryption Profile failed for Data Lake: {}", payload.getResourceId(), exception);
                String reason = getFailureReason(exception);
                SdxCluster sdxCluster = sdxService.getById(payload.getResourceId());
                sdxStatusService.setStatusForDatalakeAndNotify(DATALAKE_DISABLE_ENCRYPTION_PROFILE_FAILED,
                        List.of(sdxCluster.getName(), reason), "Disable Encryption Profile failed: " + reason, sdxCluster);
                sdxStatusService.setStatusForDatalake(RUNNING, "Disable Encryption Profile failed: " + reason, sdxCluster);
                sendEvent(context, SDX_DISABLE_ENCRYPTION_PROFILE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }
}
