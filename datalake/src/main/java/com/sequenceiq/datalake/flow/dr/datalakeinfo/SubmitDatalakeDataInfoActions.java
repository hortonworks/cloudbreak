package com.sequenceiq.datalake.flow.dr.datalakeinfo;

import static com.sequenceiq.datalake.flow.dr.datalakeinfo.SubmitDatalakeDataInfoEvent.SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.dr.datalakeinfo.event.SubmitDatalakeDataInfoFailedEvent;
import com.sequenceiq.datalake.flow.dr.datalakeinfo.event.SubmitDatalakeDataInfoRequest;
import com.sequenceiq.datalake.flow.dr.datalakeinfo.event.SubmitDatalakeDataInfoTriggerEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;
import com.sequenceiq.flow.core.Flow;

@Configuration
public class SubmitDatalakeDataInfoActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitDatalakeDataInfoActions.class);

    @Bean(name = "SUBMIT_DATALAKE_DATA_INFO_IN_PROGRESS_STATE")
    public Action<?, ?> submitDatalakeDataInfo() {
        return new AbstractSdxAction<>(SubmitDatalakeDataInfoTriggerEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SubmitDatalakeDataInfoTriggerEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Beginning submission of datalake data info: " + payload.getDataInfoJSON());
                sendEvent(context, SubmitDatalakeDataInfoRequest.from(context, payload.getOperationId(), payload.getDataInfoJSON()));
            }

            @Override
            protected Object getFailurePayload(SubmitDatalakeDataInfoTriggerEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return new SubmitDatalakeDataInfoFailedEvent(payload.getResourceId(), payload.getUserId(), ex);
            }
        };
    }

    @Bean(name = "SUBMIT_DATALAKE_DATA_INFO_FAILED_STATE")
    public Action<?, ?> submitFailed() {
        return new AbstractSdxAction<>(SubmitDatalakeDataInfoFailedEvent.class) {
            @Override
            protected void doExecute(SdxContext context, SubmitDatalakeDataInfoFailedEvent payload, Map<Object, Object> variables) {
                Exception exception = payload.getException();
                LOGGER.error("Submit datalake data info failed.", exception);
                Flow flow = getFlow(context.getFlowParameters().getFlowId());
                flow.setFlowFailed(payload.getException());
                sendEvent(context, SUBMIT_DATALAKE_DATA_INFO_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(SubmitDatalakeDataInfoFailedEvent payload, Optional<SdxContext> flowContext, Exception ex) {
                return null;
            }
        };
    }
}
