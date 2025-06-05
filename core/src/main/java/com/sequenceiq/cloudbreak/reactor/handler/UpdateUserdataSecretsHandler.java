package com.sequenceiq.cloudbreak.reactor.handler;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecorator;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateUserdataSecretsHandler extends ExceptionCatcherEventHandler<UpdateUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateUserdataSecretsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Inject
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Inject
    private EncryptionKeyService encryptionKeyService;

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
        UpdateUserdataSecretsRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();

        String environmentCrn = stackService.findEnvironmentCrnByStackId(stackId);
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);
        if (environment.isEnableSecretEncryption()) {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
            List<InstanceMetaData> instanceMetaDatas = new ArrayList<>();
            List<String> newAuthorizedClients = new ArrayList<>();
            stack.getNotDeletedAndNotZombieInstanceMetaDataList().stream()
                    .filter(imd -> imd.getUserdataSecretResourceId() != null)
                    .forEach(imd -> {
                        instanceMetaDatas.add(imd);
                        newAuthorizedClients.add(cloudInformationDecorator.getAuthorizedClientForLuksEncryptionKey(stack, imd));
                    });
            encryptionKeyService.updateLuksEncryptionKeyAccess(stack, cloudContext, cloudCredential, newAuthorizedClients, List.of());
            LOGGER.info("Updating userdata secrets of stack [{}]...", stack.getName());
            userdataSecretsService.updateUserdataSecrets(stack, instanceMetaDatas, environment, cloudContext, cloudCredential);
        } else {
            LOGGER.debug("Skipping updating userdata secrets because secret encryption is disabled.");
        }
        return new UpdateUserdataSecretsSuccess(stackId);
    }
}
