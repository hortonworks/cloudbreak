package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesOrchestrationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesOrchestrationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DeleteVolumesOrchestrationHandler extends ExceptionCatcherEventHandler<DeleteVolumesOrchestrationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesOrchestrationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private DeleteVolumesService deleteVolumesService;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteVolumesOrchestrationEvent> event) {
        return new DeleteVolumesFailedEvent(e.getMessage(), e, resourceId);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesOrchestrationEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DeleteVolumesOrchestrationEvent> deleteVolumesOrchestrationEvent) {
        LOGGER.debug("Staring DeleteVolumesOrchestrationHandler with event: {}", deleteVolumesOrchestrationEvent);
        DeleteVolumesOrchestrationEvent payload = deleteVolumesOrchestrationEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getRequestGroup();
        LOGGER.debug("Starting orchestration after deleting volumes to group {}", requestGroup);
        try {
            Stack stack = stackService.getByIdWithLists(stackId);
            LOGGER.debug("Mounting volumes after deleting block storage from the instance!");
            Map<String, Map<String, String>> fstabInformation = deleteVolumesService.redeployStatesAndMountDisks(stack, requestGroup);
            parseFstabAndPersistDiskInformation(fstabInformation, stack);
            return new DeleteVolumesOrchestrationFinishedEvent(stackId, requestGroup);
        } catch (Exception ex) {
            LOGGER.error("Remounting disks after deleting block storage failed for stack: {}, and group: {}, Exception:: {}", stackId, requestGroup,
                    ex.getCause());
            return new DeleteVolumesFailedEvent(ex.getMessage(), ex, stackId);
        }
    }

    private void parseFstabAndPersistDiskInformation(Map<String, Map<String, String>> fstabInformation, Stack stack) {
        LOGGER.debug("Parsing fstab information from host orchestrator mounting additional volumes - {}", fstabInformation);
        fstabInformation.forEach((hostname, value) -> {
            Optional<String> instanceIdOptional = stack.getInstanceMetaDataAsList().stream()
                    .filter(instanceMetaData -> hostname.equals(instanceMetaData.getDiscoveryFQDN()))
                    .map(InstanceMetaData::getInstanceId)
                    .findFirst();

            if (instanceIdOptional.isPresent()) {
                String uuids = value.getOrDefault("uuids", "");
                String fstab = value.getOrDefault("fstab", "");
                if (!StringUtils.isEmpty(uuids) && !StringUtils.isEmpty(fstab)) {
                    LOGGER.debug("Persisting resources for instance id - {}, hostname - {}, uuids - {}, fstab - {}.", instanceIdOptional.get(), hostname,
                            uuids, fstab);
                    persistUuidAndFstab(stack, instanceIdOptional.get(), hostname, uuids, fstab);
                }
            }
        });
    }

    private void persistUuidAndFstab(Stack stack, String instanceId, String discoveryFQDN, String uuids, String fstab) {
        resourceService.saveAll(stack.getDiskResources().stream()
                .filter(volumeSet -> instanceId.equals(volumeSet.getInstanceId()))
                .peek(volumeSet -> resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class).ifPresent(volumeSetAttributes -> {
                    volumeSetAttributes.setUuids(uuids);
                    volumeSetAttributes.setFstab(fstab);
                    if (!discoveryFQDN.equals(volumeSetAttributes.getDiscoveryFQDN())) {
                        LOGGER.info("DiscoveryFQDN is updated for {} to {}", volumeSet.getResourceName(), discoveryFQDN);
                    }
                    volumeSetAttributes.setDiscoveryFQDN(discoveryFQDN);
                    resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
                }))
                .collect(Collectors.toList()));
    }
}
