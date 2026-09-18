package com.sequenceiq.datalake.flow.encryptionprofile.handler;

import static com.sequenceiq.datalake.flow.encryptionprofile.SdxEnableEncryptionProfileEvent.SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileFailedEvent;
import com.sequenceiq.datalake.flow.encryptionprofile.event.SdxEnableEncryptionProfileHandlerEvent;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.event.EventSelectorUtil;

@Component
public class SdxEnableEncryptionProfileHandler extends AbstractSdxEncryptionProfileHandler<SdxEnableEncryptionProfileHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxEnableEncryptionProfileHandler.class);

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SdxEnableEncryptionProfileHandlerEvent.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SdxEnableEncryptionProfileHandlerEvent> event) {
        LOGGER.warn("Exception during Enable Encryption Profile in Data Lake: ", e);
        return new SdxEnableEncryptionProfileFailedEvent(resourceId, event.getData().getUserId(), e);
    }

    @Override
    protected String operationName() {
        return "Enable encryption profile";
    }

    @Override
    protected FlowIdentifier triggerOnStack(StackV4Endpoint stackV4Endpoint, SdxCluster sdxCluster, SdxEnableEncryptionProfileHandlerEvent event) {
        LOGGER.info("Enable encryption profile with encryptionProfileCrn: {}", event.getEncryptionProfileCrn());
        return stackV4Endpoint.enableEncryptionProfileByCrn(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn(), event.getEncryptionProfileCrn());
    }

    @Override
    protected String successEventSelector() {
        return SDX_ENABLE_ENCRYPTION_PROFILE_SUCCESS_EVENT.event();
    }
}
