package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataSuccess;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class CreateUserDataHandler implements EventHandler<CreateUserDataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserDataHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private UserDataService userDataService;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateUserDataRequest.class);
    }

    @Override
    public void accept(Event<CreateUserDataRequest> event) {
        CreateUserDataRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        Selectable response;
        try {
            idBrokerService.generateIdBrokerSignKey(stackId);
            userDataService.createUserData(stackId);

            String environmentCrn = stackService.findEnvironmentCrnByStackId(stackId);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(environmentCrn);
            if (environment.isEnableSecretEncryption()) {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                List<InstanceMetaData> instanceMetaDatas = stack.getInstanceMetaDataAsList().stream()
                        .filter(imd -> imd.getUserdataSecretResourceId() == null)
                        .toList();
                List<Resource> secretResources = userdataSecretsService.createUserdataSecrets(stack,
                        instanceMetaDatas.stream().map(InstanceMetaData::getPrivateId).toList(), cloudContext, cloudCredential);
                userdataSecretsService.assignSecretsToInstances(stack, secretResources, instanceMetaDatas);
                encryptionKeyService.updateCloudSecretManagerEncryptionKeyAccess(stack, cloudContext, cloudCredential,
                        secretResources.stream().map(Resource::getResourceReference).toList(), List.of());
            } else {
                LOGGER.debug("Skipping creating userdata secrets because secret encryption is disabled.");
            }
            response = new CreateUserDataSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Creating user data has failed", e);
            response = new CreateUserDataFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
