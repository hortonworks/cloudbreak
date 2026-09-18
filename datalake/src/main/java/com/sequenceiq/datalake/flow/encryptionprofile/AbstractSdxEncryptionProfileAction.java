package com.sequenceiq.datalake.flow.encryptionprofile;

import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.SdxFailedEvent;
import com.sequenceiq.datalake.service.AbstractSdxAction;

abstract class AbstractSdxEncryptionProfileAction<P extends SdxEvent> extends AbstractSdxAction<P> {

    private static final String UNKNOWN_ERROR = "Unknown error";

    private final BiFunction<SdxEvent, Exception, ? extends SdxFailedEvent> failedEventFactory;

    protected AbstractSdxEncryptionProfileAction(Class<P> payloadClass, BiFunction<SdxEvent, Exception, ? extends SdxFailedEvent> failedEventFactory) {
        super(payloadClass);
        this.failedEventFactory = failedEventFactory;
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<SdxContext> flowContext, Exception ex) {
        return failedEventFactory.apply(payload, ex);
    }

    protected String getFailureReason(Exception exception) {
        return Optional.ofNullable(exception)
                .map(Exception::getMessage)
                .filter(StringUtils::isNotBlank)
                .orElse(UNKNOWN_ERROR);
    }
}
