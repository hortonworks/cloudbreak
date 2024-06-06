package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

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
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets.RemoveUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets.RemoveUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecorator;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RemoveUserdataSecretsHandler extends ExceptionCatcherEventHandler<RemoveUserdataSecretsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveUserdataSecretsHandler.class);

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
        return EventSelectorUtil.selector(RemoveUserdataSecretsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RemoveUserdataSecretsRequest> event) {
        LOGGER.error("Unexpected error occurred while removing userdata secrets!", e);
        return new DownscaleFailureEvent(resourceId, "Removing userdata secrets", Set.of(), Map.of(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<RemoveUserdataSecretsRequest> event) {
        RemoveUserdataSecretsRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        List<String> downscaleHosts = request.getDownscaleHosts();

        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.info("Deleting userdata secrets for stack [{}]...", stack.getName());
        CloudInformationDecorator cloudInformationDecorator = cloudInformationDecoratorProvider.getForStack(stack);
        List<InstanceMetaData> instancesWithUndeletedSecretResources = new ArrayList<>();
        List<Long> secretResourceIds = new ArrayList<>();
        List<String> authorizedClientsToRemove = new ArrayList<>();
        stack.getInstanceGroups().stream()
                .flatMap(ig -> ig.getInstanceMetaData().stream())
                .filter(imd -> imd.getUserdataSecretResourceId() != null && downscaleHosts.contains(imd.getDiscoveryFQDN()))
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
        return new RemoveUserdataSecretsSuccess(stackId);
    }
}
