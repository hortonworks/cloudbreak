package com.sequenceiq.cloudbreak.reactor.handler.cluster.install;

import static com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion.CM;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CentralCmTemplateUpdater;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProviderProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.install.ConfigureClusterManagerManagementServicesSuccess;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ConfigureClusterManagerManagementServicesHandler extends ExceptionCatcherEventHandler<ConfigureClusterManagerManagementServicesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureClusterManagerManagementServicesHandler.class);

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Inject
    private CentralCmTemplateUpdater centralCmTemplateUpdater;

    @Inject
    private CmTemplateComponentConfigProviderProcessor cmTemplateComponentConfigProviderProcessor;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ConfigureClusterManagerManagementServicesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ConfigureClusterManagerManagementServicesRequest> event) {
        LOGGER.error("ConfigureClusterManagerManagementServicesHandler step failed with the following message: {}", e.getMessage());
        return new ConfigureClusterManagerManagementServicesFailed(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ConfigureClusterManagerManagementServicesRequest> event) {
        Long stackId = event.getData().getResourceId();
        Selectable response;
        try {
            clusterBuilderService.configureManagementServices(stackId);
            updateServiceConfigsForUpgrade(stackId, event.getData());
            response = new ConfigureClusterManagerManagementServicesSuccess(stackId);
        } catch (RuntimeException | IOException | CloudbreakException e) {
            LOGGER.error("ConfigureClusterManagerManagementServicesHandler step failed with the following message: {}", e.getMessage());
            response = new ConfigureClusterManagerManagementServicesFailed(stackId, e);
        }
        return response;
    }

    private void updateServiceConfigsForUpgrade(Long stackId, ConfigureClusterManagerManagementServicesRequest data) throws IOException, CloudbreakException {
        Optional<com.sequenceiq.cloudbreak.cloud.model.Image> currentImageOpt = data.getCurrentImageOpt();
        Optional<StatedImage> targetStatedImageOpt = data.getTargetStatedImageOpt();
        StackDto stack = stackDtoService.getById(stackId);
        TemplatePreparationObject templatePreparationObject = stackToTemplatePreparationObjectConverter.convert(stack);
        CmTemplateProcessor cmTemplateProcessor = centralCmTemplateUpdater.getCmTemplateProcessor(templatePreparationObject);
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stack);
        String fromCmVersion = currentImageOpt
                .map(com.sequenceiq.cloudbreak.cloud.model.Image::getPackageVersions)
                .filter(packageVersions -> packageVersions.containsKey(CM.getKey()))
                .map(packageVersions -> packageVersions.get(CM.getKey()))
                .orElse("");
        String toCmVersion = targetStatedImageOpt
                .map(StatedImage::getImage)
                .map(Image::getPackageVersions)
                .filter(packageVersions -> packageVersions.containsKey(CM.getKey()))
                .map(packageVersions -> packageVersions.get(CM.getKey()))
                .orElse("");

        for (Map.Entry<String, Map<String, String>> entry : cmTemplateComponentConfigProviderProcessor.getServiceConfigsToBeUpdatedDuringUpgrade(
                cmTemplateProcessor, templatePreparationObject, fromCmVersion, toCmVersion).entrySet()) {
            LOGGER.info("Updating service configs for service: {}", entry.getKey());
            clusterApi.updateServiceConfig(entry.getKey(), entry.getValue());
        }
    }
}
