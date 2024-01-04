package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationHandlerSelectors.PREPARE_PARCEL_SETTINGS_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradePreparationFailureEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeParcelSettingsPreparationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeParcelSettingsPreparationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeParcelSettingsPreparationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private ParcelService parcelService;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeParcelSettingsPreparationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade parcel settings preparation event. {}", event);
        ClusterUpgradeParcelSettingsPreparationEvent request = event.getData();
        Long stackId = request.getResourceId();
        try {
            StackDto stackDto = stackDtoService.getById(stackId);
            Set<ClouderaManagerProduct> clouderaManagerProducts = getRequiredProductsFromImage(stackDto, request.getImageChangeDto());
            LOGGER.debug("The following parcels will be prepared for upgrade: {}", clouderaManagerProducts);
            clusterApiConnectors.getConnector(stackDto).updateParcelSettings(clouderaManagerProducts);
            return new ClusterUpgradePreparationEvent(START_CLUSTER_UPGRADE_PARCEL_DOWNLOAD_EVENT.name(), stackId, clouderaManagerProducts,
                    request.getImageChangeDto().getImageId());
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade parcel settings preparation failed.", e);
            return new ClusterUpgradePreparationFailureEvent(request.getResourceId(), e);
        }
    }

    @Override
    public String selector() {
        return PREPARE_PARCEL_SETTINGS_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeParcelSettingsPreparationEvent> event) {
        LOGGER.error("Cluster upgrade preparation was unsuccessful due to an unexpected error", e);
        return new ClusterUpgradePreparationFailureEvent(resourceId, e);
    }

    private Set<ClouderaManagerProduct> getRequiredProductsFromImage(StackDto stackDto, ImageChangeDto imageChangeDto)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return parcelService.getRequiredProductsFromImage(stackDto, getImageFromCatalog(stackDto, imageChangeDto));
    }

    private Image getImageFromCatalog(StackDto stackDto, ImageChangeDto imageChangeDto)
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        return imageCatalogService.getImage(stackDto.getWorkspace().getId(), imageChangeDto.getImageCatalogUrl(), imageChangeDto.getImageCatalogName(),
                imageChangeDto.getImageId()).getImage();
    }
}
