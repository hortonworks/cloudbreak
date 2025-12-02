package com.sequenceiq.freeipa.flow.freeipa.upscale.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleUpdateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleUpdateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecorator;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class UpscaleUpdateUserdataSecretsHandler extends ExceptionCatcherEventHandler<UpscaleUpdateUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpscaleUpdateUserdataSecretsHandler.class);

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Inject
    private StackService stackService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Inject
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpscaleUpdateUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpscaleUpdateUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while updating userdata secrets!", e);
        return new UpscaleFailureEvent(resourceId, "Updating userdata secrets", Set.of(), ERROR, Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpscaleUpdateUserdataSecretsRequest> event) {
        UpscaleUpdateUserdataSecretsRequest request = event.getData();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        Long stackId = request.getResourceId();
        List<Long> newInstanceIds = request.getNewInstanceIds();

        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn());
        List<InstanceMetaData> newInstanceMetadas = new ArrayList<>();
        List<String> newAuthorizedClients = new ArrayList<>();
        stack.getInstanceGroups().stream()
                .flatMap(ig -> ig.getNotDeletedInstanceMetaDataSet().stream())
                .filter(imd -> imd.getUserdataSecretResourceId() != null && newInstanceIds.contains(imd.getId()))
                .forEach(imd -> {
                    newInstanceMetadas.add(imd);
                    newAuthorizedClients.add(cloudInformationDecorator.getAuthorizedClientForLuksEncryptionKey(stack, imd));
                });
        encryptionKeyService.updateLuksEncryptionKeyAccess(stack, cloudContext, cloudCredential, newAuthorizedClients, List.of());
        LOGGER.info("Updating userdata secrets of instances [{}]...", newInstanceMetadas);
        userdataSecretsService.updateUserdataSecrets(stack, newInstanceMetadas, environment.getCredential(), request.getCloudContext(),
                request.getCloudCredential());
        return new UpscaleUpdateUserdataSecretsSuccess(stackId);
    }
}
