package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesCMConfigHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.request.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class AddVolumesCMConfigHandler extends EventSenderAwareHandler<AddVolumesCMConfigHandlerEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesCMConfigHandler.class);

    private static final Set<String> BLACKLISTED_ROLES = ImmutableSet.of("DATANODE", "ZEPPELIN_SERVER", "KAFKA_BROKER",
            "SCHEMA_REGISTRY_SERVER", "STREAMS_MESSAGING_MANAGER_SERVER", "SERVER", "NIFI_NODE", "NAMENODE", "STATESTORE",
            "CATALOGSERVER", "KUDU_MASTER", "KUDU_TSERVER", "SOLR_SERVER", "NIFI_REGISTRY_SERVER", "HUE_LOAD_BALANCER", "KNOX_GATEWAY");

    @Inject
    private ConfigUpdateUtilService configUpdateUtilService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    protected AddVolumesCMConfigHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AddVolumesCMConfigHandlerEvent.class);
    }

    @Override
    public void accept(Event<AddVolumesCMConfigHandlerEvent> addVolumesCMConfigHandlerEvent) {
        AddVolumesCMConfigHandlerEvent payload = addVolumesCMConfigHandlerEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getInstanceGroup();
        LOGGER.debug("Starting to update CM config after orchestration on group {}", payload.getInstanceGroup());
        try {
            StackDto stack = stackDtoService.getById(stackId);
            Optional<InstanceGroupView> instanceGroupViewOptional = stack.getInstanceGroupViews().stream().filter(group -> group.getGroupName()
                    .equals(requestGroup)).findFirst();
            InstanceGroupView instanceGroupView = instanceGroupViewOptional.orElseThrow();
            int instanceStorageCount = getInstanceStorageCount(instanceGroupView);
            TemporaryStorage temporaryStorage = getTemporaryStorage(instanceGroupView);
            int attachedVolumesCount = getAttachedVolumesCount(instanceGroupView);
            LOGGER.debug("Updating CM config!");
            String blueprintText = stack.getBlueprint().getBlueprintText();
            CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
            Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
            List<String> hostTemplateRoleGroupNames = processor.getHostTemplateRoleNames(requestGroup);
            Set<String> hostTemplateComponents = processor.getComponentsInHostGroup(requestGroup);
            if (checkConfigChangeRequired(hostTemplateComponents)) {
                configUpdateUtilService.updateCMConfigsForComputeAndStartServices(stack, hostTemplateServiceComponents, instanceStorageCount,
                        attachedVolumesCount, hostTemplateRoleGroupNames, temporaryStorage);
            }
            eventSender().sendEvent(new AddVolumesCMConfigFinishedEvent(stackId, requestGroup, payload.getNumberOfDisks(), payload.getType(),
                    payload.getSize(), payload.getCloudVolumeUsageType()), addVolumesCMConfigHandlerEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("Failed to add disks", e);
            eventSender().sendEvent(new AddVolumesFailedEvent(stackId, null, null, e), addVolumesCMConfigHandlerEvent.getHeaders());
        }
    }

    private int getInstanceStorageCount(InstanceGroupView instanceGroupView) {
        return instanceGroupView.getTemplate().getInstanceStorageCount();
    }

    private TemporaryStorage getTemporaryStorage(InstanceGroupView instanceGroupView) {
        return instanceGroupView.getTemplate().getTemporaryStorage();
    }

    private int getAttachedVolumesCount(InstanceGroupView instanceGroupView) {
        return instanceGroupView.getTemplate().getVolumeTemplates().stream().map(VolumeTemplate::getVolumeCount).reduce(0, Integer::sum);
    }

    private boolean checkConfigChangeRequired(Set<String> hostTemplateComponents) {
        for (String service : hostTemplateComponents) {
            if (BLACKLISTED_ROLES.contains(service)) {
                return false;
            }
        }
        return true;
    }
}
