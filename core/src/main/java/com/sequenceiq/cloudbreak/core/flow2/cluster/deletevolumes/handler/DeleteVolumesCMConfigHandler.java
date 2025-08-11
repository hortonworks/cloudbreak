package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesCMConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesCMConfigFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesFailedEvent;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DeleteVolumesCMConfigHandler extends ExceptionCatcherEventHandler<DeleteVolumesCMConfigEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteVolumesCMConfigHandler.class);

    private static final Set<String> BLACKLISTED_ROLES = ImmutableSet.of("DATANODE", "ZEPPELIN_SERVER", "KAFKA_BROKER",
            "SCHEMA_REGISTRY_SERVER", "STREAMS_MESSAGING_MANAGER_SERVER", "SERVER", "NIFI_NODE", "NAMENODE", "STATESTORE",
            "CATALOGSERVER", "KUDU_MASTER", "KUDU_TSERVER", "SOLR_SERVER", "NIFI_REGISTRY_SERVER", "HUE_LOAD_BALANCER", "KNOX_GATEWAY");

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ConfigUpdateUtilService configUpdateUtilService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DeleteVolumesService deleteVolumesService;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DeleteVolumesCMConfigEvent> event) {
        return new DeleteVolumesFailedEvent(e.getMessage(), e, resourceId);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DeleteVolumesCMConfigEvent.class);
    }

    @Override
    public Selectable doAccept(HandlerEvent<DeleteVolumesCMConfigEvent> deleteVolumesCMConfigEvent) {
        LOGGER.debug("Starting DeleteVolumesCMConfigHandler with event: {}", deleteVolumesCMConfigEvent);
        DeleteVolumesCMConfigEvent payload = deleteVolumesCMConfigEvent.getData();
        Long stackId = payload.getResourceId();
        String requestGroup = payload.getRequestGroup();
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            CmTemplateProcessor processor = getTemplateProcessor(stackDto);
            List<String> hostTemplateRoleGroupNames = processor.getHostTemplateRoleNames(requestGroup);
            Set<String> hostTemplateComponents = processor.getComponentsInHostGroup(requestGroup);
            Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
            if (checkConfigChangeRequired(hostTemplateComponents)) {
                LOGGER.debug("Updating CM config for stack: {}", stackDto);
                configUpdateUtilService.updateCMConfigsForComputeAndStartServices(stackDto, hostTemplateServiceComponents,
                        hostTemplateRoleGroupNames, requestGroup);
            }
            LOGGER.debug("Starting CM services for stack: {}", stackDto);
            deleteVolumesService.startClouderaManagerService(stackDto, hostTemplateServiceComponents);
            return new DeleteVolumesCMConfigFinishedEvent(stackId, requestGroup);
        } catch (Exception ex) {
            LOGGER.error("Error modifying CM configs for stack: {}, and group: {}, Exception:: {}", stackId, requestGroup,
                    ex.getCause());
            return new DeleteVolumesFailedEvent(ex.getMessage(), ex, stackId);
        }
    }

    private boolean checkConfigChangeRequired(Set<String> hostTemplateComponents) {
        for (String service : hostTemplateComponents) {
            if (BLACKLISTED_ROLES.contains(service)) {
                return false;
            }
        }
        return true;
    }

    private CmTemplateProcessor getTemplateProcessor(StackDto stackDto) {
        String blueprintText = stackDto.getBlueprintJsonText();
        return cmTemplateProcessorFactory.get(blueprintText);
    }
}
