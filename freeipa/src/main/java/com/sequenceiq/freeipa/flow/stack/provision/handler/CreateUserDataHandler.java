package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class CreateUserDataHandler extends ExceptionCatcherEventHandler<CreateUserDataRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateUserDataHandler.class);

    @Inject
    private UserDataService userDataService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CreateUserDataRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<CreateUserDataRequest> event) {
        LOGGER.error("Creating user data has failed with unexpected error", e);
        return new CreateUserDataFailed(resourceId, e, ERROR);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<CreateUserDataRequest> event) {
        Long stackId = event.getData().getResourceId();
        CloudContext cloudContext = event.getData().getCloudContext();
        CloudCredential cloudCredential = event.getData().getCloudCredential();
        try {
            securityConfigService.initSaltSecurityConfigs(stackId);
            userDataService.createUserData(stackId);

            String environmentCrn = stackService.getEnvironmentCrnByStackId(stackId);
            DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(environmentCrn);
            if (environment.isEnableSecretEncryption()) {
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                List<InstanceMetaData> instanceMetaDatas = stack.getInstanceGroups().stream()
                        .flatMap(ig -> ig.getNotDeletedInstanceMetaDataSet().stream())
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
            return new CreateUserDataSuccess(stackId);
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Creating user data has failed", e);
            return new CreateUserDataFailed(stackId, e, ERROR);
        }
    }
}
