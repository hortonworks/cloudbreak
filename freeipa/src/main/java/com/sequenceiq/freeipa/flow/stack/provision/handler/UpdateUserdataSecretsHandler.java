package com.sequenceiq.freeipa.flow.stack.provision.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class UpdateUserdataSecretsHandler extends ExceptionCatcherEventHandler<UpdateUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateUserdataSecretsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while updating userdata secrets", e);
        return new UpdateUserdataSecretsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateUserdataSecretsRequest> event) {
        Long stackId = event.getData().getResourceId();
        CloudContext cloudContext = event.getData().getCloudContext();
        CloudCredential cloudCredential = event.getData().getCloudCredential();
        String environmentCrn = stackService.getEnvironmentCrnByStackId(stackId);
        DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(environmentCrn);
        if (environment.isEnableSecretEncryption()) {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            List<InstanceMetaData> instanceMetaDatas = stack.getInstanceGroups().stream()
                    .flatMap(ig -> ig.getNotDeletedInstanceMetaDataSet().stream())
                    .filter(imd -> imd.getUserdataSecretResourceId() != null)
                    .toList();
            userdataSecretsService.updateUserdataSecrets(stack, instanceMetaDatas, environment.getCredential(), cloudContext, cloudCredential);
        } else {
            LOGGER.debug("Skipping updating userdata secrets because secret encryption is disabled.");
        }
        return new UpdateUserdataSecretsSuccess(stackId);
    }
}
