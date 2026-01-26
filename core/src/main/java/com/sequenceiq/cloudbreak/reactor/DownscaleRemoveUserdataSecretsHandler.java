package com.sequenceiq.cloudbreak.reactor;

import java.util.ArrayList;
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
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecorator;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DownscaleRemoveUserdataSecretsHandler extends ExceptionCatcherEventHandler<DownscaleRemoveUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownscaleRemoveUserdataSecretsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Inject
    private ResourceService resourceService;

    @Inject
    private EncryptionKeyService encryptionKeyService;

    @Inject
    private UserdataSecretsService userdataSecretsService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DownscaleRemoveUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DownscaleRemoveUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while removing userdata secrets!", e);
        return new DownscaleRemoveUserdataSecretsFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DownscaleRemoveUserdataSecretsRequest> event) {
        DownscaleRemoveUserdataSecretsRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        List<Long> instancePrivateIds = request.getInstancePrivateIds();

        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Deleting userdata secrets for stack [{}]...", stack.getName());
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        List<InstanceMetaData> instancesWithUndeletedSecretResources = new ArrayList<>();
        List<Long> secretResourceIds = new ArrayList<>();
        List<String> authorizedClientsToRemove = new ArrayList<>();
        stack.getTerminatedAndNonTerminatedInstanceMetaDataAsList().stream()
                .filter(imd -> imd.getUserdataSecretResourceId() != null && instancePrivateIds.contains(imd.getPrivateId()))
                .forEach(imd -> {
                    instancesWithUndeletedSecretResources.add(imd);
                    secretResourceIds.add(imd.getUserdataSecretResourceId());
                    authorizedClientsToRemove.add(cloudInformationDecorator.getAuthorizedClientForLuksEncryptionKey(stack, imd));
                });
        List<String> secretResourceReferences = new ArrayList<>();
        Iterable<Resource> resourceIterable = resourceService.findAllByResourceId(secretResourceIds);
        resourceIterable.forEach(r -> secretResourceReferences.add(r.getResourceReference()));

        encryptionKeyService.updateCloudSecretManagerEncryptionKeyAccess(stack, cloudContext, cloudCredential, List.of(), secretResourceReferences);
        encryptionKeyService.updateLuksEncryptionKeyAccess(stack, cloudContext, cloudCredential, List.of(), authorizedClientsToRemove);
        userdataSecretsService.deleteUserdataSecretsForInstances(instancesWithUndeletedSecretResources, cloudContext, cloudCredential);
        return new DownscaleRemoveUserdataSecretsSuccess(stackId);
    }
}
