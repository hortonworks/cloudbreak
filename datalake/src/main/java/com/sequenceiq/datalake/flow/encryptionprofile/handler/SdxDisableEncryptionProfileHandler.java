package com.sequenceiq.datalake.flow.encryptionprofile.handler;

import static com.sequenceiq.datalake.flow.encryptionprofile.SdxDisableEncryptionProfileEvent.SDX_DISABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxDisableEncryptionProfileHandlerEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Component
public class SdxDisableEncryptionProfileHandler extends AbstractSdxEncryptionProfileHandler<SdxDisableEncryptionProfileHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxDisableEncryptionProfileHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxDisableEncryptionProfileHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxDisableEncryptionProfileHandlerEvent> event) {
        LOGGER.warn("Exception during Disable Encryption Profile in Data Lake: ", e);
        return new SdxDisableEncryptionProfileFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected String operationName() {
        return "Disable encryption profile";
    }

    @Override
    protected FlowIdentifier triggerOnStack(StackV4Endpoint stackV4Endpoint, SdxCluster sdxCluster, SdxDisableEncryptionProfileHandlerEvent event) {
        return stackV4Endpoint.disableEncryptionProfileByCrn(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn());
    }

    @Override
    protected String successEventSelector() {
        return SDX_DISABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT.event();
    }
}
