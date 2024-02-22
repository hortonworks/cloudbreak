package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_EVENT;

import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateTriggerEvent;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;

@Service
public class FreeipaInstanceMetadataUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaInstanceMetadataUpdateService.class);

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    public FlowIdentifier updateInstanceMetadata(String envCrn, InstanceMetadataUpdateType updateType) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(envCrn, ThreadBasedUserCrnProvider.getAccountId());
        Map<InstanceMetadataUpdateType, InstanceMetadataUpdateTypeProperty> types = instanceMetadataUpdateProperties.getTypes();
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(stack.getCloudPlatform());
        instanceMetadataUpdateProperties.validateUpdateType(updateType, cloudPlatform);
        if (StringUtils.equals(types.get(updateType).metadata().get(cloudPlatform).imdsVersion(), stack.getSupportedImdsVersion())) {
            throw new BadRequestException("The given update type is already executed for the stack, no need to do it again.");
        }
        FreeIpaInstanceMetadataUpdateTriggerEvent triggerEvent =
                new FreeIpaInstanceMetadataUpdateTriggerEvent(STACK_IMDUPDATE_EVENT.event(), stack.getId(), updateType);
        return flowManager.notify(STACK_IMDUPDATE_EVENT.event(), triggerEvent);
    }
}
